package net.atlasvisuals.module.impl.render;
import net.minecraft.client.renderer.BindGroupLayouts;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import net.atlasvisuals.util.Pipes;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyCanvas implements SkyPaint {
   private static final float RADIUS = 90.0F;
   private static final float WIDEST_HALF_ANGLE = 1.35F;
   private static final int HALO_SIZE = 128;
   private static final int START_BYTES = 4096 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
   private static final RenderPipeline BLEND = pipeline("sky_canvas", BlendFunction.TRANSLUCENT);
   private static final RenderPipeline ADD = pipeline("sky_canvas_add", BlendFunction.LIGHTNING);
   private static SkyTexture plain;
   private static SkyTexture halo;
   private SkyCanvas.Run[] runs = new SkyCanvas.Run[64];
   private int runCount;
   private ByteBufferBuilder bytes;
   private BufferBuilder builder;
   private int quads;
   private float forwardX;
   private float forwardY;
   private float forwardZ;
   private float rightX;
   private float rightZ;
   private float upX;
   private float upY;
   private float upZ;

   private static RenderPipeline pipeline(String name, BlendFunction blend) {
      return Pipes.world(name).withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color").withBindGroupLayout(BindGroupLayouts.SAMPLER0).withColorTargetState(new ColorTargetState(blend)).withCull(false).withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS).build();
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[] argb) {
      return SkyTexture.upload(name, width, height, argb);
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[][] levels) {
      return SkyTexture.upload(name, width, height, levels);
   }

   @Override
   public void gradient(boolean additive, float u0, float u1, float top, float bottom, int topColor, int bottomColor) {
      BufferBuilder out = this.open(plain(), additive, 1);
      this.onSphere(out, u0, top, 0.0F, 0.0F, topColor);
      this.onSphere(out, u1, top, 1.0F, 0.0F, topColor);
      this.onSphere(out, u1, bottom, 1.0F, 1.0F, bottomColor);
      this.onSphere(out, u0, bottom, 0.0F, 1.0F, bottomColor);
   }

   @Override
   public void band(SkyImage texture, float u0, float u1, float top, float bottom, int color, boolean additive, int pieces) {
      BufferBuilder out = this.open((SkyTexture)texture, additive, pieces);

      for (int i = 0; i < pieces; i++) {
         float from = (float)i / pieces;
         float to = (float)(i + 1) / pieces;
         float left = Mth.lerp(from, u0, u1);
         float right = Mth.lerp(to, u0, u1);
         this.onSphere(out, left, top, from, 0.0F, color);
         this.onSphere(out, right, top, to, 0.0F, color);
         this.onSphere(out, right, bottom, to, 1.0F, color);
         this.onSphere(out, left, bottom, from, 1.0F, color);
      }
   }

   @Override
   public void sprite(SkyImage texture, float u, float y, float width, float height, float spin, int color, boolean additive) {
      BufferBuilder out = this.open((SkyTexture)texture, additive, 1);
      this.face(u, y);
      float halfWidth = 90.0F * (float)Math.tan(Math.min(width * (float) (Math.PI / 3) * 0.5F, 1.35F));
      float halfHeight = 90.0F * (float)Math.tan(Math.min(height * (float) (Math.PI / 3) * 0.5F, 1.35F));
      float cos = (float)Math.cos(spin);
      float sin = (float)Math.sin(spin);
      this.onPlane(out, -halfWidth, -halfHeight, cos, sin, 0.0F, 0.0F, color);
      this.onPlane(out, halfWidth, -halfHeight, cos, sin, 1.0F, 0.0F, color);
      this.onPlane(out, halfWidth, halfHeight, cos, sin, 1.0F, 1.0F, color);
      this.onPlane(out, -halfWidth, halfHeight, cos, sin, 0.0F, 1.0F, color);
   }

   @Override
   public void glow(float u, float y, float radius, int rgb, float alpha) {
      if (!(alpha <= 0.003F)) {
         this.sprite(halo(), u, y, radius * 2.0F, radius * 2.0F, 0.0F, ARGB.color(Mth.clamp(alpha, 0.0F, 1.0F), rgb), true);
      }
   }

   void flush(float alpha) {
      if (this.builder != null) {
         BufferBuilder done = this.builder;
         this.builder = null;

         try {
            MeshData mesh = done.build();

            label169: {
               try {
                  if (mesh != null && this.runCount != 0) {
                     GpuBufferSlice vertices = RenderSystem.getDevice().createCommandEncoder().transientMemory().uploadGpu(mesh.vertexBuffer(), DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize(), GpuBuffer.USAGE_VERTEX);
                     AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                     GpuBuffer indexBuffer = indices.getBuffer(this.quads * 6);
                     GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                        .writeTransform(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
                     RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                     RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Atlas sky canvas", target.getColorTextureView(), java.util.Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty());

                     try {
                        RenderPipeline active = null;

                        for (int i = 0; i < this.runCount; i++) {
                           SkyCanvas.Run run = this.runs[i];
                           RenderPipeline wanted = run.additive ? ADD : BLEND;
                           if (wanted != active) {
                              active = wanted;
                              pass.setPipeline(RenderSystem.getCompiledPipeline(wanted));
                              RenderSystem.bindDefaultUniforms(pass);
                              pass.setUniform("DynamicTransforms", transforms);
                              pass.setVertexBuffer(0, vertices);
                              pass.setIndexBuffer(indexBuffer, indices.type());
                           }

                           pass.setUniform("Sampler0", run.texture.view(), run.texture.sampler());
                           pass.drawIndexed(run.count * 6, 1, run.first * 6, 0, 0);
                        }
                     } catch (Throwable var22) {
                        if (pass != null) {
                           try {
                              pass.close();
                           } catch (Throwable var21) {
                              var22.addSuppressed(var21);
                           }
                        }

                        throw var22;
                     }

                     if (pass != null) {
                        pass.close();
                     }
                     break label169;
                  }
               } catch (Throwable var23) {
                  if (mesh != null) {
                     try {
                        mesh.close();
                     } catch (Throwable var20) {
                        var23.addSuppressed(var20);
                     }
                  }

                  throw var23;
               }

               if (mesh != null) {
                  mesh.close();
               }

               return;
            }

            if (mesh != null) {
               mesh.close();
            }
         } finally {
            this.runCount = 0;
            this.quads = 0;
         }
      }
   }

   void clear() {
      BufferBuilder dropped = this.builder;
      this.builder = null;
      this.runCount = 0;
      this.quads = 0;
      if (dropped != null) {
         MeshData ignored = dropped.build();
         if (ignored != null) {
            ignored.close();
         }
      }
   }

   private BufferBuilder open(SkyTexture texture, boolean additive, int count) {
      if (this.builder == null) {
         if (this.bytes == null) {
            this.bytes = new ByteBufferBuilder(START_BYTES);
         }

         this.builder = new BufferBuilder(this.bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      }

      SkyCanvas.Run last = this.runCount == 0 ? null : this.runs[this.runCount - 1];
      if (last == null || last.texture != texture || last.additive != additive) {
         if (this.runCount == this.runs.length) {
            this.runs = Arrays.copyOf(this.runs, this.runCount * 2);
         }

         last = this.runs[this.runCount];
         if (last == null) {
            last = this.runs[this.runCount] = new SkyCanvas.Run();
         }

         last.texture = texture;
         last.additive = additive;
         last.first = this.quads;
         last.count = 0;
         this.runCount++;
      }

      last.count += count;
      this.quads += count;
      return this.builder;
   }

   private void face(float u, float y) {
      double azimuth = (u - 0.5) * (float) (Math.PI * 2);
      double elevation = (0.72F - y) * (float) (Math.PI / 3);
      float cosA = (float)Math.cos(azimuth);
      float sinA = (float)Math.sin(azimuth);
      float cosE = (float)Math.cos(elevation);
      float sinE = (float)Math.sin(elevation);
      this.forwardX = -cosA * cosE;
      this.forwardY = sinE;
      this.forwardZ = -sinA * cosE;
      this.rightX = sinA;
      this.rightZ = -cosA;
      this.upX = cosA * sinE;
      this.upY = cosE;
      this.upZ = sinA * sinE;
   }

   private void onSphere(BufferBuilder out, float u, float y, float s, float t, int color) {
      this.face(u, y);
      out.addVertex(this.forwardX * 90.0F, this.forwardY * 90.0F, this.forwardZ * 90.0F).setUv(s, t).setColor(color);
   }

   private void onPlane(BufferBuilder out, float x, float y, float cos, float sin, float s, float t, int color) {
      float across = x * cos - y * sin;
      float down = x * sin + y * cos;
      out.addVertex(
            this.forwardX * 90.0F + this.rightX * across - this.upX * down,
            this.forwardY * 90.0F - this.upY * down,
            this.forwardZ * 90.0F + this.rightZ * across - this.upZ * down
         )
         .setUv(s, t)
         .setColor(color);
   }

   private static SkyTexture plain() {
      if (plain == null) {
         int[] white = new int[4];
         Arrays.fill(white, -1);
         plain = SkyTexture.upload("plain", 2, 2, white);
      }

      return plain;
   }

   private static SkyTexture halo() {
      if (halo == null) {
         int[] pixels = new int[16384];
         float middle = 64.0F;

         for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
               float d = Math.min(1.0F, (float)Math.hypot(x + 0.5F - middle, y + 0.5F - middle) / middle);
               float rest = 1.0F - d;
               float rest2 = rest * rest;
               pixels[y * 128 + x] = ARGB.color(Mth.clamp(0.85F * rest2 * rest2 + 0.15F * rest, 0.0F, 1.0F), 16777215);
            }
         }

         halo = SkyTexture.upload("halo", 128, 128, pixels);
      }

      return halo;
   }

   @Environment(EnvType.CLIENT)
   private static final class Run {
      SkyTexture texture;
      boolean additive;
      int first;
      int count;
   }
}
