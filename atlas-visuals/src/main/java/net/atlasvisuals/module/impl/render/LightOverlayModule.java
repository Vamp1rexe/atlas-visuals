package net.atlasvisuals.module.impl.render;

import java.util.Arrays;
import net.atlasvisuals.chat.ModuleChat;
import net.atlasvisuals.event.EventPriority;
import net.atlasvisuals.event.Stage;
import net.atlasvisuals.event.impl.KeyInputEvent;
import net.atlasvisuals.event.impl.Render3DEvent;
import net.atlasvisuals.event.impl.TickEvent;
import net.atlasvisuals.gui.ColorTheme;
import net.atlasvisuals.input.ModuleKeybinds;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleCategory;
import net.atlasvisuals.module.setting.BooleanSetting;
import net.atlasvisuals.module.setting.ColorSetting;
import net.atlasvisuals.module.setting.KeySetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.NumberSetting;
import net.atlasvisuals.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10322;
import net.minecraft.class_1297;
import net.minecraft.class_1944;
import net.minecraft.class_2231;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2269;
import net.minecraft.class_2312;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2401;
import net.minecraft.class_2413;
import net.minecraft.class_2457;
import net.minecraft.class_2488;
import net.minecraft.class_2577;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3481;
import net.minecraft.class_638;

/**
 * Light level overlay in the style of Lighty's carpet mode (Apache-2.0, SchmarrnDevs): a thin coloured
 * carpet on every block a hostile mob could spawn on. Red spawns at any time, orange only at night,
 * the safe colour has enough block light. The toggle key (F8) switches it on and off anywhere in game.
 */
@Environment(EnvType.CLIENT)
public final class LightOverlayModule extends ClientModule {
   private static final int MAX_SPOTS = 10000;
   private static final float CARPET = 0.0625F;
   /** Lighty's own colours. */
   private static final int CLASSIC_DANGER = 0xFFFF0000;
   private static final int CLASSIC_WARNING = 0xFFFF6600;
   private static final int CLASSIC_SAFE = 0xFF00FF00;
   private static final byte DANGER = 0;
   private static final byte WARNING = 1;
   private static final byte SAFE = 2;
   private static final class_2350[] SIDES = {class_2350.field_11035, class_2350.field_11039, class_2350.field_11043, class_2350.field_11034};

   private final KeySetting key = this.addSetting("toggle-key", new KeySetting("Toggle Key", 297));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Colors", "Theme", "Theme", "Classic", "Custom"));
   private final ColorSetting dangerColor = this.addSetting("danger-color", new ColorSetting("Spawns Always", CLASSIC_DANGER));
   private final ColorSetting warningColor = this.addSetting("warning-color", new ColorSetting("Spawns At Night", CLASSIC_WARNING));
   private final ColorSetting safeColor = this.addSetting("safe-color", new ColorSetting("Safe", CLASSIC_SAFE));
   private final NumberSetting opacity = this.addSetting("opacity", new NumberSetting("Opacity", 0.45F, 0.05F, 1.0F).step(0.05F));
   private final BooleanSetting showSafe = this.addSetting("show-safe", new BooleanSetting("Show Safe Spots", true));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 24.0F, 8.0F, 64.0F).step(1.0F));
   private final NumberSetting height = this.addSetting("height", new NumberSetting("Height", 12.0F, 4.0F, 32.0F).step(1.0F));
   private final NumberSetting skyThreshold = this.addSetting("sky-threshold", new NumberSetting("Sky Light Threshold", 7.0F, 0.0F, 15.0F).step(1.0F));
   private final NumberSetting blockThreshold = this.addSetting("block-threshold", new NumberSetting("Block Light Threshold", 0.0F, 0.0F, 15.0F).step(1.0F));

   private final class_2338.class_2339 ground = new class_2338.class_2339();
   private final class_2338.class_2339 above = new class_2338.class_2339();
   private final class_2338.class_2339 side = new class_2338.class_2339();
   /** The finished scan that is drawn, and the one being filled a few rows per tick. */
   private Spots shown = new Spots();
   private Spots building = new Spots();
   private int scanX;
   private int centerX;
   private int centerY;
   private int centerZ;
   private int scanRange;
   private int scanHeight;
   private boolean scanning;

   public LightOverlayModule() {
      super("light-overlay", "Light Overlay", "Shows where mobs can spawn, like Lighty's carpet mode. Toggle it with F8.", ModuleCategory.VISUALS);
      this.dangerColor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.warningColor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.safeColor.visibleWhen(() -> this.colorMode.is("Custom") && this.showSafe.get());
      // The key works while the overlay is off, so it listens for the whole session.
      KeyInputEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, this::onKey);
   }

   @Override
   protected void onEnable() {
      this.shown.clear();
      this.building.clear();
      this.scanning = false;
      this.on(TickEvent.TYPE, Stage.POST, event -> this.onTick());
      this.on(Render3DEvent.TYPE, event -> this.onRender());
   }

   @Override
   protected void onDisable() {
      this.shown.clear();
      this.building.clear();
      this.scanning = false;
   }

   private void onKey(KeyInputEvent event) {
      int bound = this.key.value();
      if (event.action == 1 && ModuleKeybinds.isBindable(bound) && event.key == bound) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1755 == null && mc.field_1724 != null) {
            this.toggle();
            ModuleChat.actionBarAlways(this, this.isEnabled() ? ModuleChat.body().good("On") : ModuleChat.body().danger("Off"));
            event.setCancelled(true);
         }
      }
   }

   private void onTick() {
      class_310 mc = class_310.method_1551();
      class_638 level = mc.field_1687;
      class_1297 camera = mc.method_1560();
      if (level == null || camera == null) {
         this.shown.clear();
         this.scanning = false;
         return;
      }

      if (!this.scanning) {
         class_2338 at = camera.method_24515();
         this.centerX = at.method_10263();
         this.centerY = at.method_10264();
         this.centerZ = at.method_10260();
         this.scanRange = this.range.getInt();
         this.scanHeight = this.height.getInt();
         this.scanX = this.centerX - this.scanRange;
         this.building.clear();
         this.scanning = true;
      }

      // A full scan takes about five ticks, so the overlay follows new torches within a quarter second.
      int rows = Math.max(1, (this.scanRange * 2 + 1 + 4) / 5);
      int last = this.centerX + this.scanRange;
      for (int i = 0; i < rows && this.scanX <= last; i++, this.scanX++) {
         this.scanRow(level, this.scanX);
      }

      if (this.scanX > last) {
         Spots done = this.building;
         this.building = this.shown;
         this.shown = done;
         this.scanning = false;
      }
   }

   private void scanRow(class_638 level, int x) {
      int minY = Math.max(level.method_31607(), this.centerY - this.scanHeight);
      int maxY = Math.min(level.method_31600() - 1, this.centerY + this.scanHeight);
      int range2 = this.scanRange * this.scanRange;
      int dx = x - this.centerX;
      boolean safeToo = this.showSafe.get();
      int skyLimit = this.skyThreshold.getInt();
      int blockLimit = this.blockThreshold.getInt();
      for (int z = this.centerZ - this.scanRange; z <= this.centerZ + this.scanRange; z++) {
         int dz = z - this.centerZ;
         if (dx * dx + dz * dz > range2) {
            continue;
         }

         for (int y = minY; y <= maxY; y++) {
            if (this.building.size >= MAX_SPOTS) {
               return;
            }

            this.ground.method_10103(x, y, z);
            this.above.method_10103(x, y + 1, z);
            class_2680 floor = level.method_8320(this.ground);
            class_2680 up = level.method_8320(this.above);
            if (blocked(level, floor, up, this.ground, this.above)) {
               continue;
            }

            float offset = offset(up);
            if (offset < 0.0F) {
               continue;
            }

            int blockLight = level.method_8314(class_1944.field_9282, this.above);
            byte kind;
            if (blockLight > blockLimit) {
               if (!safeToo) {
                  continue;
               }
               kind = SAFE;
            } else {
               kind = level.method_8314(class_1944.field_9284, this.above) > skyLimit ? WARNING : DANGER;
            }

            // Lighty draws the carpet's edge only where the neighbouring floor does not hide it.
            class_2338 edgeOf = offset > 0.001F ? this.above : this.ground;
            int sides = 0;
            for (int s = 0; s < SIDES.length; s++) {
               this.side.method_25505(edgeOf, SIDES[s]);
               if (class_2248.method_9607(class_2246.field_10340.method_9564(), level.method_8320(this.side), SIDES[s])) {
                  sides |= 1 << s;
               }
            }

            this.building.add(x, y, z, offset, kind, sides);
         }
      }
   }

   private void onRender() {
      Spots spots = this.shown;
      if (spots.size == 0) {
         return;
      }

      int alpha = Math.round(this.opacity.get() * 255.0F) << 24;
      int[] palette = {this.color(DANGER) & 0xFFFFFF | alpha, this.color(WARNING) & 0xFFFFFF | alpha, this.color(SAFE) & 0xFFFFFF | alpha};
      WorldQuads quads = WorldQuads.layer(false);
      for (int i = 0; i < spots.size; i++) {
         int c = palette[spots.kind[i]];
         double x = spots.x[i];
         double z = spots.z[i];
         double y = spots.y[i] + 1 + spots.offset[i];
         double top = y + CARPET;
         quads.quad(x, top, z, x, top, z + 1, x + 1, top, z + 1, x + 1, top, z, c);
         int sides = spots.sides[i];
         if ((sides & 1) != 0) {
            quads.quad(x, top, z + 1, x, y, z + 1, x + 1, y, z + 1, x + 1, top, z + 1, c);
         }
         if ((sides & 2) != 0) {
            quads.quad(x, top, z, x, y, z, x, y, z + 1, x, top, z + 1, c);
         }
         if ((sides & 4) != 0) {
            quads.quad(x + 1, top, z, x + 1, y, z, x, y, z, x, top, z, c);
         }
         if ((sides & 8) != 0) {
            quads.quad(x + 1, top, z + 1, x + 1, y, z + 1, x + 1, y, z, x + 1, top, z, c);
         }
      }
   }

   /** The colour for one kind of spot: from the Colors tab, Lighty's red/orange/green, or picked by hand. */
   private int color(byte kind) {
      if (this.colorMode.is("Classic")) {
         return kind == DANGER ? CLASSIC_DANGER : kind == WARNING ? CLASSIC_WARNING : CLASSIC_SAFE;
      } else if (this.colorMode.is("Custom")) {
         return (kind == DANGER ? this.dangerColor : kind == WARNING ? this.warningColor : this.safeColor).argb();
      } else {
         int theme = ColorThemeModule.get().color().argb();
         return kind == DANGER ? theme : ColorTheme.shade(theme, kind == WARNING ? 0.5F : 1.7F);
      }
   }

   /** Lighty's checks for a block a mob cannot stand on or spawn in. */
   private static boolean blocked(class_638 level, class_2680 floor, class_2680 up, class_2338 pos, class_2338 posUp) {
      class_2248 upBlock = up.method_26204();
      return up.method_26234(level, posUp)
         || !floor.method_26206(level, pos, class_2350.field_11036)
         || upBlock instanceof class_2457
         || upBlock instanceof class_2269
         || upBlock instanceof class_2312
         || upBlock instanceof class_2231
         || upBlock instanceof class_2401
         || upBlock instanceof class_2577
         || upBlock instanceof class_10322
         || !validSpawn(level, floor, pos)
         || !up.method_26227().method_15769()
         || !upBlock.method_9538(up)
         || up.method_26164(class_3481.field_24459);
   }

   private static boolean validSpawn(class_638 level, class_2680 floor, class_2338 pos) {
      if (floor.method_26204() instanceof class_2413) {
         return true;
      }

      try {
         return floor.method_26170(level, pos, null);
      } catch (RuntimeException e) {
         // Some blocks ask about the mob type; without one the overlay cannot tell, so it shows the spot.
         return true;
      }
   }

   /** Snow one layer deep lifts the carpet; deeper snow blocks spawning there. */
   private static float offset(class_2680 up) {
      if (up.method_26204() instanceof class_2488) {
         int layers = up.method_11654(class_2488.field_11518);
         return layers != 1 ? -1.0F : 0.125F * layers;
      }

      return 0.0F;
   }

   @Environment(EnvType.CLIENT)
   private static final class Spots {
      int size;
      int[] x = new int[256];
      int[] y = new int[256];
      int[] z = new int[256];
      float[] offset = new float[256];
      byte[] kind = new byte[256];
      byte[] sides = new byte[256];

      void clear() {
         this.size = 0;
      }

      void add(int px, int py, int pz, float off, byte k, int s) {
         if (this.size == this.x.length) {
            int grown = this.size * 2;
            this.x = Arrays.copyOf(this.x, grown);
            this.y = Arrays.copyOf(this.y, grown);
            this.z = Arrays.copyOf(this.z, grown);
            this.offset = Arrays.copyOf(this.offset, grown);
            this.kind = Arrays.copyOf(this.kind, grown);
            this.sides = Arrays.copyOf(this.sides, grown);
         }

         this.x[this.size] = px;
         this.y[this.size] = py;
         this.z[this.size] = pz;
         this.offset[this.size] = off;
         this.kind[this.size] = k;
         this.sides[this.size] = (byte)s;
         this.size++;
      }
   }
}
