package net.atlasvisuals.module.impl.render;

import java.util.Map;
import net.atlasvisuals.gui.ColorTheme;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleCategory;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.setting.ColorSetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/** The Colors tab: holds the one colour and pushes it into every effect when the player picks a new one. */
@Environment(EnvType.CLIENT)
public final class ColorThemeModule extends ClientModule {
   /** How much darker (below 1) or lighter (above 1) than the theme each second colour should be. */
   private static final Map<String, Float> SHADES = Map.of(
      "second-color", 0.55F,
      "aura-color", 0.55F,
      "sheen-color", 1.6F,
      "sky-text-color", 1.15F,
      "sky-title-color", 1.3F,
      "night-color", 0.12F,
      "sunset-color", 0.8F,
      "aurora-top-color", 1.3F,
      "warning-color", 0.5F,
      "safe-color", 1.7F
   );
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Theme Color", ColorTheme.BASE));

   public ColorThemeModule() {
      super("color-theme", "Colors", "One colour for the sky, hand aura, particles and everything else.", ModuleCategory.VISUALS);
      this.color.onChange(this::shareColor);
   }

   public static ColorThemeModule get() {
      return ModuleRegistry.get(ColorThemeModule.class);
   }

   public ColorSetting color() {
      return this.color;
   }

   /** Applies a colour picked in the tab. This overwrites colours set on the individual effects. */
   public void choose(int argb) {
      this.color.setArgb(argb | 0xFF000000);
      for (ClientModule module : ModuleRegistry.all()) {
         this.applyColors(module);
         for (Setting setting : module.settings()) {
            // The other hit particles have a fixed colour.
            if (module != this && setting instanceof ModeSetting mode && mode.modes().contains("Dust")) {
               mode.set("Dust");
            }
         }
      }
   }

   /** Gives one effect the theme colour, for a fresh install and after its settings are reset. */
   public void applyColors(ClientModule module) {
      if (module != this) {
         int rgb = this.color.argb();
         for (Setting setting : module.settings()) {
            if (setting instanceof ModeSetting mode) {
               useOwnColors(mode);
            } else if (setting instanceof ColorSetting target) {
               target.setArgb(ColorTheme.shade(rgb, SHADES.getOrDefault(setting.id(), 1.0F)));
            }
         }
      }
   }

   /** Colour fields only count in their Manual or Custom mode, so switch to it. */
   private static void useOwnColors(ModeSetting mode) {
      String id = mode.id();
      if (id.equals("color") || id.equals("color-mode") || id.equals("dust-color-mode") || id.equals("theme")) {
         mode.set(mode.modes().contains("Manual") ? "Manual" : "Custom");
      }
   }

   private void shareColor() {
      int before = ColorTheme.version();
      ColorTheme.set(this.color.argb());
      if (ColorTheme.version() != before) {
         PinkWater.themeChanged(Minecraft.getInstance());
      }
   }
}
