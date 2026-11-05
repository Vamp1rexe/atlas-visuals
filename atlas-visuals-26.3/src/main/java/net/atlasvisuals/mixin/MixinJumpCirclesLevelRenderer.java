package net.atlasvisuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.atlasvisuals.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LevelRenderer.class})
public abstract class MixinJumpCirclesLevelRenderer {
   @Inject(method = {"submitBlockDestroyAnimation"}, at = {@At("HEAD")})
   private void atlas$jumpCircles(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.submit(levelRenderState.cameraRenderState, poseStack, collector);
      }
   }
}