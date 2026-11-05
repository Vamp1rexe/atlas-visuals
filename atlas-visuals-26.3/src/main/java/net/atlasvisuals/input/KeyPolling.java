package net.atlasvisuals.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.sdl.SDLMouse;

@Environment(EnvType.CLIENT)
public final class KeyPolling {
   private KeyPolling() {
   }

   public static boolean isKeyDown(Window window, int key) {
      return InputConstants.isKeyDown(key);
   }

   /** Buttons count from 1, as the game's mouse events do. */
   public static boolean isMouseDown(int button) {
      return button >= 1 && (SDLMouse.SDL_GetMouseState((FloatBuffer)null, (FloatBuffer)null) & 1 << button - 1) != 0;
   }
}