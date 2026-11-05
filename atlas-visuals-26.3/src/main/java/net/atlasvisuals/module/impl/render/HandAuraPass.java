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
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.atlasvisuals.util.ShaderOverlay;
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
final class HandAuraPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final RenderPipeline PIPELINE = Pipes.screen("hand_aura").withBindGroupLayout(Pipes.samplers("CaptureSampler", "HaloSampler")).withBindGroupLayout(Pipes.uniform("HandAuraConfig")).withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_COLOR)).build();
   private static final Supplier<String> PASS_LABEL = () -> "Atlas hand aura";
   private static final Supplier<String> CONFIG_LABEL = () -> "Atlas hand aura config";
   private static final int CONFIG_SIZE = 48;
   private static final org.joml.Vector4f CLEAR = new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
   private static TextureTarget hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandAuraPass() {
   }

   static boolean begin() {
      if (!capturing) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.gameRenderer.mainRenderTarget();
         if (mc.level != null
            && !mc.gameRenderer.mainCamera().isPanoramicMode()
            && main.getColorTexture() != null
            && main.width > 0
            && main.height > 0
            && !ShaderOverlay.shaderPackInUse()) {
            GpuDevice device = RenderSystem.getDevice();
            if (!shaderReady(device)) {
               return false;
            } else {
               if (hand == null) {
                  allocate(main.width, main.height);
               } else if (hand.width != main.width || hand.height != main.height) {
                  hand.resize(main.width, main.height);
               }

               device.createCommandEncoder().clearColorAndDepthTextures(hand.getColorTexture(), CLEAR, hand.getDepthTexture(), 0.0);
               capturing = true;
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static GpuTextureView color() {
      return hand.getColorTextureView();
   }

   static GpuTextureView depth() {
      return hand.getDepthTextureView();
   }

   static boolean end() {
      if (!capturing) {
         return false;
      } else {
         capturing = false;
         return true;
      }
   }

   static void compose(int tintArgb, float aura, float reachPx, float rim, float rimPx, int sheenArgb, float sheen, float phase) {
      RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = aura > 0.0F ? blur.spread(encoder, hand.getColorTextureView(), hand.width, hand.height, reachPx) : hand.getColorTextureView();
      putColor(0, tintArgb, aura);
      putColor(16, sheenArgb, sheen);
      configData.putFloat(32, rim);
      configData.putFloat(36, rimPx);
      configData.putFloat(40, phase);
      configData.putFloat(44, 0.0F);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorTextureView(), java.util.Optional.empty());

      try {
         pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
         pass.setUniform("HandAuraConfig", configSlice);
         pass.setUniform("CaptureSampler", hand.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.setUniform("HaloSampler", halo, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
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

   private static void putColor(int offset, int argb, float weight) {
      configData.putFloat(offset, (argb >> 16 & 0xFF) / 255.0F);
      configData.putFloat(offset + 4, (argb >> 8 & 0xFF) / 255.0F);
      configData.putFloat(offset + 8, (argb & 0xFF) / 255.0F);
      configData.putFloat(offset + 12, weight);
   }

   private static boolean shaderReady(GpuDevice device) {
      if (net.atlasvisuals.util.Pipes.ready(PIPELINE) && AuraBlur.compiled(device)) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Hand Aura shaders did not compile; the hand is drawn as usual");
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      hand = new TextureTarget("Atlas hand aura", width, height, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, com.mojang.renderpearl.api.GpuFormat.D32_FLOAT);
      blur = new AuraBlur("Atlas hand aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 48L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(48);
   }

   static void release() {
      if (capturing) {
         end();
      }

      if (hand != null) {
         hand.destroyBuffers();
         hand = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
