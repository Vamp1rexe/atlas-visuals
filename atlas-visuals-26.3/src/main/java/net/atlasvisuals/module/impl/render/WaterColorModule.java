package net.atlasvisuals.module.impl.render;

import java.util.Arrays;
import net.atlasvisuals.event.Stage;
import net.atlasvisuals.event.impl.TickEvent;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleCategory;
import net.atlasvisuals.module.impl.client.SpotifyHudModule;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.util.spotify.NowPlaying;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/** Recolours water. Works on its own, with or without Spotify. */
@Environment(EnvType.CLIENT)
public final class WaterColorModule extends ClientModule {
   private static final String ALWAYS = "Always";
   private static final String MUSIC = "While Music Plays";
   private final ModeSetting design = this.addSetting("design", new ModeSetting("Water Color", PinkWater.Design.DEFAULT.label(), designs()));
   private final ModeSetting when = this.addSetting("when", new ModeSetting("When", ALWAYS, ALWAYS, MUSIC));

   public WaterColorModule() {
      super("water-color", "Water Color", "Gives water a different colour.", ModuleCategory.VISUALS);
   }

   private static String[] designs() {
      String[] modes = PinkWater.modes();
      return Arrays.copyOfRange(modes, 1, modes.length);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      PinkWater.reset(Minecraft.getInstance());
   }

   private void onTick(TickEvent event) {
      PinkWater.tick(Minecraft.getInstance(), this.wanted(), PinkWater.Design.byLabel(this.design.get()));
   }

   private boolean wanted() {
      if (this.when.is(ALWAYS)) {
         return true;
      }

      SpotifyHudModule spotify = SpotifyHudModule.active();
      NowPlaying now = spotify != null ? spotify.nowPlaying() : null;
      return now != null && now.playing();
   }
}
