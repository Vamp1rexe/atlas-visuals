package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.atlasvisuals.module.impl.render.HandModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({FirstPersonHandsAndItemsRenderer.class})
public abstract class MixinHandModelItemInHandRenderer {
   @WrapOperation(
      method = {"renderPlayerHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )}
   )
   private void atlas$longRightArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
      if (HandModelModule.active != null) {
         pose.pushPose();
         HandModelModule.shoulder(pose, true, renderer);
         original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
         pose.popPose();
      }
   }

   @WrapOperation(
      method = {"renderPlayerHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )}
   )
   private void atlas$longLeftArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
      if (HandModelModule.active != null) {
         pose.pushPose();
         HandModelModule.shoulder(pose, false, renderer);
         original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
         pose.popPose();
      }
   }
}
