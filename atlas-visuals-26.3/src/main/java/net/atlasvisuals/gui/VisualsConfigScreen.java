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
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

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
   private EditBox search;

   public VisualsConfigScreen(Screen parent) {
      super(Component.literal("Atlas Visuals"), parent);
   }

   @Override
   int top() {
      return SEARCH_TOP + 24;
   }

   @Override
   void addRows() {
      this.addSearchBox();
      String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
      if (query.isEmpty() || COLORS.toLowerCase(Locale.ROOT).contains(query) || "theme".contains(query)) {
         this.row(COLORS, Button.builder(Component.literal("Change All Colors"), button ->
            this.minecraft.gui.setScreen(new ColorsScreen(this))
         ).bounds(this.widgetLeft(), 0, this.widgetWidth(), 20).build());
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
      int width = Math.min(SEARCH_WIDTH, this.width - 40);
      int left = (this.width - width) / 2;
      if (this.search == null) {
         this.search = new EditBox(this.font, left, SEARCH_TOP, width, 18, Component.literal("Search"));
         this.search.setMaxLength(64);
         this.search.setResponder(text -> {
            this.scrollToTop();
            this.rebuild();
         });
      } else {
         this.search.setX(left);
         this.search.setWidth(width);
      }

      this.addRenderableWidget(this.search);
      this.setFocused(this.search);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractRenderState(graphics, mouseX, mouseY, delta);
      if (this.search.getValue().isEmpty()) {
         graphics.text(this.font, "Search effects and settings...", this.search.getX() + 12, SEARCH_TOP + 5, 0xFF808080);
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
      Button toggle = Button.builder(onOff(module), button -> {
         module.toggle();
         button.setMessage(onOff(module));
      }).bounds(left, 0, TOGGLE_WIDTH, 20).build();
      Button settings = Button.builder(Component.literal("Settings"), button ->
         this.minecraft.gui.setScreen(new ModuleSettingsScreen(module, this))
      ).bounds(left + TOGGLE_WIDTH + 4, 0, this.widgetWidth() - TOGGLE_WIDTH - 4, 20).build();
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

   private static Component onOff(ClientModule module) {
      return Component.literal(module.isEnabled() ? "§aON" : "§7OFF");
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
