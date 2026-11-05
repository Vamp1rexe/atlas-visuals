package net.atlasvisuals.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import net.atlasvisuals.gui.VisualsConfigScreen;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.impl.client.SpotifyHudModule;
import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.impl.render.HandAuraModule;
import net.atlasvisuals.module.impl.render.JumpCirclesModule;
import net.atlasvisuals.module.setting.ColorSetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.Setting;
import net.atlasvisuals.util.spotify.LyricsQuery;
import net.atlasvisuals.util.spotify.NowPlaying;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Starts the real game, loads a world, turns every effect on and takes screenshots. */
public final class VisualsGameTest implements FabricClientGameTest {
   private static final String TITLE = "Blinding Lights";
   private static final String ARTIST = "The Weeknd";

   @Override
   public void runTest(ClientGameTestContext context) {
      // Straight after loading, with nothing clicked: the normal title screen, no intro.
      context.takeScreenshot("00-title-screen");
      try (TestSingleplayerContext world = context.worldBuilder().create()) {
         world.getClientWorld().waitForChunksRender();
         command(world, "gamerule doDaylightCycle false");
         command(world, "tp @p 0 -58 0 0 0");
         command(world, "fill -8 -61 3 8 -60 16 water");
         command(world, "item replace entity @p weapon.mainhand with diamond_pickaxe");
         context.waitTicks(100);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("01-water-hand-aura-pose");

         // Spotify is faked here so the test does not depend on the app, but the lyrics come from the real LRCLIB lookup.
         context.runOnClient(client -> fakeSong());
         command(world, "time set 18000");
         command(world, "tp @p 0 -58 0 0 -35");
         context.waitTicks(320);
         context.takeScreenshot("02-sky-lyrics-other-artist-night");
         command(world, "time set 6000");
         context.waitTicks(60);
         context.takeScreenshot("03-sky-lyrics-other-artist-day");

         context.runOnClient(client -> client.method_1507(new VisualsConfigScreen(null)));
         context.waitTicks(5);
         context.takeScreenshot("04-menu");
         context.runOnClient(client -> client.method_1507(ModuleScreens.settingsOf(SpotifyHudModule.get())));
         context.waitTicks(5);
         context.takeScreenshot("05-spotify-settings");
         context.runOnClient(client -> client.method_1507(ModuleScreens.settingsOf(ModuleRegistry.get(HandAuraModule.class))));
         context.waitTicks(5);
         context.takeScreenshot("05b-aura-settings");
         context.runOnClient(client -> checkHexInput(client.field_1755));
         context.waitTicks(5);
         context.takeScreenshot("05d-aura-settings-after-hex");
         context.runOnClient(client -> {
            net.minecraft.class_437 viaModMenu = ModuleScreens.viaModMenu();
            System.out.println("[AtlasVisualsTest] Mod Menu config screen: " + (viaModMenu == null ? "Mod Menu not installed" : viaModMenu.getClass().getName()));
            net.minecraft.class_437 modsScreen = ModuleScreens.modMenuList();
            if (modsScreen != null) {
               client.method_1507(modsScreen);
            }
         });
         context.waitTicks(10);
         context.takeScreenshot("05c-modmenu-list");
         context.runOnClient(client -> client.method_1507(null));

         // The Colors tab has to reach the sky, water, aura and particles.
         command(world, "tp @p 0 -58 0 0 0");
         context.runOnClient(client -> client.method_1507(new net.atlasvisuals.gui.ColorsScreen(null)));
         context.waitTicks(5);
         context.takeScreenshot("07-colors-tab");
         context.runOnClient(client -> {
            client.method_1507(null);
            ColorThemeModule.get().choose(0xFF3A70FF);
            checkThemeReached();
         });
         context.waitTicks(100);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("08-theme-blue");
         context.runOnClient(client -> ColorThemeModule.get().choose(0xFF30E060));
         context.waitTicks(100);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("09-theme-green");
         context.runOnClient(client -> ColorThemeModule.get().choose(0xFFFF2020));
         context.waitTicks(100);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("10-theme-red");

         // The first effect-specific look: a jump circle seen from above, with the music sky faded by daylight checks elsewhere.
         context.runOnClient(client -> ModuleRegistry.get(JumpCirclesModule.class).setEnabled(true));
         command(world, "tp @p 0 -60 -6 0 75");
         context.waitTicks(20);
         context.runOnClient(client -> client.field_1724.method_6043());
         context.waitTicks(9);
         context.takeScreenshot("05e-pentagram");
         command(world, "time set 18000");
         for (ClientModule module : ModuleRegistry.all()) {
            context.runOnClient(client -> module.setEnabled(true));
         }

         context.runOnClient(client -> {
            for (ClientModule module : ModuleRegistry.all()) {
               for (Setting setting : module.settings()) {
                  setting.isVisible();
               }
            }
         });
         command(world, "tp @p 0 -58 0 0 -35");
         context.waitTicks(100);
         context.takeScreenshot("05-all-effects");
         command(world, "tp @p 0 -58 0 0 10");
         command(world, "effect give @p jump_boost 5 1");
         context.waitTicks(60);
         context.takeScreenshot("06-all-effects-level");
         context.runOnClient(client -> ColorThemeModule.get().choose(net.atlasvisuals.gui.ColorTheme.BASE));
         context.waitTicks(100);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("11-theme-pink");
         FeatureSweep.run(context, world, VisualsGameTest::fakeSong);
      }

      // Leaving a world and joining another with everything on.
      context.takeScreenshot("12-title-after-leaving");
      try (TestSingleplayerContext world = context.worldBuilder().create()) {
         world.getClientWorld().waitForChunksRender();
         context.waitTicks(60);
         context.takeScreenshot("13-second-world");
      }
   }

   private static void checkThemeReached() {
      int blue = 0xFF3A70FF;
      int moon = net.atlasvisuals.gui.ColorTheme.tinted(new int[]{0xFFFFD040})[0];
      if ((moon & 0xFF) <= (moon >> 16 & 0xFF)) {
         throw new AssertionError("A yellow emoji did not turn blue");
      }

      if (((ColorSetting)find(ModuleRegistry.get(HandAuraModule.class), "glow-color")).argb() != blue
         || ((ColorSetting)find(ModuleRegistry.get(JumpCirclesModule.class), "color")).argb() != blue
         || ((ColorSetting)find(SpotifyHudModule.get(), "sky-glow-color")).argb() != blue
         || net.atlasvisuals.gui.ColorTheme.argb() != blue
         || net.atlasvisuals.gui.ColorTheme.recolor(net.atlasvisuals.gui.ColorTheme.BASE) != blue) {
         throw new AssertionError("The theme colour did not reach every effect");
      }

      System.out.println("[AtlasVisualsTest] Theme colour reached the effects");
   }

   private static void checkHexInput(net.minecraft.class_437 screen) {
      HandAuraModule aura = ModuleRegistry.get(HandAuraModule.class);
      net.minecraft.class_342 field = null;
      for (net.minecraft.class_364 child : screen.method_25396()) {
         if (child instanceof net.minecraft.class_342 text) {
            field = text;
            break;
         }
      }

      if (field == null) {
         throw new AssertionError("No colour field on the Hand Aura page");
      }

      ColorSetting glow = (ColorSetting)find(aura, "glow-color");
      expect(field, glow, "#ff0000", 0xFFFF0000);
      expect(field, glow, "00FF00", 0xFF00FF00);
      expect(field, glow, "#00f", 0xFF0000FF);
      expect(field, glow, "80FFFFFF", 0x80FFFFFF);
      expect(field, glow, "#12", 0x80FFFFFF);
      expect(field, glow, "nonsense", 0x80FFFFFF);
      expect(field, glow, "#22DDFF", 0x8022DDFF);
      expect(field, glow, "FF22DDFF", 0xFF22DDFF);
      System.out.println("[AtlasVisualsTest] Hex colour input OK");
   }

   private static void expect(net.minecraft.class_342 field, ColorSetting setting, String typed, int wanted) {
      field.method_1852(typed);
      if (setting.argb() != wanted) {
         throw new AssertionError("Typed " + typed + ", got " + ColorSetting.hex(setting.argb()) + ", wanted " + ColorSetting.hex(wanted));
      }
   }

   private static void command(TestSingleplayerContext world, String command) {
      world.getServer().runCommand(command);
   }

   private static void fakeSong() {
      try {
         SpotifyHudModule module = SpotifyHudModule.get();
         Object poller = field(SpotifyHudModule.class, "poller").get(module);
         ((ExecutorService)field(poller.getClass(), "executor").get(poller)).shutdownNow();
         skyStaysUp(module);         NowPlaying now = new NowPlaying(
            NowPlaying.Status.PLAYING, TITLE, ARTIST, "After Hours", NowPlaying.Source.SESSION, 200000L, 30000L, System.currentTimeMillis(), 1.0, null
         );
         field(SpotifyHudModule.class, "nowPlaying").set(module, now);
         LyricsQuery query = new LyricsQuery(TITLE, ARTIST, "After Hours", 0).withDurationMillis(200000L);
         Class<?> record = Class.forName(SpotifyHudModule.class.getName() + "$TrackLyrics");
         Constructor<?> make = record.getDeclaredConstructor(String.class, String.class, LyricsQuery.class);
         make.setAccessible(true);
         field(SpotifyHudModule.class, "lyrics").set(module, make.newInstance(TITLE, ARTIST, query));
      } catch (ReflectiveOperationException failure) {
         throw new AssertionError(failure);
      }
   }

   /** The sky follows the music, not the lyrics: up without lyrics, up through a song change, down after a real stop. */
   private static void skyStaysUp(SpotifyHudModule module) throws ReflectiveOperationException {
      Field playing = field(SpotifyHudModule.class, "nowPlaying");
      NowPlaying song = new NowPlaying(
         NowPlaying.Status.PLAYING, TITLE, ARTIST, "After Hours", NowPlaying.Source.SESSION, 200000L, 30000L, System.currentTimeMillis(), 1.0, null
      );
      NowPlaying between = new NowPlaying(
         NowPlaying.Status.PAUSED, TITLE, ARTIST, "After Hours", NowPlaying.Source.SESSION, 200000L, 30000L, System.currentTimeMillis(), 1.0, null
      );
      field(SpotifyHudModule.class, "lyrics").set(module, null);
      playing.set(module, song);
      expectSky(module, true, "a song without lyrics");
      playing.set(module, between);
      expectSky(module, true, "the gap between two songs");
      field(SpotifyHudModule.class, "skyHeldSince").setLong(module, System.nanoTime() - 7000000000L);
      expectSky(module, false, "a real stop");
      playing.set(module, song);
      ((ModeSetting)find(module, "glowing-sky")).set("Off");
      expectSky(module, false, "the sky turned off");
      ((ModeSetting)find(module, "glowing-sky")).set("All Songs");
      expectSky(module, true, "the sky turned back on");
      System.out.println("[AtlasSweep] Sky stays up with music: OK");
   }

   private static void expectSky(SpotifyHudModule module, boolean wanted, String during) {
      if (module.peepPlaying() != wanted) {
         throw new AssertionError("Sky should be " + (wanted ? "up" : "down") + " during " + during);
      }
   }

   private static Setting find(ClientModule module, String id) {
      for (Setting setting : module.settings()) {
         if (setting.id().equals(id)) {
            return setting;
         }
      }

      throw new AssertionError("No setting " + id);
   }

   private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
      Field field = owner.getDeclaredField(name);
      field.setAccessible(true);
      return field;
   }
}
