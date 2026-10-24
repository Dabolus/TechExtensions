package dev.gga.techextensions.entity;

import dev.gga.techextensions.config.TechExtensionsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A bubble entity that traps a living entity inside it, floats them upward
 * for a configurable duration, then pops — releasing the entity to fall.
 */
public class BubbleTrapEntity extends Entity {
    /** Entity tag marking a living entity as currently trapped, so the Bubble Gun does not target it again. */
    public static final String TRAPPED_TAG = "techextensions:bubble_trapped";

    private static final EntityDataAccessor<Integer> DATA_TRAPPED_ID =
            SynchedEntityData.defineId(BubbleTrapEntity.class, EntityDataSerializers.INT);

    /** Ticks to wait for the trapped entity to load after a world reload before giving up. */
    private static final int RESOLVE_GRACE_TICKS = 40;
    /** Upward speed of levitation per amplifier level (vanilla). */
    private static final double LEVITATION_SPEED_PER_LEVEL = 0.05D;

    private int floatTicks = 0;
    private int unresolvedTicks = 0;
    /** Persisted so the trapped entity can be found again after a reload. */
    @Nullable
    private UUID trappedUuid;

    private boolean released = false;
    private boolean appliedLevitation = false;

    public BubbleTrapEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean isTrapped(Entity entity) {
        return entity.entityTags().contains(TRAPPED_TAG);
    }

    /** Entities that loaded with the trapped tag, checked a tick later once their bubble had a chance to load too. */
    private static final List<Entity> LOADED_TAGGED = new ArrayList<>();

    /**
     * Registers the cleanup of entities that were saved while trapped (a player logging out mid-bubble, a mob
     * whose bubble is gone) so the tag can never stick for good.
     */
    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (isTrapped(entity)) {
                LOADED_TAGGED.add(entity);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (LOADED_TAGGED.isEmpty()) {
                return;
            }
            for (Entity entity : LOADED_TAGGED) {
                if (!entity.isRemoved() && !hasBubble(entity)) {
                    entity.removeTag(TRAPPED_TAG);
                    if (entity instanceof ServerPlayer player) {
                        player.removeEffect(MobEffects.LEVITATION);
                    }
                }
            }
            LOADED_TAGGED.clear();
        });
    }

    private static boolean hasBubble(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        UUID uuid = entity.getUUID();
        return !level.getEntitiesOfClass(
                        BubbleTrapEntity.class,
                        entity.getBoundingBox().inflate(8.0D),
                        bubble -> uuid.equals(bubble.trappedUuid))
                .isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TRAPPED_ID, -1);
    }

    /**
     * Sets the entity to be trapped. Should be called immediately after construction on the server.
     */
    public void setTrappedEntity(LivingEntity target) {
        this.trappedUuid = target.getUUID();
        this.entityData.set(DATA_TRAPPED_ID, target.getId());
        target.addTag(TRAPPED_TAG);
        // Stop the entity's current movement
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof ServerPlayer player) {
            applyLevitation(player);
        }
    }

    /**
     * Returns the trapped entity (server or client), or null if not found.
     */
    @Nullable
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
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            // The client only renders: the server drives movement and popping
            return;
        }

        Entity trapped = getTrappedEntity();
        if (trapped == null && trappedUuid != null) {
            // After a reload the network id is gone: find the entity by its UUID once its chunk is loaded
            trapped = serverLevel.getEntity(trappedUuid);
            if (trapped != null) {
                this.entityData.set(DATA_TRAPPED_ID, trapped.getId());
            }
        }
        if (trapped == null) {
            if (++unresolvedTicks > RESOLVE_GRACE_TICKS) {
                pop();
            }
            return;
        }
        if (!trapped.isAlive()) {
            // Trapped entity died — pop immediately
            pop();
            return;
        }

        double floatSpeed = TechExtensionsConfig.bubbleGunBubbleFloatSpeed.get();
        if (trapped instanceof ServerPlayer player) {
            // Players are client-authoritative, so levitation moves them and the bubble follows
            this.setPos(player.getX(), player.getY(), player.getZ());
            if (!player.hasEffect(MobEffects.LEVITATION)) {
                applyLevitation(player);
            }
        } else {
            // Move bubble and trapped entity upward
            this.setPos(trapped.getX(), this.getY() + floatSpeed, trapped.getZ());
            // Lock the trapped entity's position to the bubble
            trapped.setPos(this.getX(), this.getY(), this.getZ());
            trapped.setDeltaMovement(Vec3.ZERO);
            // Prevent AI from moving the trapped entity (re-applied each tick)
            if (trapped instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
        trapped.fallDistance = 0;

        floatTicks++;

        // Spawn ambient bubble particles
        if (floatTicks % 5 == 0) {
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
            serverLevel.playSound(
                    null, this.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.NEUTRAL, 0.6F, pitch);
        }

        // Check if float duration is exceeded
        if (floatTicks >= TechExtensionsConfig.bubbleGunBubbleFloatDuration.get()) {
            pop();
        }
    }

    private void applyLevitation(ServerPlayer player) {
        int remaining = Math.max(1, TechExtensionsConfig.bubbleGunBubbleFloatDuration.get() - floatTicks);
        int amplifier = Math.max(
                0,
                (int) Math.round(TechExtensionsConfig.bubbleGunBubbleFloatSpeed.get() / LEVITATION_SPEED_PER_LEVEL)
                        - 1);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, remaining, amplifier, false, false, false));
        appliedLevitation = true;
    }

    /** Frees the trapped entity: removes the marker tag and any effect this bubble applied. Idempotent. */
    private void releaseTrapped() {
        if (released) {
            return;
        }
        released = true;
        Entity trapped = getTrappedEntity();
        if (trapped == null && trappedUuid != null && this.level() instanceof ServerLevel serverLevel) {
            trapped = serverLevel.getEntity(trappedUuid);
        }
        if (trapped != null) {
            trapped.removeTag(TRAPPED_TAG);
            // Let the entity fall naturally — gravity and fall damage apply
            trapped.setDeltaMovement(Vec3.ZERO);
            if (appliedLevitation && trapped instanceof ServerPlayer player) {
                player.removeEffect(MobEffects.LEVITATION);
            }
        }
    }

    /**
     * Pops the bubble — releases the trapped entity, plays effects, and removes this entity.
     */
    private void pop() {
        Entity trapped = getTrappedEntity();
        releaseTrapped();

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
    public void remove(RemovalReason reason) {
        // Also covers /kill and other removals that bypass pop(), so the trapped entity is never left tagged.
        // Chunk unloading keeps the state: it is restored from the saved UUID on load.
        if (reason.shouldDestroy() && !this.level().isClientSide()) {
            releaseTrapped();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.floatTicks = input.getIntOr("FloatTicks", 0);
        this.trappedUuid = input.read("Trapped", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FloatTicks", this.floatTicks);
        if (this.trappedUuid != null) {
            output.store("Trapped", UUIDUtil.CODEC, this.trappedUuid);
        }
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
