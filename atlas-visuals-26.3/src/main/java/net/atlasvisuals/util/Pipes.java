package net.atlasvisuals.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** What the mod's render pipelines have in common. */
@Environment(EnvType.CLIENT)
public final class Pipes {
   private static final Set<RenderPipeline> BROKEN = new HashSet<>();

   private Pipes() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("atlas_visuals", path);
   }

   /** Drawn in the world, with the usual matrices. */
   public static RenderPipeline.Builder world(String name) {
      return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
         .withLocation(id("pipeline/" + name))
         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
   }

   /** One triangle over the whole screen, running the mod's fragment shader of that name. */
   public static RenderPipeline.Builder screen(String name) {
      return RenderPipeline.builder()
         .withLocation(id("pipeline/" + name))
         .withVertexShader("core/screenquad")
         .withFragmentShader(id("core/" + name))
         .withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
   }

   public static BindGroupLayout samplers(String... names) {
      BindGroupLayout.Builder builder = BindGroupLayout.builder();
      for (String name : names) {
         builder.withUniform(name, UniformType.COMBINED_IMAGE_SAMPLER);
      }
      return builder.build();
   }

   public static BindGroupLayout uniform(String name) {
      return BindGroupLayout.builder().withUniform(name, UniformType.UNIFORM_BUFFER).build();
   }

   /** Compiles on first use. A shader that fails is not tried again every frame. */
   public static boolean ready(RenderPipeline pipeline) {
      if (BROKEN.contains(pipeline)) {
         return false;
      }
      try {
         if (RenderSystem.getCompiledPipelineNullable(pipeline) != null) {
            return true;
         }
      } catch (RuntimeException failure) {
         org.slf4j.LoggerFactory.getLogger("AtlasVisuals").warn("Shader for {} failed", pipeline, failure);
      }
      BROKEN.add(pipeline);
      return false;
   }
}