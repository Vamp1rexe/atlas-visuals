package net.atlasvisuals.mixin;

import io.netty.channel.ChannelHandlerContext;
import java.util.function.BooleanSupplier;
import net.atlasvisuals.event.impl.PacketReceiveEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2535;
import net.minecraft.class_2596;
import net.minecraft.class_2598;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only reads incoming packets (totem pop effects). It never cancels or sends anything. */
@Environment(EnvType.CLIENT)
@Mixin({class_2535.class})
public abstract class MixinClientConnection {
   @Unique
   private final BooleanSupplier atlas$connectionActive = this::method_10758;

   @Shadow
   public abstract boolean method_10758();

   @Shadow
   public abstract class_2598 method_36121();

   @Inject(method = {"method_10770"}, at = {@At("HEAD")})
   private void atlas$onPacketReceive(ChannelHandlerContext ctx, class_2596<?> packet, CallbackInfo ci) {
      if (this.method_36121() == class_2598.field_11942) {
         PacketReceiveEvent.dispatchAsync(packet, this.atlas$connectionActive);
      }
   }
}
