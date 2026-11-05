package net.atlasvisuals.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;

/**
 * The game draws its whole sky inside one render pass, and only one can be open at a time. The mod's sky effects
 * open their own between the game's steps, so this stands in for the game's pass and can step aside for them.
 */
@Environment(EnvType.CLIENT)
public final class SkyPass implements RenderPass {
   private final Supplier<RenderPass> reopen;
   private RenderPass real;

   public SkyPass(RenderPass first, Supplier<RenderPass> reopen) {
      this.real = first;
      this.reopen = reopen;
   }

   /** Runs the mod's drawing with the game's pass out of the way. Without a SkyPass it can't draw, and returns false. */
   public static boolean aside(RenderPass pass, Supplier<Boolean> draw) {
      if (pass instanceof SkyPass sky) {
         sky.real.close();
         sky.real = null;
         try {
            return draw.get();
         } finally {
            sky.real = sky.reopen.get();
            RenderSystem.bindDefaultUniforms(sky.real);
         }
      }
      return false;
   }

   @Override
   public void close() {
      if (this.real != null) {
         this.real.close();
         this.real = null;
      }
   }

   @Override
   public void pushDebugGroup(Supplier<String> label) {
      this.real.pushDebugGroup(label);
   }

   @Override
   public void popDebugGroup() {
      this.real.popDebugGroup();
   }

   @Override
   public void writeTimestamp(GpuQueryPool pool, int index) {
      this.real.writeTimestamp(pool, index);
   }

   @Override
   public void setPipeline(CompiledRenderPipeline pipeline) {
      this.real.setPipeline(pipeline);
   }

   @Override
   public void setUniform(String name, GpuTextureView view, GpuSampler sampler) {
      this.real.setUniform(name, view, sampler);
   }

   @Override
   public void setUniform(String name, GpuBuffer buffer) {
      this.real.setUniform(name, buffer);
   }

   @Override
   public void setUniform(String name, GpuBufferSlice slice) {
      this.real.setUniform(name, slice);
   }

   @Override
   public void pushConstants(ByteBuffer data) {
      this.real.pushConstants(data);
   }

   @Override
   public void enableScissor(int x, int y, int width, int height) {
      this.real.enableScissor(x, y, width, height);
   }

   @Override
   public void disableScissor() {
      this.real.disableScissor();
   }

   @Override
   public void setVertexBuffer(int slot, GpuBufferSlice slice) {
      this.real.setVertexBuffer(slot, slice);
   }

   @Override
   public void setIndexBuffer(GpuBuffer buffer, IndexType type) {
      this.real.setIndexBuffer(buffer, type);
   }

   @Override
   public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
      this.real.drawIndexed(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
   }

   @Override
   public void multiDrawIndexed(IntBuffer a, int b, int c, int d) {
      this.real.multiDrawIndexed(a, b, c, d);
   }

   @Override
   public void multiDrawIndexed(PointerBuffer a, IntBuffer b, IntBuffer c, int d) {
      this.real.multiDrawIndexed(a, b, c, d);
   }

   @Override
   public void drawIndexedIndirect(GpuBufferSlice slice, int count) {
      this.real.drawIndexedIndirect(slice, count);
   }

   @Override
   public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> draws, GpuBuffer indices, IndexType type, Collection<String> names, T user) {
      this.real.drawMultipleIndexed(draws, indices, type, names, user);
   }

   @Override
   public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
      this.real.draw(vertexCount, instanceCount, firstVertex, firstInstance);
   }

   @Override
   public void multiDraw(IntBuffer a, int b, int c, int d) {
      this.real.multiDraw(a, b, c, d);
   }

   @Override
   public void multiDraw(IntBuffer a, IntBuffer b, int c) {
      this.real.multiDraw(a, b, c);
   }

   @Override
   public void drawIndirect(GpuBufferSlice slice, int count) {
      this.real.drawIndirect(slice, count);
   }
}