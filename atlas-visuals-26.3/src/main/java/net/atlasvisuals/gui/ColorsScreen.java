package net.atlasvisuals.gui;

import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

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

   public ColorsScreen(Screen parent) {
      super(Component.literal("Colors"), parent);
   }

   @Override
   void addRows() {
      this.row(THEME_ROW, this.hexField());
      for (int first = 0; first < PRESETS.length; first += PER_ROW) {
         AbstractWidget[] buttons = new AbstractWidget[PER_ROW];
         for (int i = 0; i < PER_ROW; i++) {
            buttons[i] = this.presetButton(PRESETS[first + i], i);
         }

         this.row(first == 0 ? "Presets" : "", buttons);
      }

      this.row("Replaces the colors set on single effects", new AbstractWidget[0]);
   }

   private AbstractWidget hexField() {
      ColorThemeModule theme = ColorThemeModule.get();
      EditBox field = new EditBox(this.font, this.widgetLeft() + SWATCH + GAP, 0, this.widgetWidth() - SWATCH - GAP, 20, Component.literal(THEME_ROW));
      field.setMaxLength(9);
      field.setValue(ColorSetting.display(theme.color().argb()));
      field.setResponder(text -> {
         Integer parsed = ColorSetting.parseHex(text, 0xFF000000);
         field.setTextColor(parsed != null ? 0xFFE0E0E0 : 0xFFFF5555);
         if (parsed != null) {
            theme.choose(parsed);
         }
      });
      return field;
   }

   private AbstractWidget presetButton(Preset preset, int column) {
      int width = (this.widgetWidth() - GAP * (PER_ROW - 1)) / PER_ROW;
      Component label = Component.literal(preset.name).withColor(preset.argb & 0xFFFFFF);
      return Button.builder(label, button -> {
         ColorThemeModule.get().choose(preset.argb);
         this.rebuild();
      }).bounds(this.widgetLeft() + column * (width + GAP), 0, width, 20).build();
   }

   @Override
   void renderRow(GuiGraphicsExtractor graphics, String label, int y) {
      if (THEME_ROW.equals(label)) {
         int left = this.widgetLeft();
         graphics.fill(left, y, left + SWATCH, y + 20, 0xFF000000);
         graphics.fill(left + 1, y + 1, left + SWATCH - 1, y + 19, ColorThemeModule.get().color().argb());
      }
   }

   private record Preset(String name, int argb) {
   }
}
