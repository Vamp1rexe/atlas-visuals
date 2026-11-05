package net.atlasvisuals.mixin;

import net.atlasvisuals.event.impl.KeyInputEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({KeyboardHandler.class})
public abstract class MixinKeyboardHandler {
   @Inject(method = {"keyPress"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
      if (KeyInputEvent.dispatch(event.key(), event.keycode(), action, event.modifiers())) {
         ci.cancel();
      }
   }
}
