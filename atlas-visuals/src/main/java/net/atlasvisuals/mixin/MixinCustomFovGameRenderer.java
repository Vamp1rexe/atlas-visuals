package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.atlasvisuals.module.impl.render.CustomFovModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_757.class})
public abstract class MixinCustomFovGameRenderer {
   @Shadow
   public abstract boolean method_35765();

   @ModifyReturnValue(
      method = {"method_3196"},
      at = {@At("RETURN")}
   )
   private float atlas$customFov(float original, @Local(argsOnly = true) boolean useFovSetting) {
      CustomFovModule module = CustomFovModule.active;
      float fov = module != null && !this.method_35765() ? module.fov() : original;
      if (!useFovSetting) {
         CustomFovModule.hudFovApplied(original, fov);
      }

      return fov;
   }
}
