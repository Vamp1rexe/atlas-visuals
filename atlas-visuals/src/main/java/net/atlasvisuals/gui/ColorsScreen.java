package net.atlasvisuals.gui;

import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** The Colors tab: pick one colour, by preset or hex code, and every effect takes it on at once. */
@Environment(EnvType.CLIENT)
public final class ColorsScreen extends ScrollingScreen {
   private static final String THEME_ROW = "Theme Color";
   private static final int SWATCH = 20;
   private static final int PER_ROW = 3;
   private static final int GAP = 4;
   private static final Preset[] PRESETS = {
      new Preset("Pink", ColorTheme.BASE),
      new Preset("Red", 0xFFFF2020),
      new Preset("Orange", 0xFFFF8A1E),
      new Preset("Yellow", 0xFFFFE030),
      new Preset("Green", 0xFF30E060),
      new Preset("Cyan", 0xFF20E0F0),
      new Preset("Blue", 0xFF3A70FF),
      new Preset("Purple", 0xFF9B4DFF),
      new Preset("White", 0xFFFFFFFF)
   };

   public ColorsScreen(class_437 parent) {
      super(class_2561.method_43470("Colors"), parent);
   }

   @Override
   void addRows() {
      this.row(THEME_ROW, this.hexField());
      for (int first = 0; first < PRESETS.length; first += PER_ROW) {
         class_339[] buttons = new class_339[PER_ROW];
         for (int i = 0; i < PER_ROW; i++) {
            buttons[i] = this.presetButton(PRESETS[first + i], i);
         }

         this.row(first == 0 ? "Presets" : "", buttons);
      }

      this.row("Replaces the colors set on single effects", new class_339[0]);
   }

   private class_339 hexField() {
      ColorThemeModule theme = ColorThemeModule.get();
      class_342 field = new class_342(this.field_22793, this.widgetLeft() + SWATCH + GAP, 0, this.widgetWidth() - SWATCH - GAP, 20, class_2561.method_43470(THEME_ROW));
      field.method_1880(9);
      field.method_1852(ColorSetting.display(theme.color().argb()));
      field.method_1863(text -> {
         Integer parsed = ColorSetting.parseHex(text, 0xFF000000);
         field.method_1868(parsed != null ? 0xFFE0E0E0 : 0xFFFF5555);
         if (parsed != null) {
            theme.choose(parsed);
         }
      });
      return field;
   }

   private class_339 presetButton(Preset preset, int column) {
      int width = (this.widgetWidth() - GAP * (PER_ROW - 1)) / PER_ROW;
      class_2561 label = class_2561.method_43470(preset.name).method_54663(preset.argb & 0xFFFFFF);
      return class_4185.method_46430(label, button -> {
         ColorThemeModule.get().choose(preset.argb);
         this.rebuild();
      }).method_46434(this.widgetLeft() + column * (width + GAP), 0, width, 20).method_46431();
   }

   @Override
   void renderRow(class_332 graphics, String label, int y) {
      if (THEME_ROW.equals(label)) {
         int left = this.widgetLeft();
         graphics.method_25294(left, y, left + SWATCH, y + 20, 0xFF000000);
         graphics.method_25294(left + 1, y + 1, left + SWATCH - 1, y + 19, ColorThemeModule.get().color().argb());
      }
   }

   private record Preset(String name, int argb) {
   }
}
