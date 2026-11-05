package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.atlasvisuals.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinSwingAnimationLivingEntity {
   @ModifyExpressionValue(
      method = {"swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getModifiedSwingDuration(Lnet/minecraft/world/item/component/SwingAnimation;)I"
      )}
   )
   private int atlas$swingTempo(int ticks) {
      SwingAnimationModule module = SwingAnimationModule.active;
      return module != null && (Object)this == Minecraft.getInstance().player ? module.swingTicks(ticks) : ticks;
   }

   @ModifyExpressionValue(
      method = {"swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity$SwingState;startIfAble(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;I)Z"
      )}
   )
   private boolean atlas$latchSwing(boolean started) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (started && module != null && (Object)this == Minecraft.getInstance().player) {
         module.latch();
      }
      return started;
   }
}