package net.atlasvisuals.module.impl.render;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.GpuFormat;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
final class SkyTexture implements SkyImage {
   private final int width;
   private final int height;
   private boolean mipmapped;
   private GpuTexture texture;
   private GpuTextureView view;

   private SkyTexture(int width, int height, GpuTexture texture, GpuTextureView view) {
      this.width = width;
      this.height = height;
      this.texture = texture;
      this.view = view;
   }

   static SkyTexture upload(String name, int width, int height, int[][] levels) {
      if (levels.length <= 1) {
         return upload(name, width, height, levels.length == 0 ? new int[0] : levels[0]);
      } else {
         for (int level = 0; level < levels.length; level++) {
            if (levels[level].length < SkyMips.size(width, level) * SkyMips.size(height, level)) {
               throw new IllegalArgumentException("Sky texture " + name + " is missing pixels at mip level " + level);
            }
         }

         String label = "atlas_visuals:sky/" + name;
         GpuDevice device = RenderSystem.getDevice();
         GpuTexture texture = device.createTexture(() -> label, 5, GpuFormat.RGBA8_UNORM, width, height, 1, levels.length);

         try {
            for (int levelx = 0; levelx < levels.length; levelx++) {
               int w = SkyMips.size(width, levelx);
               int h = SkyMips.size(height, levelx);
               int[] argb = levels[levelx];
               NativeImage image = new NativeImage(w, h, false);

               try {
                  IntBuffer pixels = MemoryUtil.memIntBuffer(image.getPointer(), w * h);

                  for (int i = 0; i < w * h; i++) {
                     pixels.put(i, ARGB.toABGR(argb[i]));
                  }

                  device.createCommandEncoder().writeToTexture(texture, image, levelx, 0, 0, 0);
               } catch (Throwable var15) {
                  try {
                     image.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }

                  throw var15;
               }

               image.close();
            }

            SkyTexture made = new SkyTexture(width, height, texture, device.createTextureView(texture));
            made.mipmapped = true;
            return made;
         } catch (RuntimeException var16) {
            texture.close();
            throw var16;
         }
      }
   }

   static SkyTexture upload(String name, int width, int height, int[] argb) {
      if (argb.length < width * height) {
         throw new IllegalArgumentException("Sky texture " + name + " is missing pixels");
      } else {
         String label = "atlas_visuals:sky/" + name;
         GpuDevice device = RenderSystem.getDevice();
         NativeImage image = new NativeImage(width, height, false);

         SkyTexture e;
         try {
            IntBuffer pixels = MemoryUtil.memIntBuffer(image.getPointer(), width * height);

            for (int i = 0; i < width * height; i++) {
               pixels.put(i, ARGB.toABGR(argb[i]));
            }

            GpuTexture texture = device.createTexture(() -> label, 5, GpuFormat.RGBA8_UNORM, width, height, 1, 1);

            try {
               device.createCommandEncoder().writeToTexture(texture, image);
               e = new SkyTexture(width, height, texture, device.createTextureView(texture));
            } catch (RuntimeException var11) {
               texture.close();
               throw var11;
            }
         } catch (Throwable var12) {
            try {
               image.close();
            } catch (Throwable var10) {
               var12.addSuppressed(var10);
            }

            throw var12;
         }

         image.close();
         return e;
      }
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   GpuTextureView view() {
      return this.view;
   }

   GpuSampler sampler() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, this.mipmapped);
   }

   /** Takes over the image of another texture of the same size, so everything using this one shows it straight away. */
   void replaceWith(SkyTexture other) {
      this.close();
      this.texture = other.texture;
      this.view = other.view;
      this.mipmapped = other.mipmapped;
      other.texture = null;
      other.view = null;
   }

   @Override
   public void close() {
      if (this.view != null) {
         this.view.close();
      }

      if (this.texture != null) {
         this.texture.close();
      }

      this.view = null;
      this.texture = null;
   }
}
