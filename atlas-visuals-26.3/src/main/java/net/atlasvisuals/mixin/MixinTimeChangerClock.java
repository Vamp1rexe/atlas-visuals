package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.atlasvisuals.module.impl.render.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** What the sky is told, while the real clock keeps counting underneath. */
@Environment(EnvType.CLIENT)
@Mixin({ClientClockManager.ClientClockInstance.class})
public abstract class MixinTimeChangerClock {
   @ModifyReturnValue(method = {"totalTicks"}, at = {@At("RETURN")})
   private long atlas$forcedTicks(long ticks) {
      TimeChangerModule module = TimeChangerModule.active;
      return module != null ? module.forcedTicks() : ticks;
   }

   @ModifyReturnValue(method = {"partialTick"}, at = {@At("RETURN")})
   private float atlas$forcedPartial(float partial) {
      return TimeChangerModule.active != null ? 0.0F : partial;
   }
}