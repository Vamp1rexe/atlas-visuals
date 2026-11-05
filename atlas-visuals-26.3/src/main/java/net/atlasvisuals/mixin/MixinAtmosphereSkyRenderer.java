package net.atlasvisuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.atlasvisuals.module.impl.render.AtmosphereModule;
import net.atlasvisuals.module.impl.render.SkyLyrics;
import net.atlasvisuals.module.impl.render.SkyPass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRenderer.class})
public abstract class MixinAtmosphereSkyRenderer {
   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"
      )}
   )
   private RenderPass atlas$skyPass(
      CommandEncoder encoder, Supplier<String> label, GpuTextureView color, Optional<Vector4fc> clear, GpuTextureView depth, OptionalDouble clearDepth, Operation<RenderPass> original
   ) {
      RenderPass first = original.call(encoder, label, color, clear, depth, clearDepth);
      return new SkyPass(first, () -> encoder.createRenderPass(label, color, Optional.empty(), depth, OptionalDouble.empty()));
   }

   @Inject(method = {"extractRenderState"}, at = {@At("TAIL")})
   private void atlas$readSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && state.skybox == Skybox.OVERWORLD) {
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(method = {"renderSkyDisc"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$dome(RenderPass pass, Vector3fc color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && SkyPass.aside(pass, atmosphere::skyDisc)) {
         ci.cancel();
      }
   }

   @Inject(method = {"renderSkyDisc"}, at = {@At("TAIL")})
   private void atlas$afterDome(RenderPass pass, Vector3fc color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         SkyPass.aside(pass, () -> {
            atmosphere.afterSkyDisc();
            return true;
         });
      }
   }

   @Inject(method = {"renderSunriseAndSunset"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$noSunriseFan(RenderPass pass, PoseStack pose, float sunAngle, Vector4fc color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.replacesSunrise()) {
         ci.cancel();
      }
   }

   @Inject(method = {"renderStars"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$stars(RenderPass pass, float starBrightness, PoseStack pose, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && SkyPass.aside(pass, () -> atmosphere.stars(pose))) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"
      )}
   )
   private void atlas$beforeMoon(
      RenderPass pass, PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         SkyPass.aside(pass, () -> {
            atmosphere.beforeMoon(pose);
            return true;
         });
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V",
         shift = Shift.AFTER
      )}
   )
   private void atlas$afterMoon(
      RenderPass pass, PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         SkyPass.aside(pass, () -> {
            atmosphere.afterMoon(pose);
            return true;
         });
      }
   }

   @Inject(method = {"renderSunMoonAndStars"}, at = {@At("TAIL")})
   private void atlas$afterCelestials(
      RenderPass pass, PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      SkyPass.aside(pass, () -> {
         AtmosphereModule atmosphere = AtmosphereModule.get();
         if (atmosphere != null) {
            atmosphere.afterCelestials();
         }
         SkyLyrics.render();
         return true;
      });
   }

   @Inject(method = {"renderEndSky"}, at = {@At("HEAD")}, cancellable = true)
   private void atlas$endSky(RenderPass pass, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && SkyPass.aside(pass, atmosphere::endSky)) {
         ci.cancel();
      }
   }
}