package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.atlasvisuals.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({FirstPersonHandsAndItemsRenderer.class})
public abstract class MixinSwingAnimationItemInHandRenderer {
   @WrapOperation(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )}
   )
   private void atlas$swingStyle(
      FirstPersonHandsAndItemsRenderer renderer, float progress, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original, @Local(argsOnly = true) InteractionHand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == InteractionHand.MAIN_HAND)) {
         module.pose(poseStack, progress, side);
      } else {
         original.call(new Object[]{renderer, progress, poseStack, side, arm});
      }
   }
}
