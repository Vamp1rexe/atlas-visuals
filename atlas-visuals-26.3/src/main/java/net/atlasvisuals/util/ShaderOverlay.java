package net.atlasvisuals.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;



@Environment(EnvType.CLIENT)
public final class ShaderOverlay {
   private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();

   private ShaderOverlay() {
   }

   private static MethodHandle irisProbe() {
      if (!FabricLoader.getInstance().isModLoaded("iris")) {
         return null;
      } else {
         try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var2) {
            return null;
         }
      }
   }

   public static boolean shaderPackInUse() {
      if (SHADER_PACK_IN_USE == null) {
         return false;
      } else {
         try {
            return (boolean)SHADER_PACK_IN_USE.invokeExact();
         } catch (Throwable var1) {
            return false;
         }
      }
   }

}
