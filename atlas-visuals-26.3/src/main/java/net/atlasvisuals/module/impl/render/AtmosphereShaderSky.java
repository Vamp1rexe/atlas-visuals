package net.atlasvisuals.module.impl.render;
import net.minecraft.client.renderer.BindGroupLayouts;
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
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class AtmosphereShaderSky {
   static final String OFF = "Off";
   static final String SPACE = "Space";
   static final String NORTHERN_LIGHTS = "Northern Lights";
   static final String RINGED_PLANET = "Ringed Planet";
   static final String BLACK_HOLE = "Black Hole";
   private static final List<String> THEMES = List.of("Space", "Northern Lights", "Ringed Planet", "Black Hole");
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final RenderPipeline PIPELINE = Pipes.world("sky_theme").withVertexShader(Pipes.id("core/sky_theme")).withFragmentShader(Pipes.id("core/sky_theme")).withBindGroupLayout(Pipes.uniform("AtlasSky")).withCull(false).withVertexBinding(0, DefaultVertexFormat.POSITION).withPrimitiveTopology(PrimitiveTopology.QUADS).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
   private static final int UNIFORM_SIZE = 112;
   private static final float CUBE = 10.0F;
   private static final int CUBE_INDICES = 36;
   private static final float MAX_STEP = 0.1F;
   private static final float TIME_WRAP = 4096.0F;
   private GpuBuffer cube;
   private GpuBuffer uniforms;
   private ByteBuffer uniformData;
   private boolean invalidLogged;
   private float time;
   private long lastNanos;

   boolean render(
      String theme,
      float visibility,
      float brightness,
      float speed,
      float stars,
      float effect,
      float band,
      int low,
      int high,
      int bandColor,
      int zenith,
      int horizon
   ) {
      int mode = THEMES.indexOf(theme);
      long now = System.nanoTime();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min((float)(now - this.lastNanos) / 1.0E9F, 0.1F);
      this.lastNanos = now;
      this.time = (this.time + dt * speed) % 4096.0F;
      if (mode >= 0 && !(visibility <= 0.002F)) {
         GpuDevice device = RenderSystem.getDevice();
         if (!net.atlasvisuals.util.Pipes.ready(PIPELINE)) {
            if (!this.invalidLogged) {
               this.invalidLogged = true;
               LOGGER.warn("Atmosphere sky shader {} did not compile; using the gradient sky", PIPELINE);
            }

            return false;
         } else {
            RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (target.width > 0 && target.height > 0) {
               if (this.cube == null) {
                  this.cube = buildCube(device);
               }

               if (this.uniforms == null) {
                  this.uniforms = device.createBuffer(() -> "Atlas sky theme settings", 136, 112L);
                  this.uniformData = BufferUtils.createByteBuffer(112);
               }

               this.putVec(0, mode, visibility, this.time, brightness);
               this.putVec(16, stars, effect, band, 1.0F);
               this.putColor(32, low);
               this.putColor(48, high);
               this.putColor(64, bandColor);
               this.putColor(80, zenith);
               this.putColor(96, horizon);
               CommandEncoder encoder = device.createCommandEncoder();
               encoder.writeToBuffer(this.uniforms.slice(), this.uniformData);
               GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                  .writeTransform(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
               AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
               RenderPass pass = encoder.createRenderPass(
                  () -> "Atlas sky theme", target.getColorTextureView(), java.util.Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty()
               );

               try {
                  this.drawSky(pass, PIPELINE, transforms, indices);
               } catch (Throwable var26) {
                  if (pass != null) {
                     try {
                        pass.close();
                     } catch (Throwable var25) {
                        var26.addSuppressed(var25);
                     }
                  }

                  throw var26;
               }

               if (pass != null) {
                  pass.close();
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void drawSky(RenderPass pass, RenderPipeline pipeline, GpuBufferSlice transforms, AutoStorageIndexBuffer indices) {
      pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
      RenderSystem.bindDefaultUniforms(pass);
      pass.setUniform("DynamicTransforms", transforms);
      pass.setUniform("AtlasSky", this.uniforms.slice());
      pass.setVertexBuffer(0, this.cube.slice());
      pass.setIndexBuffer(indices.getBuffer(36), indices.type());
      pass.drawIndexed(36, 1, 0, 0, 0);
   }

   void close() {
      if (this.cube != null) {
         this.cube.close();
      }

      if (this.uniforms != null) {
         this.uniforms.close();
      }

      this.cube = null;
      this.uniforms = null;
      this.uniformData = null;
      this.lastNanos = 0L;
      this.invalidLogged = false;
   }

   private void putVec(int offset, float x, float y, float z, float w) {
      this.uniformData.putFloat(offset, x);
      this.uniformData.putFloat(offset + 4, y);
      this.uniformData.putFloat(offset + 8, z);
      this.uniformData.putFloat(offset + 12, w);
   }

   private void putColor(int offset, int rgb) {
      this.putVec(offset, (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
   }

   private static GpuBuffer buildCube(GpuDevice device) {
      ByteBufferBuilder bytes = new ByteBufferBuilder(24 * DefaultVertexFormat.POSITION.getVertexSize());

      GpuBuffer var5;
      try {
         BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
         float s = 10.0F;
         face(builder, s, -s, -s, s, s, -s, s, s, s, s, -s, s);
         face(builder, -s, -s, s, -s, s, s, -s, s, -s, -s, -s, -s);
         face(builder, -s, s, -s, -s, s, s, s, s, s, s, s, -s);
         face(builder, -s, -s, s, -s, -s, -s, s, -s, -s, s, -s, s);
         face(builder, s, -s, s, s, s, s, -s, s, s, -s, -s, s);
         face(builder, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s);
         MeshData mesh = builder.buildOrThrow();

         try {
            var5 = device.createBuffer(() -> "Atlas sky theme cube", 32, mesh.vertexBuffer());
         } catch (Throwable var9) {
            if (mesh != null) {
               try {
                  mesh.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (mesh != null) {
            mesh.close();
         }
      } catch (Throwable var10) {
         try {
            bytes.close();
         } catch (Throwable var7) {
            var10.addSuppressed(var7);
         }

         throw var10;
      }

      bytes.close();
      return var5;
   }

   private static void face(
      BufferBuilder builder, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz
   ) {
      builder.addVertex(ax, ay, az);
      builder.addVertex(bx, by, bz);
      builder.addVertex(cx, cy, cz);
      builder.addVertex(dx, dy, dz);
   }
}
