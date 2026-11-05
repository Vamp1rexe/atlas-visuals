package net.atlasvisuals.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleKeybinds {
   private ModuleKeybinds() {
   }

   public static boolean isBindable(int key) {
      return key >= 4 && key < 512 || key >= 1000 && key < 1016;
   }
}
