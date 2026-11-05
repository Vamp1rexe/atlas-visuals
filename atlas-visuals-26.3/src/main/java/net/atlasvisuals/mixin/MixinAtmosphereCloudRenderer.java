package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.atlasvisuals.module.impl.render.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LevelExtractor.class})
public abstract class MixinAtmosphereCloudRenderer {
   @ModifyExpressionValue(
      method = {"extract"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/ARGB;colorFromVector4f(Lorg/joml/Vector4fc;)I"
      )}
   )
   private int atlas$cloudColour(int color) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      return atmosphere != null ? atmosphere.clouds(color) : color;
   }
}