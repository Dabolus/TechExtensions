package dev.gga.techextensions.entity;

import dev.gga.techextensions.config.TechExtensionsConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A bubble entity that traps a living entity inside it, floats them upward
 * for a configurable duration, then pops — releasing the entity to fall.
 */
public class BubbleTrapEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_TRAPPED_ID =
            SynchedEntityData.defineId(BubbleTrapEntity.class, EntityDataSerializers.INT);

    private int floatTicks = 0;

    public BubbleTrapEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TRAPPED_ID, -1);
    }

    /**
     * Sets the entity to be trapped. Should be called immediately after construction on the server.
     */
    public void setTrappedEntity(LivingEntity target) {
        this.entityData.set(DATA_TRAPPED_ID, target.getId());
        target.addTag("techextensions:bubble_trapped");
        // Stop the entity's current movement
        target.setDeltaMovement(0, 0, 0);
    }

    /**
     * Returns the trapped entity (server or client), or null if not found.
     */
    public Entity getTrappedEntity() {
        int id = this.entityData.get(DATA_TRAPPED_ID);
        if (id == -1) {
            return null;
        }
        return this.level().getEntity(id);
    }

    @Override
    public void tick() {
        super.tick();

        Entity trapped = getTrappedEntity();
        if (trapped == null || !trapped.isAlive()) {
            // Trapped entity disappeared — pop immediately
            pop();
            return;
        }

        // Move bubble and trapped entity upward
        double floatSpeed = TechExtensionsConfig.bubbleGunBubbleFloatSpeed;
        this.setPos(trapped.getX(), this.getY() + floatSpeed, trapped.getZ());

        // Lock the trapped entity's position to the bubble
        trapped.setPos(this.getX(), this.getY(), this.getZ());
        trapped.setDeltaMovement(0, 0, 0);
        trapped.fallDistance = 0;

        // Prevent AI from moving the trapped entity (re-applied each tick)
        if (trapped instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().stop();
        }

        floatTicks++;

        // Spawn ambient bubble particles
        if (this.level() instanceof ServerLevel serverLevel && floatTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE,
                    this.getX(),
                    this.getY() + trapped.getBbHeight() * 0.5,
                    this.getZ(),
                    3,
                    trapped.getBbWidth() * 0.5,
                    trapped.getBbHeight() * 0.5,
                    trapped.getBbWidth() * 0.5,
                    0.02);
        }

        // Play wobbling ambient sound every 15 ticks
        if (floatTicks % 15 == 0) {
            float pitch = 0.6F + (float) Math.sin(floatTicks * 0.15) * 0.3F;
            this.level()
                    .playSound(
                            null,
                            this.blockPosition(),
                            SoundEvents.HONEY_BLOCK_SLIDE,
                            SoundSource.NEUTRAL,
                            0.6F,
                            pitch);
        }

        // Check if float duration is exceeded
        if (floatTicks >= TechExtensionsConfig.bubbleGunBubbleFloatDuration) {
            pop();
        }
    }

    /**
     * Pops the bubble — releases the trapped entity, plays effects, and removes this entity.
     */
    private void pop() {
        Entity trapped = getTrappedEntity();
        if (trapped != null) {
            trapped.removeTag("techextensions:bubble_trapped");
            // Let the entity fall naturally — gravity and fall damage apply
            trapped.setDeltaMovement(0, 0, 0);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            // Pop particles
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    this.getX(),
                    this.getY() + (trapped != null ? trapped.getBbHeight() * 0.5 : 0.5),
                    this.getZ(),
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.1);

            // Pop sound
            serverLevel.playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.NEUTRAL,
                    1.0F,
                    0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
        }

        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.floatTicks = input.getIntOr("FloatTicks", 0);
        // Trapped entity ID is transient — entity might not exist after reload
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FloatTicks", this.floatTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
