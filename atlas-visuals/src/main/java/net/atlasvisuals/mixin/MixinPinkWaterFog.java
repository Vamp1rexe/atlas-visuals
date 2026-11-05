package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11403;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_11403.class})
public abstract class MixinPinkWaterFog {
   @ModifyReturnValue(
      method = {"method_71654"},
      at = {@At("RETURN")}
   )
   private int atlas$pinkWaterFog(int color) {
      return PinkWater.waterFog(color);
   }
}
