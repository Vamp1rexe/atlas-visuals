package net.atlasvisuals.module.impl.render;

import net.atlasvisuals.gui.ColorTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Draws the glowing sky in the chosen colour. The lyrics skip this, since they have their own colour settings. */
@Environment(EnvType.CLIENT)
final class RecoloredPaint implements SkyPaint {
   private final SkyPaint paint;

   RecoloredPaint(SkyPaint paint) {
      this.paint = paint;
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[] argb) {
      return this.paint.upload(name, width, height, ColorTheme.recolor(argb));
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[][] levels) {
      int[][] out = new int[levels.length][];
      for (int i = 0; i < levels.length; i++) {
         out[i] = ColorTheme.recolor(levels[i]);
      }

      return this.paint.upload(name, width, height, out);
   }

   @Override
   public void gradient(boolean additive, float u0, float u1, float top, float bottom, int topColor, int bottomColor) {
      this.paint.gradient(additive, u0, u1, top, bottom, ColorTheme.recolor(topColor), ColorTheme.recolor(bottomColor));
   }

   @Override
   public void band(SkyImage texture, float u0, float u1, float top, float bottom, int color, boolean additive, int pieces) {
      this.paint.band(texture, u0, u1, top, bottom, ColorTheme.recolor(color), additive, pieces);
   }

   @Override
   public void sprite(SkyImage texture, float u, float y, float width, float height, float spin, int color, boolean additive) {
      this.paint.sprite(texture, u, y, width, height, spin, ColorTheme.recolor(color), additive);
   }

   @Override
   public void glow(float u, float y, float radius, int rgb, float alpha) {
      this.paint.glow(u, y, radius, ColorTheme.recolor(rgb), alpha);
   }
}
