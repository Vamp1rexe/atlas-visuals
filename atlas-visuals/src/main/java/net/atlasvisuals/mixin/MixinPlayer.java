package net.atlasvisuals.mixin;

import net.atlasvisuals.event.impl.PlayerAttackEntityEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_1657.class})
public abstract class MixinPlayer {
   @Inject(method = {"method_7324"}, at = {@At("HEAD")})
   private void atlas$onAttack(class_1297 target, CallbackInfo ci) {
      if ((Object)this == class_310.method_1551().field_1724) {
         PlayerAttackEntityEvent.dispatch(target);
      }
   }
}
