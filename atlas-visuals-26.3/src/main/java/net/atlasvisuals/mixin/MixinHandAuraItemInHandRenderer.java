package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.atlasvisuals.module.impl.render.HandAuraModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sends the hand's pass into the aura's own target, then lays the result over the picture. */
@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinHandAuraItemInHandRenderer {
   @Unique
   private boolean atlas$capturing;

   @WrapOperation(
      method = {"renderItemInHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"
      )}
   )
   private RenderPass atlas$handAura(
      CommandEncoder encoder, Supplier<String> label, GpuTextureView color, Optional<Vector4fc> clear, GpuTextureView depth, OptionalDouble clearDepth, Operation<RenderPass> original
   ) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         this.atlas$capturing = true;
         return original.call(encoder, label, HandAuraModule.captureColor(), clear, HandAuraModule.captureDepth(), clearDepth);
      }
      return original.call(encoder, label, color, clear, depth, clearDepth);
   }

   @Inject(method = {"renderItemInHand"}, at = {@At("RETURN")})
   private void atlas$handAuraDone(CallbackInfo ci) {
      if (this.atlas$capturing) {
         this.atlas$capturing = false;
         HandAuraModule.endCapture(true);
      }
   }
}