package net.atlasvisuals.module.impl.render;
import java.util.Optional;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import net.atlasvisuals.util.Pipes;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
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
final class MotionBlurPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final RenderPipeline PIPELINE = Pipes.screen("motion_blur").withBindGroupLayout(Pipes.samplers("InSampler")).withBindGroupLayout(Pipes.uniform("MotionBlurConfig")).withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR)).build();
   private static final Supplier<String> PASS_LABEL = () -> "Atlas motion blur";
   private static final Supplier<String> CONFIG_LABEL = () -> "Atlas motion blur config";
   private static final int CONFIG_SIZE = 16;
   private static final long STALE_NANOS = 1000000000L;
   private static final float MIN_RETENTION = 0.003921569F;
   private static TextureTarget history;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean primed;
   private static long lastFrameNanos;
   private static int levelKey;
   private static int seed;
   private static boolean invalidShaderLogged;

   private MotionBlurPass() {
   }

   static void apply(boolean worldRendered, float retention60) {
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
         long now = System.nanoTime();
         long gap = now - lastFrameNanos;
         lastFrameNanos = now;
         int level = System.identityHashCode(mc.level);
         if (level != levelKey) {
            levelKey = level;
            primed = false;
         }

         if (history == null) {
            allocate(main.width, main.height);
            primed = false;
         } else if (history.width != main.width || history.height != main.height) {
            history.resize(main.width, main.height);
            primed = false;
         }

         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         if (primed && gap > 0L && gap < 1000000000L) {
            float retention = (float)Math.pow(retention60, gap * 6.0E-8);
            if (retention >= 0.003921569F && shaderReady(device)) {
               blend(encoder, main, retention);
            }
         }

         encoder.copyTextureToTexture(mainColor, history.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
         primed = true;
      } else {
         primed = false;
      }
   }

   private static void blend(CommandEncoder encoder, RenderTarget main, float retention) {
      seed = seed + 1 & 63;
      configData.putFloat(0, retention);
      configData.putFloat(4, seed * 7.0F);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorTextureView(), java.util.Optional.empty());

      try {
         pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
         pass.setUniform("MotionBlurConfig", configSlice);
         pass.setUniform("InSampler", history.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         pass.draw(3, 1, 0, 0);
      } catch (Throwable var7) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (net.atlasvisuals.util.Pipes.ready(PIPELINE)) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Motion Blur shader {} did not compile; frames are passed through unblurred", PIPELINE);
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      history = new TextureTarget("Atlas motion blur history", width, height, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(16);
   }

   static void release() {
      if (history != null) {
         history.destroyBuffers();
         history = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
      primed = false;
      lastFrameNanos = 0L;
      levelKey = 0;
   }
}
