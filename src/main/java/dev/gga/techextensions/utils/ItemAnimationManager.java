package dev.gga.techextensions.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Manages in-flight item animations using `Display.ItemDisplay` entities that follow waypoint paths with
 * firework particle trails. Driven by the server tick event, so animations finish even when the item that
 * started them is no longer in any inventory; displays left behind by a crash are removed when they load.
 */
public final class ItemAnimationManager {

    /** Tag put on the display entities so leftovers from a crash can be recognised and removed. */
    private static final String DISPLAY_TAG = "techextensions:item_animation";
    /** Blocks per tick that animated items travel. */
    private static final double SPEED = 1.5;
    /** Distance between two trail particles. */
    private static final double PARTICLE_SPACING = 0.5;

    private static final List<Task> TASKS = new ArrayList<>();
    /** Leftover displays found while loading, discarded from the tick (discarding inside the load callback leaves a dead entity in the tick list). */
    private static final List<Entity> ORPHANS = new ArrayList<>();

    private ItemAnimationManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ItemAnimationManager::tick);
        // Finish everything on shutdown so no display entity is saved mid-flight
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> finishAll());
        // Displays saved mid-flight by a crash are no longer tracked: remove them when their chunk loads
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Display.ItemDisplay
                    && entity.entityTags().contains(DISPLAY_TAG)
                    && !isTracked(entity)) {
                ORPHANS.add(entity);
            }
        });
    }

    /** Schedules a new animated item travel along the given path; {@code onComplete} runs on arrival. */
    public static void schedule(ServerLevel world, List<Vec3> path, ItemStack displayItem, Runnable onComplete) {
        Task task = new Task(world, path, displayItem, onComplete);
        // Track before spawning so the ENTITY_LOAD cleanup does not remove the new display
        TASKS.add(task);
        world.addFreshEntity(task.display);
    }

    private static boolean isTracked(Entity entity) {
        for (Task task : TASKS) {
            if (task.display == entity) {
                return true;
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        if (!ORPHANS.isEmpty()) {
            ORPHANS.forEach(Entity::discard);
            ORPHANS.clear();
        }
        if (TASKS.isEmpty()) {
            return;
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            if (it.next().tick()) {
                it.remove();
            }
        }
    }

    private static void finishAll() {
        for (Task task : TASKS) {
            task.finish();
        }
        TASKS.clear();
    }

    /** Represents an in-flight item traveling along waypoints. */
    private static final class Task {
        final ServerLevel world;
        final List<Vec3> path;
        /** Path length up to each waypoint, computed once. */
        final double[] cumulativeLength;

        final double totalLength;
        final int durationTicks;
        final Runnable onComplete;
        final Display.ItemDisplay display;
        int tick = 0;

        Task(ServerLevel world, List<Vec3> path, ItemStack displayItem, Runnable onComplete) {
            this.world = world;
            this.path = path;
            this.onComplete = onComplete;
            this.cumulativeLength = new double[path.size()];
            double total = 0;
            for (int i = 1; i < path.size(); i++) {
                total += path.get(i - 1).distanceTo(path.get(i));
                cumulativeLength[i] = total;
            }
            this.totalLength = total;
            this.durationTicks = Math.max(1, (int) Math.ceil(total / SPEED));

            Vec3 start = path.getFirst();
            this.display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, world);
            this.display.setItemStack(displayItem.copy());
            this.display.setNoGravity(true);
            this.display.setInvulnerable(true);
            this.display.setPos(start.x, start.y, start.z);
            this.display.addTag(DISPLAY_TAG);
        }

        /** @return true once the animation has finished */
        boolean tick() {
            tick++;
            double progress = Math.min(1.0, (double) tick / durationTicks);
            Vec3 pos = interpolate(progress);
            display.setPos(pos);

            // Firework trail particles
            Vec3 prevPos = interpolate(Math.max(0, (double) (tick - 1) / durationTicks));
            Vec3 seg = pos.subtract(prevPos);
            int steps = Math.max(1, (int) Math.ceil(seg.length() / PARTICLE_SPACING));
            for (int p = 0; p < steps; p++) {
                double t = (double) p / steps;
                world.sendParticles(
                        ParticleTypes.FIREWORK,
                        prevPos.x + seg.x * t,
                        prevPos.y + seg.y * t,
                        prevPos.z + seg.z * t,
                        1,
                        0,
                        0,
                        0,
                        0);
            }

            if (tick >= durationTicks) {
                finish();
                return true;
            }
            return false;
        }

        void finish() {
            display.discard();
            onComplete.run();
        }

        /** Position along the waypoints; `progress` ranges from 0.0 (first point) to 1.0 (last point). */
        Vec3 interpolate(double progress) {
            if (path.size() < 2) {
                return path.getFirst();
            }
            double targetDist = totalLength * Mth.clamp(progress, 0.0, 1.0);
            for (int i = 1; i < path.size(); i++) {
                if (cumulativeLength[i] >= targetDist) {
                    double segStart = cumulativeLength[i - 1];
                    double segLen = cumulativeLength[i] - segStart;
                    double segProgress = segLen > 0 ? (targetDist - segStart) / segLen : 0;
                    return path.get(i - 1).lerp(path.get(i), segProgress);
                }
            }
            return path.getLast();
        }
    }
}
