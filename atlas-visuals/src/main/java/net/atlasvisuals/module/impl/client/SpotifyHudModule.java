package net.atlasvisuals.module.impl.client;

import com.sun.jna.Platform;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.atlasvisuals.chat.ModuleChat;
import net.atlasvisuals.event.Stage;
import net.atlasvisuals.event.impl.TickEvent;
import net.atlasvisuals.gui.ThemeTint;
import net.atlasvisuals.input.KeyPolling;
import net.atlasvisuals.input.ModuleKeybinds;
import net.atlasvisuals.module.ClientModule;
import net.atlasvisuals.module.ModuleCategory;
import net.atlasvisuals.module.ModuleRegistry;
import net.atlasvisuals.module.setting.BooleanSetting;
import net.atlasvisuals.module.setting.ColorSetting;
import net.atlasvisuals.module.setting.KeySetting;
import net.atlasvisuals.module.setting.ModeSetting;
import net.atlasvisuals.module.setting.NumberSetting;
import net.atlasvisuals.module.setting.Setting;
import net.atlasvisuals.util.spotify.AlbumArt;
import net.atlasvisuals.util.spotify.Lyrics;
import net.atlasvisuals.util.spotify.LyricsQuery;
import net.atlasvisuals.util.spotify.LyricsService;
import net.atlasvisuals.util.spotify.MediaSession;
import net.atlasvisuals.util.spotify.NowPlaying;
import net.atlasvisuals.util.spotify.PlayerSource;
import net.atlasvisuals.util.spotify.SpotifyWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class SpotifyHudModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("AtlasVisuals");
   private static final long POLL_INTERVAL_MS = 500L;
   private static final float TINT_STEP_IN = 0.016666668F;
   private static final float TINT_STEP_OUT = 0.033333335F;
   private static final long WINDOW_POLL_MS = 1000L;
   private static final long REPOLL_AFTER_KEY_MS = 250L;
   private static final long KEY_COOLDOWN_MS = 200L;
   private static final int ART_MAX_SIDE = 256;
   private static final int STABLE_POLLS = 2;
   private static final int SESSION_HICCUPS = 2;
   private static final long ERROR_LOG_INTERVAL_MS = 60000L;
   private static final String SKY_ALL = "All Songs";
   private static final String SKY_PEEP = "Only Lil Peep";
   private static final String SKY_OFF = "Off";
   private static final long SKY_HOLD_NANOS = 6000000000L;
   private boolean skyHeld;
   private long skyHeldSince;
   private final BooleanSetting showArtist = this.addSetting("show-artist", new BooleanSetting("Show Artist", true));
   private final BooleanSetting showAlbumArt = this.addSetting("show-album-art", new BooleanSetting("Show Album Art", false));
   private final BooleanSetting showProgress = this.addSetting("show-progress", new BooleanSetting("Show Progress", true));
   private final BooleanSetting scrollTitles = this.addSetting("scroll-titles", new BooleanSetting("Scroll Long Titles", true));
   private final BooleanSetting showLyrics = this.addSetting("show-lyrics", new BooleanSetting("Show Lyrics", true));
   private final ModeSetting lyricLines = this.addSetting("lyric-lines", new ModeSetting("Lyric Lines", "2", "1", "2", "3"));
   private final ModeSetting lyricMotion = this.addSetting("lyric-motion", new ModeSetting("Lyric Motion", "Wave", "Wave", "Glide", "Fade"));
   private final ModeSetting lyricHighlight = this.addSetting("lyric-highlight", new ModeSetting("Lyric Highlight", "Karaoke", "Karaoke", "Line", "Off"));
   private final BooleanSetting lyricsOnline = this.addSetting("lyrics-online", new BooleanSetting("Lyrics Online", true));
   private final BooleanSetting skyLyrics = this.addSetting("sky-lyrics", new BooleanSetting("Sky Lyrics", true));
   private final BooleanSetting skyTitle = this.addSetting("sky-title", new BooleanSetting("Sky Song Title", true));
   private final BooleanSetting skyEmojis = this.addSetting("sky-emojis", new BooleanSetting("Sky Emojis", true));
   private final ModeSetting skyEmojiStyle = this.addSetting(
      "sky-emoji-style", new ModeSetting("Sky Emoji Style", "Beside", "Beside", "Behind", "Replace Word")
   );
   private final BooleanSetting skyFollow = this.addSetting("sky-follow", new BooleanSetting("Sky Follows View", true));
   private final NumberSetting skyHeight = this.addSetting("sky-height", new NumberSetting("Sky Height", 28.0F, 10.0F, 70.0F).step(1.0F));
   private final NumberSetting skySize = this.addSetting("sky-size", new NumberSetting("Sky Text Size", 1.0F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyTitleSize = this.addSetting("sky-title-size", new NumberSetting("Sky Title Size", 1.0F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyEmojiSize = this.addSetting("sky-emoji-size", new NumberSetting("Sky Emoji Size", 1.0F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyOpacity = this.addSetting("sky-opacity", new NumberSetting("Sky Opacity", 0.9F, 0.1F, 1.0F).step(0.05F));
   private final ColorSetting skyTextColor = this.addSetting("sky-text-color", new ColorSetting("Sky Text Color", -24876));
   private final ColorSetting skyTitleColor = this.addSetting("sky-title-color", new ColorSetting("Sky Title Color", -34104));
   private final BooleanSetting skyGlowAtmosphere = this.addSetting("sky-glow-atmosphere", new BooleanSetting("Sky Glow From Atmosphere", false));
   private final ColorSetting skyGlowColor = this.addSetting("sky-glow-color", new ColorSetting("Sky Glow Color", -60011));
   private final NumberSetting skyGlow = this.addSetting("sky-glow", new NumberSetting("Sky Glow", 0.8F, 0.0F, 1.0F).step(0.05F));
   private final ModeSetting glowingSky = this.addSetting(
      "glowing-sky", new ModeSetting("Glowing Sky", SKY_ALL, SKY_ALL, SKY_PEEP, SKY_OFF)
   );
   private final NumberSetting glowStrength = this.addSetting("glow-strength", new NumberSetting("Glow Strength", 1.0F, 0.1F, 1.0F).step(0.05F));
   private final BooleanSetting worldTint = this.addSetting("world-tint", new BooleanSetting("Tint Fog While Glowing", true));
   private final NumberSetting cardWidth = this.addSetting("card-width", new NumberSetting("Card Width", 320.0F, 200.0F, 560.0F).step(1.0F));
   private final ColorSetting titleColor = this.addSetting("title-color", new ColorSetting("Title Color", -1447442));
   private final ColorSetting artistColor = this.addSetting("artist-color", new ColorSetting("Artist Color", -6052949));
   private final ColorSetting lyricsColor = this.addSetting("lyrics-color", new ColorSetting("Lyrics Color", -657929));
   private final ColorSetting upcomingLyricsColor = this.addSetting("upcoming-lyrics-color", new ColorSetting("Upcoming Lyrics Color", -6052949));
   private final ColorSetting highlightColor = this.addSetting("highlight-color", new ColorSetting("Highlight Color", -14756000));
   private final BooleanSetting showWhenIdle = this.addSetting("show-when-idle", new BooleanSetting("Show When Idle", false));
   private final BooleanSetting mediaKeys = this.addSetting("media-keys", new BooleanSetting("Media Keys", false));
   private final KeySetting playPauseKey = this.addSetting("play-pause-key", new KeySetting("Play / Pause", -1));
   private final KeySetting nextKey = this.addSetting("next-key", new KeySetting("Next Track", -1));
   private final KeySetting previousKey = this.addSetting("previous-key", new KeySetting("Previous Track", -1));
   private volatile NowPlaying nowPlaying = NowPlaying.WAITING;
   private volatile SpotifyHudModule.TrackLyrics lyrics;
   private volatile SpotifyHudModule.Poller poller;
   private final Object publishLock = new Object();
   private final KeySetting[] keys = new KeySetting[]{this.playPauseKey, this.nextKey, this.previousKey};
   private final SpotifyWindow.MediaKey[] actions = new SpotifyWindow.MediaKey[]{
      SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT, SpotifyWindow.MediaKey.PREVIOUS
   };
   private final boolean[] wasDown = new boolean[this.keys.length];
   private long lastKeyAt;

   public SpotifyHudModule() {
      super("spotify-hud", "Spotify Sky", "A glowing sky with the lyrics of what Spotify is playing, and optional media keys.", ModuleCategory.CLIENT);
      this.usesNotifications();
      this.artistColor.visibleWhen(this.showArtist::get);
      this.lyricsColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Line"));
      this.upcomingLyricsColor.visibleWhen(this.showLyrics::get);
      this.highlightColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Off"));
      this.lyricLines.visibleWhen(this.showLyrics::get);
      this.lyricMotion.visibleWhen(this.showLyrics::get);
      this.lyricHighlight.visibleWhen(this.showLyrics::get);
      this.lyricsOnline.visibleWhen(this.showLyrics::get);
      this.skyLyrics.visibleWhen(this.showLyrics::get);
      this.skyTitle.visibleWhen(this::skyLyrics);
      this.skyEmojis.visibleWhen(this::skyLyrics);
      this.skyFollow.visibleWhen(this::skyLyrics);
      this.skyHeight.visibleWhen(this::skyLyrics);
      this.skySize.visibleWhen(this::skyLyrics);
      this.skyTitleSize.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
      this.skyEmojiStyle.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
      this.skyEmojiSize.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
      this.skyOpacity.visibleWhen(this::skyLyrics);
      this.skyTextColor.visibleWhen(this::skyLyrics);
      this.skyTitleColor.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
      this.skyGlowAtmosphere.visibleWhen(this::skyLyrics);
      this.skyGlowColor.visibleWhen(() -> this.skyLyrics() && !this.skyGlowAtmosphere.get());
      this.skyGlow.visibleWhen(this::skyLyrics);
      this.lyricsOnline.onChange(() -> {
         SpotifyHudModule.Poller running = this.poller;
         if (running != null) {
            running.lyricsService.setOnline(this.lyricsOnline.get());
         }
      });
      this.glowStrength.visibleWhen(() -> !this.glowingSky.is(SKY_OFF));
      this.worldTint.visibleWhen(() -> !this.glowingSky.is(SKY_OFF));
      // These configured the old on-screen card, which this mod does not have.
      for (Setting unused : new Setting[]{
         this.showArtist, this.showAlbumArt, this.showProgress, this.scrollTitles, this.lyricLines, this.lyricMotion, this.lyricHighlight,
         this.cardWidth, this.titleColor, this.artistColor, this.lyricsColor, this.upcomingLyricsColor, this.highlightColor, this.showWhenIdle
      }) {
         unused.visibleWhen(() -> false);
      }

      this.playPauseKey.visibleWhen(this.mediaKeys::get);
      this.nextKey.visibleWhen(this.mediaKeys::get);
      this.previousKey.visibleWhen(this.mediaKeys::get);
   }

   @Override
   protected void onEnable() {
      Arrays.fill(this.wasDown, true);
      if (!PlayerSource.supported()) {
         synchronized (this.publishLock) {
            this.nowPlaying = NowPlaying.UNSUPPORTED;
            this.lyrics = null;
         }

         ModuleChat.send(this, ModuleChat.body().danger("Not supported").text(", only the Spotify app on Windows, macOS and Linux works"));
      } else {
         SpotifyHudModule.Poller started = new SpotifyHudModule.Poller();
         synchronized (this.publishLock) {
            this.poller = started;
            this.nowPlaying = NowPlaying.WAITING;
            this.lyrics = null;
         }

         started.start();
         this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      }
   }

   @Override
   protected void onDisable() {
      ThemeTint.pink(0.0F);
      SpotifyHudModule.Poller stopped;
      synchronized (this.publishLock) {
         stopped = this.poller;
         this.poller = null;
         this.nowPlaying = PlayerSource.supported() ? NowPlaying.WAITING : NowPlaying.UNSUPPORTED;
         this.lyrics = null;
      }

      if (stopped != null) {
         stopped.stop();
      }
   }

   private void publish(SpotifyHudModule.Poller from, NowPlaying state, SpotifyHudModule.TrackLyrics trackLyrics) {
      synchronized (this.publishLock) {
         if (from == this.poller) {
            this.nowPlaying = state;
            this.lyrics = trackLyrics;
         }
      }
   }

   public static SpotifyHudModule get() {
      return ModuleRegistry.get(SpotifyHudModule.class);
   }

   public static SpotifyHudModule active() {
      SpotifyHudModule module = get();
      return module != null && module.isEnabled() ? module : null;
   }

   public static void hudButton(SpotifyWindow.MediaKey key) {
      SpotifyHudModule module = active();
      if (module != null) {
         module.press(key);
      }
   }

   public static boolean shouldShowHud() {
      SpotifyHudModule module = active();
      if (module == null) {
         return false;
      } else {
         NowPlaying now = module.nowPlaying;
         return now.status() != NowPlaying.Status.UNSUPPORTED && now.status() != NowPlaying.Status.WAITING ? now.active() || module.showWhenIdle.get() : false;
      }
   }

   public NowPlaying nowPlaying() {
      return this.nowPlaying;
   }

   public Lyrics lyricsFor(NowPlaying now) {
      SpotifyHudModule.TrackLyrics current = this.lyrics;
      SpotifyHudModule.Poller running = this.poller;
      if (current != null && running != null && this.wantsLyrics(now)) {
         return current.title().equals(now.title()) && current.artist().equals(now.artist()) ? running.lyricsService.lookup(current.query()) : null;
      } else {
         return null;
      }
   }

   public boolean wantsLyrics(NowPlaying now) {
      return this.showLyrics.get() && now != null && now.lyricsEligible();
   }

   public boolean showArtist() {
      return this.showArtist.get();
   }

   public boolean showAlbumArt() {
      return this.showAlbumArt.get();
   }

   public boolean showProgress() {
      return this.showProgress.get();
   }

   public boolean scrollTitles() {
      return this.scrollTitles.get();
   }

   public boolean showLyrics() {
      return this.showLyrics.get();
   }

   public boolean skyLyrics() {
      return this.showLyrics.get() && this.skyLyrics.get();
   }

   public boolean skyTitle() {
      return this.skyTitle.get();
   }

   public boolean skyEmojis() {
      return this.skyEmojis.get();
   }

   public SpotifyHudModule.SkyEmojiStyle skyEmojiStyle() {
      String var1 = this.skyEmojiStyle.get();

      return switch (var1) {
         case "Behind" -> SpotifyHudModule.SkyEmojiStyle.BEHIND;
         case "Replace Word" -> SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD;
         default -> SpotifyHudModule.SkyEmojiStyle.BESIDE;
      };
   }

   public boolean skyFollow() {
      return this.skyFollow.get();
   }

   public float skyHeight() {
      return this.skyHeight.get();
   }

   public float skySize() {
      return this.skySize.get();
   }

   public float skyTitleSize() {
      return this.skyTitleSize.get();
   }

   public float skyEmojiSize() {
      return this.skyEmojiSize.get();
   }

   public float skyOpacity() {
      return this.skyOpacity.get();
   }

   public int skyTextColor() {
      return this.skyTextColor.argb();
   }

   public int skyTitleColor() {
      return this.skyTitleColor.argb();
   }

   public boolean skyGlowAtmosphere() {
      return this.skyGlowAtmosphere.get();
   }

   public int skyGlowColor() {
      return this.skyGlowColor.argb();
   }

   public boolean peepPlaying() {
      NowPlaying now = this.nowPlaying;
      long time = System.nanoTime();
      if (this.glowingSky.is(SKY_OFF)) {
         this.skyHeld = false;
         return false;
      } else if (now != null && now.playing()) {
         // Off in "Only Lil Peep" for anyone else, at once.
         this.skyHeld = this.glowingSky.is(SKY_ALL)
            || now.artist() != null && now.artist().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").contains("lilpeep");
         this.skyHeldSince = time;
         return this.skyHeld;
      } else {
         // Spotify reports "not playing" for a moment between songs: only let the sky go after a real stop.
         return this.skyHeld && time - this.skyHeldSince < SKY_HOLD_NANOS;
      }
   }

   public float glowStrength() {
      return this.glowStrength.get();
   }

   public float skyGlow() {
      return this.skyGlow.get();
   }

   public int lyricLines() {
      try {
         return Math.max(1, Math.min(3, Integer.parseInt(this.lyricLines.get())));
      } catch (NumberFormatException var2) {
         return 2;
      }
   }

   public SpotifyHudModule.LyricMotion lyricMotion() {
      String var1 = this.lyricMotion.get();

      return switch (var1) {
         case "Glide" -> SpotifyHudModule.LyricMotion.GLIDE;
         case "Fade" -> SpotifyHudModule.LyricMotion.FADE;
         default -> SpotifyHudModule.LyricMotion.WAVE;
      };
   }

   public SpotifyHudModule.LyricHighlight lyricHighlight() {
      String var1 = this.lyricHighlight.get();

      return switch (var1) {
         case "Line" -> SpotifyHudModule.LyricHighlight.LINE;
         case "Off" -> SpotifyHudModule.LyricHighlight.OFF;
         default -> SpotifyHudModule.LyricHighlight.KARAOKE;
      };
   }

   public boolean lyricsOnline() {
      return this.lyricsOnline.get();
   }

   public float cardWidth() {
      return this.cardWidth.get();
   }

   public int titleColor() {
      return this.titleColor.argb();
   }

   public int artistColor() {
      return this.artistColor.argb();
   }

   public int lyricsColor() {
      return this.lyricsColor.argb();
   }

   public int upcomingLyricsColor() {
      return this.upcomingLyricsColor.argb();
   }

   public int highlightColor() {
      return this.highlightColor.argb();
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      float tint = ThemeTint.pink();
      boolean peep = this.peepPlaying();
      ThemeTint.pink(peep && this.worldTint.get() ? tint + 0.016666668F : tint - 0.033333335F);
      boolean accept = this.mediaKeys.get() && mc.field_1755 == null && mc.method_1569() && mc.field_1724 != null;

      for (int i = 0; i < this.keys.length; i++) {
         boolean down = isDown(mc, this.keys[i].value());
         if (down && !this.wasDown[i] && accept) {
            this.press(this.actions[i]);
         }

         this.wasDown[i] = down;
      }
   }

   private void press(SpotifyWindow.MediaKey key) {
      long now = System.currentTimeMillis();
      SpotifyHudModule.Poller running = this.poller;
      if (running != null && now - this.lastKeyAt >= 200L) {
         this.lastKeyAt = now;
         if (running.press(key)) {
            if (key == SpotifyWindow.MediaKey.PLAY_PAUSE) {
               synchronized (this.publishLock) {
                  if (this.poller == running) {
                     this.nowPlaying = this.nowPlaying.toggled();
                  }
               }
            }
         }
      }
   }

   private static boolean isDown(class_310 mc, int key) {
      if (!ModuleKeybinds.isBindable(key)) {
         return false;
      } else {
         return key >= 1000 ? GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), key - 1000) == 1 : KeyPolling.isKeyDown(mc.method_22683(), key);
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricHighlight {
      KARAOKE,
      LINE,
      OFF;
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricMotion {
      WAVE,
      GLIDE,
      FADE;
   }

   @Environment(EnvType.CLIENT)
   private final class Poller {
      final ScheduledThreadPoolExecutor executor;
      final LyricsService lyricsService = new LyricsService();
      private PlayerSource media;
      private boolean mediaFailed;
      private SpotifyWindow window;
      private boolean windowFailed;
      private long nextWindowReadAt;
      private AlbumArt art;
      private int artVersion;
      private boolean artDecoded;
      private LyricsQuery lyricsCandidate;
      private int lyricsCandidatePolls;
      private LyricsQuery lyricsQuery;
      private SpotifyHudModule.TrackLyrics published;
      private int sessionFailures;
      private String lastError;
      private long lastErrorLoggedAt;

      Poller() {
         this.lyricsService.setOnline(SpotifyHudModule.this.lyricsOnline.get());
         this.executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "AtlasVisuals-Spotify");
            thread.setDaemon(true);
            return thread;
         });
         this.executor.setRemoveOnCancelPolicy(true);
         this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      }

      void start() {
         this.executor.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
      }

      void stop() {
         try {
            this.executor.execute(this::closeMedia);
         } catch (RejectedExecutionException var2) {
         }

         this.executor.shutdown();
         this.lyricsService.close();
      }

      private void closeMedia() {
         if (this.media != null) {
            this.media.close();
         }
      }

      boolean press(SpotifyWindow.MediaKey key) {
         try {
            this.executor.execute(() -> this.control(key));
            this.executor.schedule(this::poll, 250L, TimeUnit.MILLISECONDS);
            return true;
         } catch (RejectedExecutionException var3) {
            return false;
         }
      }

      private void poll() {
         if (SpotifyHudModule.this.poller == this) {
            try {
               NowPlaying next = this.readSession();
               if (next == null) {
                  if (this.sessionFailures > 0 && this.sessionFailures <= 2 && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                     return;
                  }

                  long now = System.currentTimeMillis();
                  if (SpotifyHudModule.this.nowPlaying.source() != NowPlaying.Source.SESSION && now < this.nextWindowReadAt) {
                     return;
                  }

                  this.nextWindowReadAt = now + 1000L;
                  next = this.readWindow();
               }

               if (next != null) {
                  SpotifyHudModule.this.publish(this, next, this.lyricsFor(next));
               }
            } catch (LinkageError | RuntimeException var4) {
               SpotifyHudModule.LOGGER.debug("Spotify HUD poll failed", var4);
            }
         }
      }

      private NowPlaying readSession() {
         if (this.mediaFailed) {
            return null;
         } else {
            if (this.media == null) {
               try {
                  this.media = PlayerSource.spotify();
               } catch (LinkageError var3) {
                  SpotifyHudModule.LOGGER.warn("Spotify HUD: media session unavailable ({})", var3.toString());
                  this.mediaFailed = true;
                  return null;
               }
            }

            if (this.media.isUnsupported()) {
               return null;
            } else {
               MediaSession.Snapshot snapshot = this.media.poll();
               if (snapshot != null) {
                  this.sessionFailures = 0;
                  return NowPlaying.fromSession(snapshot, this.artFor(snapshot), SpotifyHudModule.this.nowPlaying);
               } else {
                  String error = this.media.lastError();
                  this.logError(error);
                  this.sessionFailures = error != null && !this.media.isUnsupported() ? this.sessionFailures + 1 : 0;
                  return null;
               }
            }
         }
      }

      private AlbumArt artFor(MediaSession.Snapshot snapshot) {
         if (!SpotifyHudModule.this.showAlbumArt.get()) {
            this.art = null;
            this.artDecoded = false;
            return null;
         } else {
            if (!this.artDecoded || snapshot.thumbnailVersion() != this.artVersion) {
               this.artDecoded = true;
               this.artVersion = snapshot.thumbnailVersion();
               this.art = null;
               if (snapshot.hasThumbnail()) {
                  try {
                     this.art = AlbumArt.decode(snapshot.thumbnail(), 256);
                  } catch (RuntimeException | LinkageError var3) {
                     SpotifyHudModule.LOGGER.debug("Spotify HUD: album art could not be decoded", var3);
                  }
               }
            }

            return this.art;
         }
      }

      private NowPlaying readWindow() {
         if (!Platform.isWindows()) {
            if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
               return NowPlaying.NOT_RUNNING;
            } else {
               this.executor.shutdown();
               return NowPlaying.UNSUPPORTED;
            }
         } else {
            if (!this.windowFailed && this.window == null) {
               try {
                  this.window = new SpotifyWindow();
               } catch (LinkageError var2) {
                  this.windowFailed(var2);
               }
            }

            if (this.window != null) {
               try {
                  return NowPlaying.fromWindowTitle(this.window.readTitle(), SpotifyHudModule.this.nowPlaying);
               } catch (LinkageError var3) {
                  this.windowFailed(var3);
               }
            }

            if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
               return NowPlaying.NOT_RUNNING;
            } else {
               this.executor.shutdown();
               return NowPlaying.UNSUPPORTED;
            }
         }
      }

      private void windowFailed(LinkageError failure) {
         SpotifyHudModule.LOGGER.warn("Spotify HUD: Win32 access unavailable ({})", failure.toString());
         this.window = null;
         this.windowFailed = true;
      }

      private SpotifyHudModule.TrackLyrics lyricsFor(NowPlaying now) {
         if (!SpotifyHudModule.this.wantsLyrics(now)) {
            this.lyricsCandidate = null;
            this.lyricsCandidatePolls = 0;
            this.lyricsQuery = null;
            this.published = null;
            return null;
         } else {
            LyricsQuery query = new LyricsQuery(now.title(), now.artist(), now.album(), 0).withDurationMillis(now.durationMs());
            if (query.equals(this.lyricsCandidate)) {
               this.lyricsCandidatePolls++;
            } else {
               this.lyricsCandidate = query;
               this.lyricsCandidatePolls = 1;
            }

            if (this.lyricsCandidatePolls >= 2) {
               this.lyricsQuery = this.lyricsCandidate;
            }

            if (this.lyricsQuery != null && this.lyricsQuery.title().equals(now.title()) && this.lyricsQuery.artist().equals(now.artist())) {
               if (this.published == null
                  || !this.published.query().equals(this.lyricsQuery)
                  || !this.published.title().equals(now.title())
                  || !this.published.artist().equals(now.artist())) {
                  this.published = new SpotifyHudModule.TrackLyrics(now.title(), now.artist(), this.lyricsQuery);
               }

               return this.published;
            } else {
               return this.published;
            }
         }
      }

      private void control(SpotifyWindow.MediaKey key) {
         if (SpotifyHudModule.this.poller == this) {
            if (this.media != null && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
               boolean accepted = switch (key) {
                  case PLAY_PAUSE -> this.media.togglePlayPause();
                  case NEXT -> this.media.skipNext();
                  case PREVIOUS -> this.media.skipPrevious();
               };
               if (accepted || this.media.lastError() == null) {
                  return;
               }

               SpotifyHudModule.LOGGER.debug("Spotify HUD: media session control failed ({}); sending the media key", this.media.lastError());
            }

            if (Platform.isWindows()) {
               try {
                  SpotifyWindow.tap(key);
               } catch (RuntimeException | LinkageError var3) {
                  SpotifyHudModule.LOGGER.debug("Spotify HUD media key failed", var3);
               }
            }
         }
      }

      private void logError(String error) {
         if (error != null) {
            long now = System.currentTimeMillis();
            if (!Objects.equals(error, this.lastError) || now - this.lastErrorLoggedAt >= 60000L) {
               this.lastError = error;
               this.lastErrorLoggedAt = now;
               SpotifyHudModule.LOGGER.debug("Spotify HUD media session: {}", error);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum SkyEmojiStyle {
      BESIDE,
      BEHIND,
      REPLACE_WORD;
   }

   @Environment(EnvType.CLIENT)
   private record TrackLyrics(String title, String artist, LyricsQuery query) {
   }
}
