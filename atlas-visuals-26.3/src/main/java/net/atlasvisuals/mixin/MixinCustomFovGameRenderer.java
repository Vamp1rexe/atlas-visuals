package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.CustomFovModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Camera.class})
public abstract class MixinCustomFovGameRenderer {
   @Shadow
   public abstract boolean isPanoramicMode();

   @ModifyReturnValue(method = {"calculateFov"}, at = {@At("RETURN")})
   private float atlas$customFov(float original) {
      CustomFovModule module = CustomFovModule.active;
      return module != null && !this.isPanoramicMode() ? module.fov() : original;
   }

   @ModifyReturnValue(method = {"calculateHudFov"}, at = {@At("RETURN")})
   private float atlas$customHudFov(float original) {
      float fov = this.atlas$customFov(original);
      CustomFovModule.hudFovApplied(original, fov);
      return fov;
   }
}