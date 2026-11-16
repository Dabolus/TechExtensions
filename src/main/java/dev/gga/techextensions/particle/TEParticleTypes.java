package dev.gga.techextensions.particle;

import dev.gga.techextensions.TechExtensions;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class TEParticleTypes {
    public static final SimpleParticleType SHRINK_RAY = FabricParticleTypes.simple();

    public static void init() {
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "shrink_ray_particle"),
                SHRINK_RAY);
    }
}
