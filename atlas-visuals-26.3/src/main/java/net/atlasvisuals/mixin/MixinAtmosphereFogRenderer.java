package net.atlasvisuals.mixin;

import net.atlasvisuals.module.impl.render.AtmosphereModule;
import net.atlasvisuals.module.impl.render.PeepWorld;
import net.atlasvisuals.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class MixinAtmosphereFogRenderer {
   @Inject(
      method = {"computeFogColor"},
      at = {@At("TAIL")}
   )
   private void atlas$fogColour(
      Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkness, Vector4f dest, CallbackInfo ci
   ) {
      FogType fluid = camera.getFluidInCamera();
      if (fluid == FogType.WATER) {
         PinkWater.underwater(dest);
      }

      if (fluid == FogType.NONE && !(darkness > 0.0F)) {
         if (!(camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
            )
          {
            AtmosphereModule atmosphere = AtmosphereModule.get();
            if (atmosphere != null) {
               atmosphere.fog(camera, level, dest);
            }

            PeepWorld.fog(dest);
         }
      }
   }
}
