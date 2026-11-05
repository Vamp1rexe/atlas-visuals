package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({WaterFogEnvironment.class})
public abstract class MixinPinkWaterFog {
   @ModifyReturnValue(method = {"getBaseColor"}, at = {@At("RETURN")})
   private Vector3fc atlas$pinkWaterFog(Vector3fc color) {
      int before = 0xFF000000 | Math.round(color.x() * 255.0F) << 16 | Math.round(color.y() * 255.0F) << 8 | Math.round(color.z() * 255.0F);
      int after = PinkWater.waterFog(before);
      return after == before ? color : new Vector3f((after >> 16 & 0xFF) / 255.0F, (after >> 8 & 0xFF) / 255.0F, (after & 0xFF) / 255.0F);
   }
}