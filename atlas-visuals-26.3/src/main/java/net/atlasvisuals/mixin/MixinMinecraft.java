package net.atlasvisuals.mixin;

import net.atlasvisuals.event.EventBus;
import net.atlasvisuals.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
   @Inject(method = {"tick"}, at = {@At("HEAD")})
   private void atlas$onTickStart(CallbackInfo ci) {
      EventBus.INSTANCE.drainAsync();
      TickEvent.dispatch();
   }

   @Inject(method = {"tick"}, at = {@At("RETURN")})
   private void atlas$onTickEnd(CallbackInfo ci) {
      TickEvent.dispatchPost();
   }
}
