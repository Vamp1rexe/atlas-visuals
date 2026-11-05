package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_775;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_775.class})
public abstract class MixinLiquidBlockRenderer {
   @ModifyReturnValue(
      method = {"method_3343"},
      at = {@At("RETURN")}
   )
   private int atlas$pinkWaterGlow(int light) {
      return PinkWater.light(light);
   }
}
