package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.atlasvisuals.event.impl.Render3DEvent;
import net.atlasvisuals.util.ShaderOverlay;
import net.atlasvisuals.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_5636;
import net.minecraft.class_761;
import net.minecraft.class_12180.class_12181;
import net.minecraft.class_758.class_4596;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_761.class})
public abstract class MixinLevelRenderer {
   @ModifyArg(
      method = {"method_22710"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_863;method_23099(Lnet/minecraft/class_4604;DDDF)V"
      ),
      index = 0
   )
   private class_4604 atlas$onRenderLevel(
      class_4604 frustum, @Local(argsOnly = true,ordinal = 0) Matrix4f viewRotation, @Local(argsOnly = true,ordinal = 1) Matrix4f projection
   ) {
      float partialTick = class_310.method_1551().method_61966().method_60637(false);
      WorldQuads.beginFrame(class_310.method_1551().field_1773.method_19418().method_71156());
      class_12181 capture = ShaderOverlay.capture();

      try {
         Render3DEvent.dispatch(partialTick, frustum);
      } catch (Throwable var9) {
         if (capture != null) {
            try {
               capture.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (capture != null) {
         capture.close();
      }

      return frustum;
   }

   @ModifyArg(
      method = {"method_22710"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_761;method_62199(Lnet/minecraft/class_9909;Lnet/minecraft/class_12075;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"
      ),
      index = 2
   )
   private GpuBufferSlice atlas$noFluidFogOnTop(GpuBufferSlice fog, @Local(argsOnly = true) class_4184 camera) {
      return atlas$onTopFog(fog, camera);
   }

   @Unique
   private static GpuBufferSlice atlas$onTopFog(GpuBufferSlice fog, class_4184 camera) {
      return camera.method_19334() == class_5636.field_27888
         ? fog
         : ((AccessorGameRenderer)class_310.method_1551().field_1773).atlas$getFogRenderer().method_71109(class_4596.field_60101);
   }

   @Inject(
      method = {"method_22710"},
      at = {@At("TAIL")}
   )
   private void atlas$overlayAfterShaderPack(
      CallbackInfo ci,
      @Local(argsOnly = true) GpuBufferSlice fog,
      @Local(argsOnly = true) class_4184 camera,
      @Local(argsOnly = true,ordinal = 0) Matrix4f viewRotation,
      @Local(argsOnly = true,ordinal = 1) Matrix4f projection
   ) {
      WorldQuads.render(viewRotation, projection);
      if (ShaderOverlay.pending()) {
         ShaderOverlay.render(camera, viewRotation, projection, fog, atlas$onTopFog(fog, camera));
      }
   }

   @Inject(
      method = {"method_22710"},
      at = {@At("TAIL")}
   )
   private void atlas$worldFogForHand(CallbackInfo ci, @Local(argsOnly = true) GpuBufferSlice fog, @Local(argsOnly = true) class_4184 camera) {
      if (camera.method_19334() != class_5636.field_27888) {
         RenderSystem.setShaderFog(fog);
      }
   }

}
