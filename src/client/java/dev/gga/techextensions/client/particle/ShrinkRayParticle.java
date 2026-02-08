package dev.gga.techextensions.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SonicBoomParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class ShrinkRayParticle extends SonicBoomParticle {
    protected ShrinkRayParticle(ClientLevel clientLevel, double d, double e, double f, double g, SpriteSet spriteSet) {
        super(clientLevel, d, e, f, g, spriteSet);
        this.quadSize *= 0.1f;
        this.lifetime = 5;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(
                SimpleParticleType simpleParticleType,
                ClientLevel clientLevel,
                double d,
                double e,
                double f,
                double g,
                double h,
                double i,
                RandomSource randomSource) {
            return new ShrinkRayParticle(clientLevel, d, e, f, g, this.sprites);
        }
    }
}
