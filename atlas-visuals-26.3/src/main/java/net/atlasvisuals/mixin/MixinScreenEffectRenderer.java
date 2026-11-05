package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.atlasvisuals.module.impl.render.CustomFovModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ScreenEffectRenderer.class})
public abstract class MixinScreenEffectRenderer {
   @ModifyExpressionValue(
      method = {"submit"},
      at = {@At(
         value = "NEW",
         target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"
      )}
   )
   private PoseStack atlas$undoCustomFov(PoseStack poseStack) {
      float scale = CustomFovModule.screenEffectScale();
      if (scale != 1.0F) {
         poseStack.last().pose().scale(scale, scale, 1.0F);
      }

      return poseStack;
   }


}
