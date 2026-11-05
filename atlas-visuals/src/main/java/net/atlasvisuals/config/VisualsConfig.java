package net.atlasvisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.setting.ActionSetting;
import net.atlasvisuals.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores which effects are on and every setting in config/atlas-visuals.json. */
@Environment(EnvType.CLIENT)
public final class VisualsConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private VisualsConfig() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("atlas-visuals.json");
   }

   /** @return whether there were no saved settings to load, as on a fresh install */
   public static boolean load(Set<String> onByDefault) {
      JsonObject saved = read();
      for (ClientModule module : ModuleRegistry.all()) {
         JsonObject entry = saved != null && saved.get(module.id()) instanceof JsonObject object ? object : null;
         if (entry != null && entry.get("settings") instanceof JsonObject settings) {
            for (Setting setting : module.settings()) {
               JsonElement value = settings.get(setting.id());
               if (value != null) {
                  restore(setting, value);
               }
            }
         }

         enable(module, entry, onByDefault.contains(module.id()));
      }

      return saved == null;
   }

   /** A value that cannot be read keeps its default, and the game still starts. */
   private static void restore(Setting setting, JsonElement value) {
      try {
         setting.deserialize(value);
      } catch (RuntimeException failure) {
         LOGGER.warn("Could not read setting {}, using its default", setting.id(), failure);
      }
   }

   private static void enable(ClientModule module, JsonObject entry, boolean byDefault) {
      try {
         boolean enabled = byDefault;
         if (entry != null && entry.get("enabled") instanceof JsonPrimitive saved && saved.isBoolean()) {
            enabled = saved.getAsBoolean();
         }

         module.setEnabled(enabled);
      } catch (RuntimeException failure) {
         LOGGER.warn("Could not start {}", module.id(), failure);
      }
   }

   private static JsonObject read() {
      Path path = file();
      if (!Files.isRegularFile(path)) {
         return null;
      }

      try {
         return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (IOException | RuntimeException failure) {
         LOGGER.warn("Could not read {}, using defaults", path, failure);
         return null;
      }
   }

   public static void save() {
      JsonObject root = new JsonObject();
      for (ClientModule module : ModuleRegistry.all()) {
         JsonObject settings = new JsonObject();
         for (Setting setting : module.settings()) {
            if (!(setting instanceof ActionSetting)) {
               settings.add(setting.id(), setting.serialize());
            }
         }

         JsonObject entry = new JsonObject();
         entry.addProperty("enabled", module.isEnabled());
         entry.add("settings", settings);
         root.add(module.id(), entry);
      }

      Path path = file();
      Path temp = path.resolveSibling(path.getFileName() + ".tmp");
      try {
         Files.createDirectories(path.getParent());
         Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
         try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException failure) {
         LOGGER.warn("Could not save {}", path, failure);
      }
   }
}
