package net.atlasvisuals.mixin;

import io.netty.channel.ChannelHandlerContext;
import java.util.function.BooleanSupplier;
import net.atlasvisuals.event.impl.PacketReceiveEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only reads incoming packets (totem pop effects). It never cancels or sends anything. */
@Environment(EnvType.CLIENT)
@Mixin({Connection.class})
public abstract class MixinClientConnection {
   @Unique
   private final BooleanSupplier atlas$connectionActive = this::isConnected;

   @Shadow
   public abstract boolean isConnected();

   @Shadow
   public abstract PacketFlow getReceiving();

   @Inject(method = {"channelRead0"}, at = {@At("HEAD")})
   private void atlas$onPacketReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
      if (this.getReceiving() == PacketFlow.CLIENTBOUND) {
         PacketReceiveEvent.dispatchAsync(packet, this.atlas$connectionActive);
      }
   }
}
