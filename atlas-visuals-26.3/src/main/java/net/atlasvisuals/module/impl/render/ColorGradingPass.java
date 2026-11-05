package net.atlasvisuals.module.impl.render;
import java.util.Optional;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import net.atlasvisuals.util.Pipes;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class ColorGradingPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final RenderPipeline PIPELINE = Pipes.screen("color_grading").withBindGroupLayout(Pipes.samplers("InSampler")).withBindGroupLayout(Pipes.uniform("ColorGradingConfig")).withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR)).build();
   private static final Supplier<String> PASS_LABEL = () -> "Atlas color grading";
   private static final Supplier<String> CONFIG_LABEL = () -> "Atlas color grading config";
   private static final int CONFIG_SIZE = 32;
   private static TextureTarget copy;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean invalidShaderLogged;

   private ColorGradingPass() {
   }

   static void apply(boolean worldRendered, float vibrance, float contrast, float exposure, float warmth, float vignette) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      GpuTexture mainColor = main.getColorTexture();
      if (worldRendered
         && mc.level != null
         && mc.isGameLoadFinished()
         && !mc.gameRenderer.mainCamera().isPanoramicMode()
         && mainColor != null
         && main.width > 0
         && main.height > 0) {
         if (copy == null) {
            allocate(main.width, main.height);
         } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height);
         }

         GpuDevice device = RenderSystem.getDevice();
         if (shaderReady(device)) {
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(mainColor, copy.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
            configData.putFloat(0, vibrance);
            configData.putFloat(4, contrast);
            configData.putFloat(8, exposure);
            configData.putFloat(12, warmth);
            configData.putFloat(16, vignette);
            encoder.writeToBuffer(configSlice, configData);
            RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorTextureView(), java.util.Optional.empty());

            try {
               pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
               pass.setUniform("ColorGradingConfig", configSlice);
               pass.setUniform("InSampler", copy.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
               pass.draw(3, 1, 0, 0);
            } catch (Throwable var15) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (pass != null) {
               pass.close();
            }
         }
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (net.atlasvisuals.util.Pipes.ready(PIPELINE)) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", PIPELINE);
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      copy = new TextureTarget("Atlas color grading copy", width, height, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      if (copy != null) {
         copy.destroyBuffers();
         copy = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
