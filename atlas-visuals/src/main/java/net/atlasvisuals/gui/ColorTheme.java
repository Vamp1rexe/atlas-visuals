package net.atlasvisuals.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The colour chosen in the Colors tab. The glowing sky, its fog and the water are designed around one pink,
 * so {@link #recolor} moves that pink to the chosen colour and keeps their light and dark shades.
 */
@Environment(EnvType.CLIENT)
public final class ColorTheme {
   /** The pink the sky and water were designed with. With this colour, {@link #recolor} changes nothing. */
   public static final int BASE = 0xFFFF50B0;
   private static final float BASE_HUE = hue(BASE);
   private static final float BASE_SATURATION = saturation(BASE);
   private static volatile Shift shift = new Shift(BASE, 0.0F, 1.0F, 1.0F);
   private static volatile int version;

   private ColorTheme() {
   }

   public static void set(int argb) {
      int rgb = argb | 0xFF000000;
      if (rgb != shift.rgb) {
         // A grey or white theme has no hue of its own, so it only removes saturation.
         float sat = saturation(rgb);
         float turn = sat > 0.0F ? hue(rgb) - BASE_HUE : 0.0F;
         float value = Math.max(rgb >> 16 & 0xFF, Math.max(rgb >> 8 & 0xFF, rgb & 0xFF)) / 255.0F;
         shift = new Shift(rgb, turn, sat / BASE_SATURATION, value);
         version++;
      }
   }

   public static int argb() {
      return shift.rgb;
   }

   /** Changes whenever the colour does, for things that must rebuild textures or chunks. */
   public static int version() {
      return version;
   }

   public static int recolor(int argb) {
      Shift now = shift;
      if (now.rgb == BASE) {
         return argb;
      }

      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int max = Math.max(r, Math.max(g, b));
      int min = Math.min(r, Math.min(g, b));
      if (max == 0) {
         return argb;
      }

      float saturation = Math.min(1.0F, (float)(max - min) / max * now.saturation);
      float value = max / 255.0F * now.value;
      float hue = hue(argb) + now.turn;
      return argb & 0xFF000000 | hsv(hue, saturation, value);
   }

   public static int[] recolor(int[] pixels) {
      if (shift.rgb == BASE) {
         return pixels;
      }

      int[] out = new int[pixels.length];
      for (int i = 0; i < pixels.length; i++) {
         out[i] = pixels[i] >>> 24 == 0 ? pixels[i] : recolor(pixels[i]);
      }

      return out;
   }

   /** Paints a colour in another one, keeping how bright and how pale it was. */
   public static int tint(int argb, int target) {
      int max = Math.max(argb >> 16 & 0xFF, Math.max(argb >> 8 & 0xFF, argb & 0xFF));
      int targetMax = Math.max(target >> 16 & 0xFF, Math.max(target >> 8 & 0xFF, target & 0xFF));
      return argb & 0xFF000000 | hsv(hue(target), (float)Math.sqrt(saturation(argb)) * saturation(target), max / 255.0F * (targetMax / 255.0F));
   }

   public static int[] tinted(int[] pixels) {
      int target = shift.rgb;
      int[] out = new int[pixels.length];
      for (int i = 0; i < pixels.length; i++) {
         out[i] = pixels[i] >>> 24 == 0 ? pixels[i] : tint(pixels[i], target);
      }

      return out;
   }

   /** A darker or lighter version of a colour: amount 0 is black, 1 is the colour, 2 is white. */
   public static int shade(int argb, float amount) {
      int alpha = argb & 0xFF000000;
      float k = amount <= 1.0F ? amount : 2.0F - amount;
      float white = amount <= 1.0F ? 0.0F : (amount - 1.0F) * 255.0F;
      int r = Math.round((argb >> 16 & 0xFF) * k + white);
      int g = Math.round((argb >> 8 & 0xFF) * k + white);
      int b = Math.round((argb & 0xFF) * k + white);
      return alpha | r << 16 | g << 8 | b;
   }

   private static float hue(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int max = Math.max(r, Math.max(g, b));
      int min = Math.min(r, Math.min(g, b));
      if (max == min) {
         return 0.0F;
      }

      float range = max - min;
      float sector = max == r ? (g - b) / range : (max == g ? (b - r) / range + 2.0F : (r - g) / range + 4.0F);
      return (sector < 0.0F ? sector + 6.0F : sector) / 6.0F;
   }

   private static float saturation(int rgb) {
      int max = Math.max(rgb >> 16 & 0xFF, Math.max(rgb >> 8 & 0xFF, rgb & 0xFF));
      int min = Math.min(rgb >> 16 & 0xFF, Math.min(rgb >> 8 & 0xFF, rgb & 0xFF));
      return max == 0 ? 0.0F : (float)(max - min) / max;
   }

   private static int hsv(float hue, float saturation, float value) {
      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      int sector = (int)h % 6;
      float f = h - (int)h;
      float p = value * (1.0F - saturation);
      float q = value * (1.0F - saturation * f);
      float t = value * (1.0F - saturation * (1.0F - f));
      float r;
      float g;
      float b;
      switch (sector) {
         case 0 -> { r = value; g = t; b = p; }
         case 1 -> { r = q; g = value; b = p; }
         case 2 -> { r = p; g = value; b = t; }
         case 3 -> { r = p; g = q; b = value; }
         case 4 -> { r = t; g = p; b = value; }
         default -> { r = value; g = p; b = q; }
      }

      return Math.round(r * 255.0F) << 16 | Math.round(g * 255.0F) << 8 | Math.round(b * 255.0F);
   }

   private record Shift(int rgb, float turn, float saturation, float value) {
   }
}
