package net.atlasvisuals.gui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.setting.ActionSetting;
import net.atlasvisuals.module.setting.BooleanSetting;
import net.atlasvisuals.module.setting.ColorSetting;
import net.atlasvisuals.module.setting.KeySetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.NumberSetting;
import net.atlasvisuals.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/** One page per effect, built from that effect's settings, so every setting is editable here. */
@Environment(EnvType.CLIENT)
final class ModuleSettingsScreen extends ScrollingScreen {
   private static final int SWATCH = 20;
   private final ClientModule module;
   private final Map<String, ColorSetting> colorRows = new HashMap<>();
   private KeySetting listening;
   private Button listeningButton;

   ModuleSettingsScreen(ClientModule module, Screen parent) {
      super(Component.literal(module.name()), parent);
      this.module = module;
   }

   @Override
   void addRows() {
      this.colorRows.clear();
      this.listening = null;
      this.listeningButton = null;
      for (Setting setting : this.module.settings()) {
         if (setting.isVisible()) {
            this.addSettingRow(setting);
         }
      }

      this.row("Reset all to default", this.button("Reset", button -> {
         this.module.resetSettings();
         ColorThemeModule.get().applyColors(this.module);
         this.rebuild();
      }));
   }

   private void addSettingRow(Setting setting) {
      if (setting instanceof BooleanSetting toggle) {
         this.row(setting.name(), this.button(onOff(toggle.get()), button -> {
            toggle.set(!toggle.get());
            this.rebuild();
         }));
      } else if (setting instanceof NumberSetting number) {
         this.row(setting.name(), new NumberSlider(this.widgetLeft(), this.widgetWidth(), number));
      } else if (setting instanceof ModeSetting mode) {
         this.row(setting.name(), this.button(mode.get(), button -> {
            mode.next();
            this.rebuild();
         }));
      } else if (setting instanceof ColorSetting color) {
         this.colorRows.put(setting.name(), color);
         this.row(setting.name(), this.colorField(color));
      } else if (setting instanceof KeySetting key) {
         this.row(setting.name(), this.button(keyName(key.value()), button -> {
            this.listening = key;
            this.listeningButton = button;
            button.setMessage(Component.literal("> press a key <"));
         }));
      } else if (setting instanceof ActionSetting action) {
         this.row(setting.name(), this.button("Run", button -> {
            action.run();
            this.rebuild();
         }));
      }
   }

   private Button button(String text, Button.OnPress press) {
      return Button.builder(Component.literal(text), press).bounds(this.widgetLeft(), 0, this.widgetWidth(), 20).build();
   }

   private AbstractWidget colorField(ColorSetting color) {
      EditBox field = new EditBox(this.font, this.widgetLeft() + SWATCH + 4, 0, this.widgetWidth() - SWATCH - 4, 20, Component.literal("Color"));
      field.setMaxLength(9);
      field.setValue(ColorSetting.display(color.argb()));
      field.setResponder(text -> {
         // A half-typed code keeps the last valid colour and turns red until it is complete.
         Integer parsed = ColorSetting.parseHex(text, color.argb());
         field.setTextColor(parsed != null ? 0xFFE0E0E0 : 0xFFFF5555);
         if (parsed != null) {
            color.setArgb(parsed);
         }
      });
      return field;
   }

   @Override
   void renderRow(GuiGraphicsExtractor graphics, String label, int y) {
      ColorSetting color = this.colorRows.get(label);
      if (color != null) {
         int left = this.widgetLeft();
         graphics.fill(left, y, left + SWATCH, y + 20, 0xFF000000);
         graphics.fill(left + 1, y + 1, left + SWATCH - 1, y + 19, color.argb() | 0xFF000000);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.listening != null) {
         int key = event.key();
         this.listening.setValue(key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE ? -1 : key);
         this.listeningButton.setMessage(Component.literal(keyName(this.listening.value())));
         this.listening = null;
         this.listeningButton = null;
         return true;
      }

      return super.keyPressed(event);
   }

   private static String onOff(boolean on) {
      return on ? "§aON" : "§7OFF";
   }

   private static String keyName(int key) {
      if (key < 0) {
         return "None";
      } else if (key >= 1000) {
         return "Mouse " + (key - 1000);
      } else {
         return com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class NumberSlider extends AbstractSliderButton {
      private final NumberSetting setting;

      NumberSlider(int x, int width, NumberSetting setting) {
         super(x, 0, width, 20, Component.literal(setting.formatted()), fraction(setting));
         this.setting = setting;
      }

      private static double fraction(NumberSetting setting) {
         float range = setting.max() - setting.min();
         return range <= 0.0F ? 0.0 : (setting.get() - setting.min()) / range;
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.literal(this.setting.formatted()));
      }

      @Override
      protected void applyValue() {
         this.setting.set(this.setting.min() + (float)this.value * (this.setting.max() - this.setting.min()));
      }
   }
}
