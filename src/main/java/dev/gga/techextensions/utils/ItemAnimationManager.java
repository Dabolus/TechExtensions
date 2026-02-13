package dev.gga.techextensions.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Manages in-flight item animations using `Display.ItemDisplay`
 * entities that follow waypoint paths with firework particle trails.
 */
public final class ItemAnimationManager {

    /** Blocks per tick that animated items travel. */
    private static final double SPEED = 1.5;

    private static final List<Task> TASKS = new ArrayList<>();
    private static long lastTickedGameTime = -1;

    private ItemAnimationManager() {}

    /** Represents an in-flight item traveling along waypoints. */
    private static class Task {
        final ServerLevel world;
        final List<Vec3> path;
        final int durationTicks;
        final Runnable onComplete;
        final Display.ItemDisplay display;
        int tick = 0;

        Task(ServerLevel world, List<Vec3> path, ItemStack displayItem, Runnable onComplete) {
            this.world = world;
            this.path = path;
            double totalLength = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                totalLength += path.get(i).distanceTo(path.get(i + 1));
            }
            this.durationTicks = Math.max(1, (int) Math.ceil(totalLength / SPEED));
            this.onComplete = onComplete;

            Vec3 start = path.getFirst();
            this.display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
            this.display.setItemStack(displayItem.copy());
            this.display.setNoGravity(true);
            this.display.setInvulnerable(true);
            this.display.setPos(start.x, start.y, start.z);
            world.addFreshEntity(this.display);
        }
    }

    /** Schedules a new animated item travel along the given path. */
    public static void schedule(ServerLevel world, List<Vec3> path, ItemStack displayItem, Runnable onComplete) {
        TASKS.add(new Task(world, path, displayItem, onComplete));
    }

    /**
     * Processes all active animations. Call once per tick from
     * `inventoryTick`. Guards against multiple calls per game tick.
     */
    public static void tick(ServerLevel world) {
        long gameTime = world.getGameTime();
        if (gameTime == lastTickedGameTime || TASKS.isEmpty()) return;
        lastTickedGameTime = gameTime;

        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            task.tick++;
            double progress = Math.min(1.0, (double) task.tick / task.durationTicks);

            Vec3 pos = interpolatePath(task.path, progress);
            task.display.setPos(pos);

            // Firework trail particles
            double prevProgress = Math.max(0, (double) (task.tick - 1) / task.durationTicks);
            Vec3 prevPos = interpolatePath(task.path, prevProgress);
            Vec3 seg = pos.subtract(prevPos);
            double segLen = seg.length();
            int steps = Math.max(1, (int) Math.ceil(segLen / 0.5));
            for (int p = 0; p < steps; p++) {
                double t = (double) p / steps;
                task.world.sendParticles(
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

            if (task.tick >= task.durationTicks) {
                task.display.discard();
                task.onComplete.run();
                it.remove();
            }
        }
    }

    /**
     * Interpolates a position along a list of waypoints.
     * `progress` ranges from 0.0 (first point) to 1.0 (last point).
     */
    public static Vec3 interpolatePath(List<Vec3> path, double progress) {
        if (path.size() < 2) return path.getFirst();

        double totalLength = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            totalLength += path.get(i).distanceTo(path.get(i + 1));
        }

        double targetDist = totalLength * Math.min(1.0, Math.max(0.0, progress));
        double accumulated = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            double segLen = path.get(i).distanceTo(path.get(i + 1));
            if (accumulated + segLen >= targetDist) {
                double segProgress = segLen > 0 ? (targetDist - accumulated) / segLen : 0;
                return path.get(i).lerp(path.get(i + 1), segProgress);
            }
            accumulated += segLen;
        }
        return path.getLast();
    }
}
