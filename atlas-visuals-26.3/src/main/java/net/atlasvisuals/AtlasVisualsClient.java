package net.atlasvisuals;

import com.mojang.brigadier.Command;
import java.util.List;
import java.util.Set;
import net.atlasvisuals.config.VisualsConfig;
import net.atlasvisuals.event.EventBus;
import net.atlasvisuals.gui.VisualsConfigScreen;
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
import net.atlasvisuals.module.impl.render.LightOverlayModule;
import net.atlasvisuals.module.impl.render.MotionBlurModule;
import net.atlasvisuals.module.impl.render.RelicParticlesModule;
import net.atlasvisuals.module.impl.render.SwingAnimationModule;
import net.atlasvisuals.module.impl.render.TimeChangerModule;
import net.atlasvisuals.module.impl.render.TotemSpiritModule;
import net.atlasvisuals.module.impl.render.WaterColorModule;
import net.atlasvisuals.module.impl.render.WeaponInspectModule;
import net.atlasvisuals.util.combat.CombatTarget;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AtlasVisualsClient implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   /** Effects that are on after a fresh install; everything else starts off and is switched on in the menu. */
   private static final Set<String> ON_BY_DEFAULT = Set.of("spotify-hud", "hand-aura", "hand-pose", "water-color");

   @Override
   public void onInitializeClient() {
      long start = System.nanoTime();
      List<ClientModule> modules = List.of(
         new ColorThemeModule(),
         new SpotifyHudModule(),
         new AtmosphereModule(),
         new WaterColorModule(),
         new HandAuraModule(),
         new HandsModule(),
         new HandModelModule(),
         new SwingAnimationModule(),
         new WeaponInspectModule(),
         new ColorGradingModule(),
         new MotionBlurModule(),
         new CustomGlintModule(),
         new CustomFovModule(),
         new TimeChangerModule(),
         new JumpCirclesModule(),
         new LightOverlayModule(),
         new HitParticlesModule(),
         new FloatingRelicsModule(),
         new RelicParticlesModule(),
         new TotemSpiritModule()
      );
      modules.forEach(ModuleRegistry::register);
      modules.forEach(ClientModule::captureDefaults);
      CombatTarget.install();

      LOGGER.info("Atlas Visuals set up in {} ms", (System.nanoTime() - start) / 1000000L);
      ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
         long loading = System.nanoTime();
         if (VisualsConfig.load(ON_BY_DEFAULT)) {
            ModuleRegistry.all().forEach(ColorThemeModule.get()::applyColors);
         }

         LOGGER.info("Atlas Visuals settings loaded in {} ms", (System.nanoTime() - loading) / 1000000L);
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VisualsConfig.save());

      // Fallback for launchers or setups without Mod Menu.
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
         ClientCommands.literal("atlasvisuals").executes(context -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.gui.setScreen(new VisualsConfigScreen(client.gui.screen())));
            return Command.SINGLE_SUCCESS;
         })
      ));
   }
}
