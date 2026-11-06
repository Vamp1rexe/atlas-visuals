package net.atlasvisuals.test;

import com.sun.management.ThreadMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.atlasvisuals.config.VisualsConfig;
import net.atlasvisuals.gui.VisualsConfigScreen;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.impl.client.SpotifyHudModule;
import net.atlasvisuals.module.impl.render.ColorThemeModule;
import net.atlasvisuals.module.setting.ActionSetting;
import net.atlasvisuals.module.setting.BooleanSetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.NumberSetting;
import net.atlasvisuals.module.setting.Setting;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_5498;

/** Goes through every effect: what each one costs, every setting value, and the situations that tend to break effects. */
final class FeatureSweep {
   private static final int FRAMES = 200;
   private static final ThreadMXBean THREADS = (ThreadMXBean)ManagementFactory.getThreadMXBean();

   private FeatureSweep() {
   }

   static void run(ClientGameTestContext context, TestSingleplayerContext world, Runnable fakeSong) {
      setUpScene(context, world);
      List<ClientModule> effects = new ArrayList<>(ModuleRegistry.all());
      effects.remove(ColorThemeModule.get());
      watchForHangs();
      if (System.getenv("ATLAS_QUICK") != null) {
         context.runOnClient(client -> effects.forEach(module -> module.setEnabled(true)));
         context.waitTicks(40);
         situations(context, world);
         everySetting(context, effects);
         everyPage(context);
         brokenConfig(context);
         System.out.println("[AtlasSweep] Finished");
         return;
      }

      context.runOnClient(client -> effects.forEach(module -> module.setEnabled(false)));
      context.getInput().resizeWindow(1920, 1080);
      context.waitTicks(40);
      Cost baseline = measure(context, world, "nothing on", null);
      Cost second = measure(context, world, "nothing on (again)", null);
      float base = Math.min(baseline.millisPerFrame, second.millisPerFrame);
      System.out.println("[AtlasSweep] +ms is on top of the frame time with nothing on");

      for (ClientModule module : effects) {
         context.runOnClient(client -> {
            module.setEnabled(true);
            if (module == SpotifyHudModule.get()) {
               fakeSong.run();
            }
         });
         context.waitTicks(module == SpotifyHudModule.get() ? 200 : 40);
         Cost cost = measure(context, world, module.id(), base);
         context.takeScreenshot("sweep-" + module.id());
         context.runOnClient(client -> module.setEnabled(false));
         if (cost.millisPerFrame - base > 2.0F) {
            System.out.println("[AtlasSweep] EXPENSIVE " + module.id());
         }
      }

      measure(context, world, "nothing on (end)", base);
      context.runOnClient(client -> {
         for (String id : List.of("spotify-hud", "hand-aura", "hand-pose", "water-color")) {
            ModuleRegistry.all().stream().filter(module -> module.id().equals(id)).forEach(module -> module.setEnabled(true));
         }

         fakeSong.run();
      });
      context.waitTicks(200);
      measure(context, world, "fresh install set", base);
      context.runOnClient(client -> effects.forEach(module -> module.setEnabled(true)));
      context.waitTicks(40);
      measure(context, world, "everything on", base);
      context.takeScreenshot("sweep-everything");
      context.getInput().resizeWindow(854, 480);
      context.waitTicks(10);

      situations(context, world);
      everySetting(context, effects);
      everyPage(context);
      brokenConfig(context);
      System.out.println("[AtlasSweep] Finished");
   }

   /** Writes down what the game threads are doing whenever they have not moved for a few seconds. */
   private static void watchForHangs() {
      Thread watcher = new Thread(() -> {
         java.util.Map<String, String> last = new java.util.HashMap<>();
         while (true) {
            try {
               Thread.sleep(3000L);
            } catch (InterruptedException stopped) {
               return;
            }

            for (java.util.Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
               String name = entry.getKey().getName();
               if (name.equals("Render thread") || name.equals("Server thread")) {
                  StringBuilder text = new StringBuilder();
                  StackTraceElement[] frames = entry.getValue();
                  for (int i = 0; i < Math.min(14, frames.length); i++) {
                     text.append("\n    ").append(frames[i]);
                  }

                  if (text.toString().equals(last.put(name, text.toString()))) {
                     System.out.println("[AtlasHang] " + name + " unmoved for 3s:" + text);
                  }
               }
            }
         }
      }, "Atlas test watcher");
      watcher.setDaemon(true);
      watcher.start();
   }

   private static void setUpScene(ClientGameTestContext context, TestSingleplayerContext world) {
      command(world, "time set 18000");
      command(world, "fill -8 -61 3 8 -60 16 water");
      command(world, "tp @p 0 -58 0 0 -5");
      command(world, "item replace entity @p weapon.mainhand with diamond_sword[enchantments={sharpness:5}]");
      command(world, "summon zombie 0 -60 3 {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}");
      context.runOnClient(client -> {
         client.field_1690.method_42524().method_41748(260);
         client.field_1690.method_42433().method_41748(false);
         // Otherwise an idle window is held at 30 FPS.
         client.field_1690.method_61970().method_41748(net.minecraft.class_9927.field_52743);
      });
      world.getClientWorld().waitForChunksRender();
   }

   /** Plays normally while measuring, so effects that react to jumps, hits or swings actually run. */
   private static Cost measure(ClientGameTestContext context, TestSingleplayerContext world, String name, Float base) {
      long[] before = new long[2];
      context.runOnClient(client -> {
         before[0] = THREADS.getCurrentThreadCpuTime();
         before[1] = THREADS.getCurrentThreadAllocatedBytes();
      });
      long start = System.nanoTime();
      long[] fps = new long[2];
      for (int i = 0; i < FRAMES; i += 10) {
         boolean settled = i >= 40;
         context.runOnClient(client -> {
            play(client);
            if (settled) {
               fps[0] += client.method_47599();
               fps[1]++;
            }
         });
         context.waitTicks(10);
      }

      float seconds = (System.nanoTime() - start) / 1.0E9F;
      long[] after = new long[2];
      context.runOnClient(client -> {
         after[0] = THREADS.getCurrentThreadCpuTime();
         after[1] = THREADS.getCurrentThreadAllocatedBytes();
      });
      float averageFps = (float)fps[0] / fps[1];
      float frames = averageFps * seconds;
      Cost cost = new Cost(1000.0F / averageFps);
      System.out.printf(
         "[AtlasSweep] %-20s fps %6.0f  %6.2f ms/frame %7s  cpu %6.2f ms  alloc %7.1f KB/frame%n",
         name,
         averageFps,
         cost.millisPerFrame,
         base == null ? "" : String.format("%+.2f", cost.millisPerFrame - base),
         (after[0] - before[0]) / 1.0E6F / frames,
         (after[1] - before[1]) / 1024.0F / frames
      );
      return cost;
   }

   private static void play(class_310 client) {
      if (client.field_1724 == null || client.field_1761 == null || client.field_1687 == null) {
         return;
      }

      if (client.field_1724.method_24828()) {
         client.field_1724.method_6043();
      }

      client.field_1724.method_6104(net.minecraft.class_1268.field_5808);
      for (class_1297 entity : client.field_1687.method_18112()) {
         if (entity != client.field_1724 && entity.method_5858(client.field_1724) < 25.0) {
            client.field_1761.method_2918(client.field_1724, entity);
            break;
         }
      }
   }

   /** Everything on, through the situations that tend to break rendering. */
   private static void situations(ClientGameTestContext context, TestSingleplayerContext world) {
      step(context, world, "third-person", "tp @p 0 -58 0 0 0", client -> client.field_1690.method_31043(class_5498.field_26665));
      step(context, world, "first-person", null, client -> client.field_1690.method_31043(class_5498.field_26664));
      step(context, world, "underwater", "tp @p 0 -60.5 10 0 0", null);
      step(context, world, "hidden-hud", "tp @p 0 -58 0 0 -10", client -> client.field_1690.field_1842 = true);
      step(context, world, "hud-back", null, client -> client.field_1690.field_1842 = false);
      lightOverlay(context, world);
      step(context, world, "inspect", "item replace entity @p weapon.mainhand with diamond_sword", null);
      context.getInput().pressKey(70);
      context.waitTicks(15);
      context.takeScreenshot("sweep-inspect-running");
      command(world, "summon arrow 0 -55 2 {Motion:[0.0,0.4,0.6]}");
      context.waitTicks(10);
      context.takeScreenshot("sweep-arrow-trail");

      command(world, "gamemode survival @p");
      command(world, "item replace entity @p weapon.offhand with totem_of_undying");
      context.waitTicks(5);
      command(world, "damage @p 1000 minecraft:magic");
      context.waitTicks(5);
      context.takeScreenshot("sweep-totem-pop-start");
      context.waitTicks(12);
      context.takeScreenshot("sweep-totem-pop-mid");
      context.runOnClient(client -> client.field_1690.method_31043(class_5498.field_26665));
      context.waitTicks(8);
      context.takeScreenshot("sweep-totem-spirit-third-person");
      context.runOnClient(client -> client.field_1690.method_31043(class_5498.field_26664));
      context.waitTicks(25);
      context.takeScreenshot("sweep-totem-pop-after");
      context.waitTicks(40);
      command(world, "kill @p");
      context.waitTicks(30);
      context.takeScreenshot("sweep-death-screen");
      context.runOnClient(client -> {
         client.field_1724.method_7331();
         client.method_1507(null);
      });
      context.waitTicks(40);
      command(world, "gamemode creative @p");
      context.takeScreenshot("sweep-after-respawn");

      command(world, "gamemode spectator @p");
      context.waitTicks(20);
      context.takeScreenshot("sweep-spectator");
      command(world, "gamemode creative @p");

      command(world, "execute in minecraft:the_nether run tp @p 0 100 0");
      context.waitTicks(60);
      context.takeScreenshot("sweep-nether");
      command(world, "execute in minecraft:the_end run tp @p 0 70 0");
      context.waitTicks(60);
      context.takeScreenshot("sweep-end");
      command(world, "execute in minecraft:overworld run tp @p 0 -58 0 0 -10");
      context.waitTicks(60);
      world.getClientWorld().waitForChunksRender();
      context.takeScreenshot("sweep-back-overworld");

      context.getInput().resizeWindow(640, 360);
      context.waitTicks(10);
      context.takeScreenshot("sweep-small-window");
      context.getInput().resizeWindow(854, 480);
      context.waitTicks(10);
      context.runOnClient(client -> client.method_1507(new net.minecraft.class_433(true)));
      context.waitTicks(10);
      context.runOnClient(client -> client.method_1507(null));
      System.out.println("[AtlasSweep] Situations done");
   }

   /** A roofed room with a torch: red where mobs always spawn, orange at the open edge, the safe colour by the torch. */
   private static void lightOverlay(ClientGameTestContext context, TestSingleplayerContext world) {
      command(world, "fill -12 -54 -24 12 -54 -4 stone");
      command(world, "setblock 0 -60 -14 torch");
      command(world, "tp @p 0 -57 -4 180 40");
      context.runOnClient(client -> ModuleRegistry.all().stream().filter(module -> module.id().equals("light-overlay")).forEach(module -> module.setEnabled(true)));
      for (String mode : List.of("Theme", "Classic")) {
         context.runOnClient(client -> ModuleRegistry.all().stream()
            .filter(module -> module.id().equals("light-overlay"))
            .flatMap(module -> module.settings().stream())
            .filter(setting -> setting instanceof ModeSetting && setting.id().equals("color-mode"))
            .forEach(setting -> ((ModeSetting)setting).set(mode)));
         context.waitTicks(20);
         world.getClientWorld().waitForChunksRender();
         context.takeScreenshot("sweep-light-overlay-" + mode.toLowerCase(java.util.Locale.ROOT));
      }

      command(world, "fill -12 -54 -24 12 -54 -4 air");
      command(world, "setblock 0 -60 -14 air");
      command(world, "tp @p 0 -58 0 0 -10");
   }

   private static void step(ClientGameTestContext context, TestSingleplayerContext world, String name, String command, java.util.function.Consumer<class_310> action) {
      if (command != null) {
         command(world, command);
      }

      if (action != null) {
         context.runOnClient(action::accept);
      }

      context.waitTicks(20);
      context.takeScreenshot("sweep-" + name);
   }

   /** Every mode, on and off, and both ends of every slider, each on screen for a moment. */
   private static void everySetting(ClientGameTestContext context, List<ClientModule> effects) {
      int tried = 0;
      for (ClientModule module : effects) {
         for (Setting setting : module.settings()) {
            if (setting instanceof ModeSetting mode) {
               for (String option : mode.modes()) {
                  context.runOnClient(client -> mode.set(option));
                  context.waitTicks(2);
                  tried++;
               }
            } else if (setting instanceof BooleanSetting toggle) {
               context.runOnClient(client -> toggle.set(!toggle.get()));
               context.waitTicks(2);
               context.runOnClient(client -> toggle.set(!toggle.get()));
               tried += 2;
            } else if (setting instanceof NumberSetting number) {
               context.runOnClient(client -> number.set(number.min()));
               context.waitTicks(2);
               context.runOnClient(client -> number.set(number.max()));
               context.waitTicks(2);
               tried += 2;
            } else if (setting instanceof ActionSetting action) {
               context.runOnClient(client -> action.run());
               tried++;
            }
         }

         context.runOnClient(client -> {
            module.resetSettings();
            ColorThemeModule.get().applyColors(module);
         });
         context.waitTicks(2);
         System.out.println("[AtlasSweep] Settings OK: " + module.id());
      }

      System.out.println("[AtlasSweep] Tried " + tried + " setting values");
   }

   /** Opens every page and checks that no label runs into its widgets. */
   private static void everyPage(ClientGameTestContext context) {
      context.runOnClient(client -> {
         VisualsConfigScreen menu = new VisualsConfigScreen(null);
         client.method_1507(menu);
      });
      context.waitTicks(3);
      context.takeScreenshot("sweep-menu-top");
      typeSearch(context, "aura");
      context.takeScreenshot("sweep-search-aura");
      typeSearch(context, "dust");
      context.takeScreenshot("sweep-search-setting");
      typeSearch(context, "zzzz");
      context.takeScreenshot("sweep-search-nothing");

      int page = 0;
      for (ClientModule module : ModuleRegistry.all()) {
         int number = page++;
         context.runOnClient(client -> {
            class_437 screen = ModuleScreens.settingsOf(module);
            client.method_1507(screen);
            int room = client.field_1755.field_22789 / 2 + 10 - Math.max(8, client.field_1755.field_22789 / 2 - 190);
            for (Setting setting : module.settings()) {
               if (setting.isVisible() && client.field_1772.method_1727(setting.name()) > room) {
                  System.out.println("[AtlasSweep] LABEL TOO LONG " + module.id() + ": " + setting.name());
               }
            }
         });
         context.waitTicks(2);
         context.takeScreenshot(String.format("sweep-page-%02d-%s", number, module.id()));
      }

      context.runOnClient(client -> client.method_1507(null));
   }

   /** A damaged settings file must not stop the game from starting. */
   private static void brokenConfig(ClientGameTestContext context) {
      context.runOnClient(client -> {
         Path file = FabricLoader.getInstance().getConfigDir().resolve("atlas-visuals.json");
         VisualsConfig.save();
         try {
            String good = Files.readString(file);
            Files.writeString(file, "{\"hand-aura\":{\"enabled\":\"banana\",\"settings\":{\"glow-color\":{\"x\":1},\"color\":[1,2]}},\"water-color\":5,\"particles\":{\"enabled\":{}}}");
            VisualsConfig.load(Set.of());
            Files.writeString(file, "this is not json {");
            VisualsConfig.load(Set.of());
            Files.writeString(file, good);
            VisualsConfig.load(Set.of());
         } catch (IOException failure) {
            throw new AssertionError(failure);
         }

         System.out.println("[AtlasSweep] Broken settings file survived");
      });
   }

   private static void typeSearch(ClientGameTestContext context, String text) {
      context.runOnClient(client -> {
         for (net.minecraft.class_364 child : client.field_1755.method_25396()) {
            if (child instanceof net.minecraft.class_342 field) {
               field.method_1852(text);
               break;
            }
         }
      });
      context.waitTicks(3);
   }

   private static void command(TestSingleplayerContext world, String command) {
      world.getServer().runCommand(command);
   }

   private record Cost(float millisPerFrame) {
   }
}
