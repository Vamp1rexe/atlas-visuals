package net.atlasvisuals.module.impl.render;

import net.atlasvisuals.event.Stage;
import net.atlasvisuals.event.impl.TickEvent;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleCategory;
import net.atlasvisuals.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Environment(EnvType.CLIENT)
public final class TimeChangerModule extends ClientModule {
   public static volatile TimeChangerModule active;
   private final NumberSetting time = this.addSetting("time", new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F).step(1.0F));
   private long shown = Long.MIN_VALUE;

   public TimeChangerModule() {
      super("time-changer", "Time Changer", "Makes you able to set a custom time.", ModuleCategory.RENDER);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
      this.shown = Long.MIN_VALUE;
      refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      refresh();
   }

   private void onPostTick(TickEvent event) {
      long forced = this.forcedTicks();
      if (forced != this.shown) {
         this.shown = forced;
         refresh();
      }
   }

   private static void refresh() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         level.environmentAttributes().invalidateTickCache();
      }
   }

   public long forcedTicks() {
      return (long)this.time.get();
   }
}