package net.atlasvisuals.mixin;

import net.atlasvisuals.event.impl.MouseInputEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11910;
import net.minecraft.class_312;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_312.class})
public abstract class MixinMouseHandler {
   @Inject(method = {"method_1601"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$onMouseButton(long window, class_11910 buttonInfo, int action, CallbackInfo ci) {
      if (MouseInputEvent.dispatch(window, buttonInfo.comp_4801(), action, buttonInfo.comp_4797())) {
         ci.cancel();
      }
   }
}
