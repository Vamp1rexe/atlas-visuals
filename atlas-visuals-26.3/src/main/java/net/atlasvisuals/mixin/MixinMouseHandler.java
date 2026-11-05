package net.atlasvisuals.mixin;

import net.atlasvisuals.event.impl.MouseInputEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({MouseHandler.class})
public abstract class MixinMouseHandler {
   @Inject(method = {"onButton"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
      if (MouseInputEvent.dispatch(window, buttonInfo.button(), action, buttonInfo.modifiers())) {
         ci.cancel();
      }
   }
}
