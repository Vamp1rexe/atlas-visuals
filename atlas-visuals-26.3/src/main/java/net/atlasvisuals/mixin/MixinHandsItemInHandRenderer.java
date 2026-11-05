package net.atlasvisuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.atlasvisuals.module.impl.render.HandsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({FirstPersonHandsAndItemsRenderer.class})
public abstract class MixinHandsItemInHandRenderer {
   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void atlas$handsPose(
      PlayerRenderState player,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swing,
      ItemStack stack,
      float equip,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      if (HandsModule.live != null && player.avatarRenderState != null) {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm main = player.avatarRenderState.mainArm;
         HumanoidArm arm = mainHand ? main : main.getOpposite();
         HandsModule.shape(poseStack, mainHand, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }
}