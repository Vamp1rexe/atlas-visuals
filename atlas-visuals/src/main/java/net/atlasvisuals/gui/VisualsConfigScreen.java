package net.atlasvisuals.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.impl.client.SpotifyHudModule;
import net.atlasvisuals.module.impl.render.AtmosphereModule;
import net.atlasvisuals.module.impl.render.ColorGradingModule;
import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.impl.render.CustomFovModule;
import net.atlasvisuals.module.impl.render.CustomGlintModule;
import net.atlasvisuals.module.impl.render.FloatingRelicsModule;
import net.atlasvisuals.module.impl.render.HandAuraModule;
import net.atlasvisuals.module.impl.render.HandModelModule;
import net.atlasvisuals.module.impl.render.HandsModule;
import net.atlasvisuals.module.impl.render.HitParticlesModule;
import net.atlasvisuals.module.impl.render.JumpCirclesModule;
import net.atlasvisuals.module.impl.render.MotionBlurModule;
import net.atlasvisuals.module.impl.render.RelicParticlesModule;
import net.atlasvisuals.module.impl.render.SwingAnimationModule;
import net.atlasvisuals.module.impl.render.TimeChangerModule;
import net.atlasvisuals.module.impl.render.TotemSpiritModule;
import net.atlasvisuals.module.impl.render.WaterColorModule;
import net.atlasvisuals.module.impl.render.WeaponInspectModule;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** Top-level page: a search box and the effects by category, each with an on/off switch and a link to its settings. */
@Environment(EnvType.CLIENT)
public final class VisualsConfigScreen extends ScrollingScreen {
   private static final int TOGGLE_WIDTH = 50;
   private static final int SEARCH_TOP = 28;
   private static final int SEARCH_WIDTH = 240;
   private static final String COLORS = "Colors";
   private static final List<Category> CATEGORIES = List.of(
      new Category("Sky & World", List.of(SpotifyHudModule.class, AtmosphereModule.class, WaterColorModule.class, TimeChangerModule.class)),
      new Category(
         "Hands & Items",
         List.of(HandAuraModule.class, HandsModule.class, HandModelModule.class, SwingAnimationModule.class, WeaponInspectModule.class, CustomGlintModule.class)
      ),
      new Category("Particles & Effects", List.of(JumpCirclesModule.class, HitParticlesModule.class, FloatingRelicsModule.class, RelicParticlesModule.class, TotemSpiritModule.class)),
      new Category("Screen", List.of(ColorGradingModule.class, MotionBlurModule.class, CustomFovModule.class))
   );
   private class_342 search;

   public VisualsConfigScreen(class_437 parent) {
      super(class_2561.method_43470("Atlas Visuals"), parent);
   }

   @Override
   int top() {
      return SEARCH_TOP + 24;
   }

   @Override
   void addRows() {
      this.addSearchBox();
      String query = this.search.method_1882().trim().toLowerCase(Locale.ROOT);
      if (query.isEmpty() || COLORS.toLowerCase(Locale.ROOT).contains(query) || "theme".contains(query)) {
         this.row(COLORS, class_4185.method_46430(class_2561.method_43470("Change All Colors"), button ->
            this.field_22787.method_1507(new ColorsScreen(this))
         ).method_46434(this.widgetLeft(), 0, this.widgetWidth(), 20).method_46431());
      }

      int shown = 0;
      for (Category category : CATEGORIES) {
         shown += this.addCategory(category.name, category.modules(), query);
      }

      shown += this.addCategory("Other", uncategorized(), query);
      if (!query.isEmpty() && shown == 0 && !COLORS.toLowerCase(Locale.ROOT).contains(query) && !"theme".contains(query)) {
         this.row("§7No results");
      }
   }

   /** The same box stays across rebuilds, so typing keeps its cursor and focus. */
   private void addSearchBox() {
      int width = Math.min(SEARCH_WIDTH, this.field_22789 - 40);
      int left = (this.field_22789 - width) / 2;
      if (this.search == null) {
         this.search = new class_342(this.field_22793, left, SEARCH_TOP, width, 18, class_2561.method_43470("Search"));
         this.search.method_1880(64);
         this.search.method_1863(text -> {
            this.scrollToTop();
            this.rebuild();
         });
      } else {
         this.search.method_46421(left);
         this.search.method_25358(width);
      }

      this.method_37063(this.search);
      this.method_25395(this.search);
   }

   @Override
   public void method_25394(class_332 graphics, int mouseX, int mouseY, float delta) {
      super.method_25394(graphics, mouseX, mouseY, delta);
      if (this.search.method_1882().isEmpty()) {
         graphics.method_25303(this.field_22793, "Search effects and settings...", this.search.method_46426() + 12, SEARCH_TOP + 5, 0xFF808080);
      }
   }

   private int addCategory(String name, List<ClientModule> modules, String query) {
      List<ClientModule> matches = new ArrayList<>();
      for (ClientModule module : modules) {
         if (matches(module, query)) {
            matches.add(module);
         }
      }

      if (!matches.isEmpty()) {
         this.row("§d§l" + name);
         for (ClientModule module : matches) {
            this.addModuleRow(module);
         }
      }

      return matches.size();
   }

   private void addModuleRow(ClientModule module) {
      int left = this.widgetLeft();
      class_4185 toggle = class_4185.method_46430(onOff(module), button -> {
         module.toggle();
         button.method_25355(onOff(module));
      }).method_46434(left, 0, TOGGLE_WIDTH, 20).method_46431();
      class_4185 settings = class_4185.method_46430(class_2561.method_43470("Settings"), button ->
         this.field_22787.method_1507(new ModuleSettingsScreen(module, this))
      ).method_46434(left + TOGGLE_WIDTH + 4, 0, this.widgetWidth() - TOGGLE_WIDTH - 4, 20).method_46431();
      this.row(module.name(), toggle, settings);
   }

   private static boolean matches(ClientModule module, String query) {
      if (query.isEmpty() || contains(module.name(), query) || contains(module.description(), query)) {
         return true;
      }

      for (Setting setting : module.settings()) {
         if (setting.isVisible()) {
            if (contains(setting.name(), query)) {
               return true;
            }

            if (setting instanceof ModeSetting mode) {
               for (String option : mode.modes()) {
                  if (contains(option, query)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean contains(String text, String query) {
      return text != null && text.toLowerCase(Locale.ROOT).contains(query);
   }

   private static List<ClientModule> uncategorized() {
      Set<ClientModule> left = new LinkedHashSet<>(ModuleRegistry.all());
      left.remove(ModuleRegistry.get(ColorThemeModule.class));
      for (Category category : CATEGORIES) {
         category.modules().forEach(left::remove);
      }

      return new ArrayList<>(left);
   }

   private static class_2561 onOff(ClientModule module) {
      return class_2561.method_43470(module.isEnabled() ? "§aON" : "§7OFF");
   }

   private record Category(String name, List<Class<? extends ClientModule>> types) {
      List<ClientModule> modules() {
         List<ClientModule> found = new ArrayList<>();
         for (Class<? extends ClientModule> type : this.types) {
            ClientModule module = ModuleRegistry.get(type);
            if (module != null) {
               found.add(module);
            }
         }

         return found;
      }
   }
}
