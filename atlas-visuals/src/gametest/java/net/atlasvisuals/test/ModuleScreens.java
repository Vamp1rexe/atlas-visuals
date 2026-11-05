package net.atlasvisuals.test;

import java.lang.reflect.Constructor;
import net.atlasvisuals.gui.VisualsConfigScreen;
import net.atlasvisuals.module.ClientModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_437;

/** Opens the private settings page and Mod Menu's screens without the test depending on them at compile time. */
final class ModuleScreens {
   private ModuleScreens() {
   }

   static class_437 settingsOf(ClientModule module) {
      try {
         Constructor<?> make = Class.forName("net.atlasvisuals.gui.ModuleSettingsScreen").getDeclaredConstructor(ClientModule.class, class_437.class);
         make.setAccessible(true);
         return (class_437)make.newInstance(module, new VisualsConfigScreen(null));
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }

   static class_437 viaModMenu() {
      if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
         return null;
      }

      try {
         return (class_437)Class.forName("com.terraformersmc.modmenu.ModMenu")
            .getMethod("getConfigScreen", String.class, class_437.class)
            .invoke(null, "atlas_visuals", null);
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }

   static class_437 modMenuList() {
      if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
         return null;
      }

      try {
         return (class_437)Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(class_437.class).newInstance((Object)null);
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }
}
