package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.atlasvisuals.module.impl.render.HandModelModule;
import net.atlasvisuals.module.impl.render.WeaponInspectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({FirstPersonHandsAndItemsRenderer.class})
public abstract class MixinWeaponInspectItemInHandRenderer {
   @Inject(method = {"submitHandsWithItems"}, at = {@At("HEAD")})
   private void atlas$inspectHands(
      float partialTick, PoseStack poseStack, SubmitNodeCollector collector, PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci
   ) {
      if (WeaponInspectModule.active != null) {
         WeaponInspectModule.handsBegin(poseStack);
      }
   }

   @WrapOperation(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
      )}
   )
   private void atlas$inspectItem(
      ItemStackRenderState item,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      int overlay,
      int outline,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true) ItemStack stack,
      @Local(argsOnly = true) PlayerRenderState player
   ) {
      LocalPlayer holder = Minecraft.getInstance().player;
      if (holder == null || player.avatarRenderState == null) {
         original.call(item, poseStack, collector, light, overlay, outline);
      } else {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm main = player.avatarRenderState.mainArm;
         boolean right = (mainHand ? main : main.getOpposite()) == HumanoidArm.RIGHT;
         ItemDisplayContext context = right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         if (WeaponInspectModule.active != null) {
            if (!WeaponInspectModule.renderItem(poseStack, holder, stack, context, collector, light, mainHand)) {
               original.call(item, poseStack, collector, light, overlay, outline);
            }
         } else {
            if (HandModelModule.active != null) {
               HandModelModule.renderArm(poseStack, holder, stack, context, collector, light, mainHand);
            }
            original.call(item, poseStack, collector, light, overlay, outline);
         }
      }
   }
}