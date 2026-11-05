package net.atlasvisuals.mixin;

import net.atlasvisuals.event.impl.PlayerAttackEntityEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Player.class})
public abstract class MixinPlayer {
   @Inject(method = {"attack"}, at = {@At("HEAD")})
   private void atlas$onAttack(Entity target, CallbackInfo ci) {
      if ((Object)this == Minecraft.getInstance().player) {
         PlayerAttackEntityEvent.dispatch(target);
      }
   }
}
