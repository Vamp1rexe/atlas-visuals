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
import net.minecraft.class_11908;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_357;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

/** One page per effect, built from that effect's settings, so every setting is editable here. */
@Environment(EnvType.CLIENT)
final class ModuleSettingsScreen extends ScrollingScreen {
   private static final int SWATCH = 20;
   private final ClientModule module;
   private final Map<String, ColorSetting> colorRows = new HashMap<>();
   private KeySetting listening;
   private class_4185 listeningButton;

   ModuleSettingsScreen(ClientModule module, class_437 parent) {
      super(class_2561.method_43470(module.name()), parent);
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
            button.method_25355(class_2561.method_43470("> press a key <"));
         }));
      } else if (setting instanceof ActionSetting action) {
         this.row(setting.name(), this.button("Run", button -> {
            action.run();
            this.rebuild();
         }));
      }
   }

   private class_4185 button(String text, class_4185.class_4241 press) {
      return class_4185.method_46430(class_2561.method_43470(text), press).method_46434(this.widgetLeft(), 0, this.widgetWidth(), 20).method_46431();
   }

   private class_339 colorField(ColorSetting color) {
      class_342 field = new class_342(this.field_22793, this.widgetLeft() + SWATCH + 4, 0, this.widgetWidth() - SWATCH - 4, 20, class_2561.method_43470("Color"));
      field.method_1880(9);
      field.method_1852(ColorSetting.display(color.argb()));
      field.method_1863(text -> {
         // A half-typed code keeps the last valid colour and turns red until it is complete.
         Integer parsed = ColorSetting.parseHex(text, color.argb());
         field.method_1868(parsed != null ? 0xFFE0E0E0 : 0xFFFF5555);
         if (parsed != null) {
            color.setArgb(parsed);
         }
      });
      return field;
   }

   @Override
   void renderRow(class_332 graphics, String label, int y) {
      ColorSetting color = this.colorRows.get(label);
      if (color != null) {
         int left = this.widgetLeft();
         graphics.method_25294(left, y, left + SWATCH, y + 20, 0xFF000000);
         graphics.method_25294(left + 1, y + 1, left + SWATCH - 1, y + 19, color.argb() | 0xFF000000);
      }
   }

   @Override
   public boolean method_25404(class_11908 event) {
      if (this.listening != null) {
         int key = event.comp_4795();
         this.listening.setValue(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key);
         this.listeningButton.method_25355(class_2561.method_43470(keyName(this.listening.value())));
         this.listening = null;
         this.listeningButton = null;
         return true;
      }

      return super.method_25404(event);
   }

   private static String onOff(boolean on) {
      return on ? "§aON" : "§7OFF";
   }

   private static String keyName(int key) {
      if (key < 0) {
         return "None";
      } else if (key >= 1000) {
         return "Mouse " + (key - 1000 + 1);
      } else {
         String name = GLFW.glfwGetKeyName(key, 0);
         // GLFW has no printable name for keys like F8, so those use the game's own key name.
         return (name != null ? name : net.minecraft.class_3675.class_307.field_1668.method_1447(key).method_27445().getString()).toUpperCase(Locale.ROOT);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class NumberSlider extends class_357 {
      private final NumberSetting setting;

      NumberSlider(int x, int width, NumberSetting setting) {
         super(x, 0, width, 20, class_2561.method_43470(setting.formatted()), fraction(setting));
         this.setting = setting;
      }

      private static double fraction(NumberSetting setting) {
         float range = setting.max() - setting.min();
         return range <= 0.0F ? 0.0 : (setting.get() - setting.min()) / range;
      }

      @Override
      protected void method_25346() {
         this.method_25355(class_2561.method_43470(this.setting.formatted()));
      }

      @Override
      protected void method_25344() {
         this.setting.set(this.setting.min() + (float)this.field_22753 * (this.setting.max() - this.setting.min()));
      }
   }
}
