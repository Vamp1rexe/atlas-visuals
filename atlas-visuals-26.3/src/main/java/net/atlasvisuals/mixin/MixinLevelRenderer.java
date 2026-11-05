package net.atlasvisuals.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.atlasvisuals.event.impl.Render3DEvent;
import net.atlasvisuals.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LevelRenderer.class})
public abstract class MixinLevelRenderer {
   @Inject(method = {"render"}, at = {@At("HEAD")})
   private void atlas$onRenderLevel(
      GraphicsResourceAllocator allocator, boolean outline, CameraRenderState camera, GpuBufferSlice fog, Vector4f fogColor, boolean sky, boolean depth, CallbackInfo ci
   ) {
      float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
      WorldQuads.beginFrame(camera.pos);
      Render3DEvent.dispatch(partialTick, camera.cullFrustum);
   }

   @Inject(method = {"render"}, at = {@At("TAIL")})
   private void atlas$afterLevel(
      GraphicsResourceAllocator allocator, boolean outline, CameraRenderState camera, GpuBufferSlice fog, Vector4f fogColor, boolean sky, boolean depth, CallbackInfo ci
   ) {
      WorldQuads.render(camera.viewRotationMatrix);
      if (camera.fogType != FogType.NONE) {
         RenderSystem.setShaderFog(fog);
      }
   }
}