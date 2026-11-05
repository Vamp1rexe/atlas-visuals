package net.atlasvisuals.module.impl.render;

import net.atlasvisuals.gui.ColorTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CosmeticAccent {
   private CosmeticAccent() {
   }

   static int argb() {
      return ColorTheme.argb();
   }
}
