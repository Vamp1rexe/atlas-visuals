package net.atlasvisuals.module.impl.render;
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
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.lwjgl.BufferUtils;

@Environment(EnvType.CLIENT)
final class AuraBlur {
   private static final RenderPipeline PIPELINE = Pipes.screen("aura_spread").withBindGroupLayout(Pipes.samplers("InSampler")).withBindGroupLayout(Pipes.uniform("AuraSpread")).withColorTargetState(ColorTargetState.DEFAULT).build();
   private static final int CONFIG_SIZE = 16;
   private static final float TAPS = 6.0F;
   private final String name;
   private final Supplier<String> passLabel;
   private TextureTarget across;
   private TextureTarget down;
   private GpuBuffer acrossConfig;
   private GpuBuffer downConfig;
   private GpuBufferSlice acrossSlice;
   private GpuBufferSlice downSlice;
   private ByteBuffer configData;

   AuraBlur(String name) {
      this.name = name;
      this.passLabel = () -> name + " spread";
   }

   static boolean compiled(GpuDevice device) {
      return net.atlasvisuals.util.Pipes.ready(PIPELINE);
   }

   GpuTextureView spread(CommandEncoder encoder, GpuTextureView source, int width, int height, float reachPx) {
      int divisor = reachPx >= 8.0F ? 4 : 2;
      int halfWidth = Math.max(1, (width + divisor - 1) / divisor);
      int halfHeight = Math.max(1, (height + divisor - 1) / divisor);
      if (this.across == null) {
         this.allocate(halfWidth, halfHeight);
      } else if (this.across.width != halfWidth || this.across.height != halfHeight) {
         this.across.resize(halfWidth, halfHeight);
         this.down.resize(halfWidth, halfHeight);
      }

      float step = Math.max(0.75F, reachPx / 6.0F);
      this.run(encoder, source, this.across, this.acrossSlice, step / width, 0.0F, PIPELINE);
      this.run(encoder, this.across.getColorTextureView(), this.down, this.downSlice, 0.0F, step / height, PIPELINE);
      return this.down.getColorTextureView();
   }

   private void run(CommandEncoder encoder, GpuTextureView from, TextureTarget into, GpuBufferSlice slice, float du, float dv, RenderPipeline selected) {
      this.configData.putFloat(0, du);
      this.configData.putFloat(4, dv);
      encoder.writeToBuffer(slice, this.configData);
      RenderPass pass = encoder.createRenderPass(this.passLabel, into.getColorTextureView(), java.util.Optional.empty());

      try {
         pass.setPipeline(RenderSystem.getCompiledPipeline(selected));
         pass.setUniform("AuraSpread", slice);
         pass.setUniform("InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(3, 1, 0, 0);
      } catch (Throwable var12) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }
         }

         throw var12;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private void allocate(int width, int height) {
      GpuDevice device = RenderSystem.getDevice();
      this.across = new TextureTarget(this.name + " across", width, height, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null);
      this.down = new TextureTarget(this.name + " down", width, height, com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, null);
      this.acrossConfig = device.createBuffer(() -> this.name + " across config", 136, 16L);
      this.downConfig = device.createBuffer(() -> this.name + " down config", 136, 16L);
      this.acrossSlice = this.acrossConfig.slice();
      this.downSlice = this.downConfig.slice();
      this.configData = BufferUtils.createByteBuffer(16);
   }

   void release() {
      if (this.across != null) {
         this.across.destroyBuffers();
         this.across = null;
      }

      if (this.down != null) {
         this.down.destroyBuffers();
         this.down = null;
      }

      if (this.acrossConfig != null) {
         this.acrossConfig.close();
         this.acrossConfig = null;
      }

      if (this.downConfig != null) {
         this.downConfig.close();
         this.downConfig = null;
      }

      this.acrossSlice = null;
      this.downSlice = null;
      this.configData = null;
   }
}
