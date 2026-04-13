package dev.gga.techextensions.utils;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared raytrace / aiming utilities used by weapons that target blocks or entities
 * along the player's line of sight (Bubble Gun, Shrink Ray, Vacuum Gun).
 */
public final class TEAimingUtils {

    private TEAimingUtils() {}

    /** Result of an entity raytrace: the entity hit and the intersection point. */
    public record EntityHit(Entity entity, Vec3 hitLocation, double distanceSq) {}

    /**
     * Finds the closest entity along a ray from `start` to `end`.
     *
     * Builds a search AABB from the source entity's bounding box expanded along the ray
     * direction and inflated by 1 block, then tests each candidate entity's inflated bounding
     * box for intersection with the ray.
     *
     * @param level         the level to search in
     * @param source        the entity performing the trace (excluded from results)
     * @param start         ray start position
     * @param end           ray end position
     * @param entityFilter  predicate to filter candidate entities (applied after excluding source)
     * @param aabbInflation extra inflation added to each entity's bounding box beyond
     *                      {@link Entity#getPickRadius()}
     * @param selfHitBox    optional AABB for self-targeting (e.g. player feet); if the ray
     *                      intersects this box closer than any other entity, the source entity
     *                      is returned as the hit. Pass `null` to disable.
     * @return the closest entity hit, or `null` if none
     */
    @Nullable
    public static EntityHit traceEntity(
            Level level,
            Entity source,
            Vec3 start,
            Vec3 end,
            Predicate<Entity> entityFilter,
            double aabbInflation,
            @Nullable AABB selfHitBox) {

        Vec3 direction = end.subtract(start);
        AABB searchBox = source.getBoundingBox().expandTowards(direction).inflate(1.0D);

        Entity closestEntity = null;
        Vec3 closestHitLocation = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity candidate : level.getEntities(source, searchBox, entityFilter)) {
            AABB entityBB = candidate.getBoundingBox().inflate(candidate.getPickRadius() + aabbInflation);
            Optional<Vec3> intersection = entityBB.clip(start, end);
            if (intersection.isPresent()) {
                double distSq = start.distanceToSqr(intersection.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntity = candidate;
                    closestHitLocation = intersection.get();
                }
            }
        }

        // Self-targeting check (e.g. Shrink Ray shooting at own feet)
        if (selfHitBox != null) {
            Optional<Vec3> selfHit = selfHitBox.clip(start, end);
            if (selfHit.isPresent()) {
                double selfDistSq = start.distanceToSqr(selfHit.get());
                if (closestEntity == null || selfDistSq < closestDistSq) {
                    closestEntity = source;
                    closestHitLocation = selfHit.get();
                    closestDistSq = selfDistSq;
                }
            }
        }

        if (closestEntity == null) {
            return null;
        }
        return new EntityHit(closestEntity, closestHitLocation, closestDistSq);
    }

    /**
     * Performs a block raytrace from `start` to `end`.
     *
     * @param level     the level to trace in
     * @param source    the entity performing the trace (used for collision context)
     * @param start     ray start position
     * @param end       ray end position
     * @param blockMode the block shape mode for the clip context
     * @param fluidMode the fluid mode for the clip context
     * @return the block hit result, or `null` if the ray missed all blocks
     */
    @Nullable
    public static BlockHitResult traceBlock(
            Level level,
            Entity source,
            Vec3 start,
            Vec3 end,
            ClipContext.Block blockMode,
            ClipContext.Fluid fluidMode) {

        BlockHitResult result = level.clip(new ClipContext(start, end, blockMode, fluidMode, source));
        if (result.getType() == HitResult.Type.MISS) {
            return null;
        }
        return result;
    }

    /**
     * Builds a "feet" AABB for self-targeting. The box covers the lower quarter of the
     * player's body, centered at foot level. This allows a downward-aimed ray starting
     * from eye position to intersect, since {@link AABB#clip} returns empty when the
     * ray originates inside the box.
     *
     * @param player the player to build the feet box for
     * @return an AABB covering the player's lower body
     */
    public static AABB buildFeetAABB(Player player) {
        double halfWidth = player.getBbWidth() * 0.5D;
        double feetHalfHeight = player.getBbHeight() * 0.25D;
        return new AABB(
                player.getX() - halfWidth,
                player.getY() - feetHalfHeight,
                player.getZ() - halfWidth,
                player.getX() + halfWidth,
                player.getY() + feetHalfHeight,
                player.getZ() + halfWidth);
    }

    /** Common entity filter: alive and not a spectator. */
    public static final Predicate<Entity> ALIVE_NON_SPECTATOR = e -> e.isAlive() && !e.isSpectator();
}
