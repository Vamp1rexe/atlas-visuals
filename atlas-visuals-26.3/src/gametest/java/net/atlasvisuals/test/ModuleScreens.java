package net.atlasvisuals.test;

import java.lang.reflect.Constructor;
import net.atlasvisuals.gui.VisualsConfigScreen;
import net.atlasvisuals.module.ClientModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/** Opens the private settings page and Mod Menu's screens without the test depending on them at compile time. */
final class ModuleScreens {
   private ModuleScreens() {
   }

   static Screen settingsOf(ClientModule module) {
      try {
         Constructor<?> make = Class.forName("net.atlasvisuals.gui.ModuleSettingsScreen").getDeclaredConstructor(ClientModule.class, Screen.class);
         make.setAccessible(true);
         return (Screen)make.newInstance(module, new VisualsConfigScreen(null));
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }

   static Screen viaModMenu() {
      if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
         return null;
      }

      try {
         return (Screen)Class.forName("com.terraformersmc.modmenu.ModMenu")
            .getMethod("getConfigScreen", String.class, Screen.class)
            .invoke(null, "atlas_visuals", null);
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }

   static Screen modMenuList() {
      if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
         return null;
      }

      try {
         return (Screen)Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance((Object)null);
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }
}
