package net.atlasvisuals.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** The fixed list of visual effects this mod ships. */
@Environment(EnvType.CLIENT)
public final class ModuleRegistry {
   private static final List<ClientModule> MODULES = new ArrayList<>();
   private static final List<ClientModule> VIEW = Collections.unmodifiableList(MODULES);

   private ModuleRegistry() {
   }

   public static void register(ClientModule module) {
      MODULES.add(module);
      module.registered = true;
   }

   public static List<ClientModule> all() {
      return VIEW;
   }

   public static <T extends ClientModule> T get(Class<T> type) {
      for (int i = 0; i < MODULES.size(); i++) {
         ClientModule module = MODULES.get(i);
         if (module.getClass() == type) {
            return type.cast(module);
         }
      }

      return null;
   }

   static void switched(ClientModule module) {
   }
}
