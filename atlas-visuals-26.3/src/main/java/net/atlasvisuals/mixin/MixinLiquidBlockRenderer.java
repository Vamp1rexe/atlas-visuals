package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({FluidRenderer.class})
public abstract class MixinLiquidBlockRenderer {
   @ModifyReturnValue(
      method = {"getLightCoords"},
      at = {@At("RETURN")}
   )
   private int atlas$pinkWaterGlow(int light) {
      return PinkWater.light(light);
   }
}
