package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.atlasvisuals.module.impl.render.CustomFovModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1058;
import net.minecraft.class_1657;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4603;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({class_4603.class})
public abstract class MixinScreenEffectRenderer {
   @ModifyExpressionValue(
      method = {"method_23067"},
      at = {@At(
         value = "NEW",
         target = "()Lnet/minecraft/class_4587;"
      )}
   )
   private class_4587 atlas$undoCustomFov(class_4587 poseStack) {
      float scale = CustomFovModule.screenEffectScale();
      if (scale != 1.0F) {
         poseStack.method_23760().method_23761().scale(scale, scale, 1.0F);
      }

      return poseStack;
   }


}
