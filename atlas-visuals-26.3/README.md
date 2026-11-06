# Atlas Visuals

A client-side Fabric mod for Minecraft 26.3 with visual effects only. (A separate build exists for 1.21.11.)

- **Spotify Sky**: the sky glows for as long as music plays, also through song changes and on songs without lyrics. Synced lyrics appear in the sky for any song LRCLIB has them for.
- **Water Color**, **Hand Aura**, **Hand Pose** (moves the held item, farther away by default), 3D Hand, Swing Animation, Weapon Inspect
- Atmosphere, Color Grading, Motion Blur, Custom Glint, Custom FOV, Time Changer
- Jump Circles (a pentagram where you jump), Hit Particles, Floating Relics, Particles, Totem Spirit
- Light Overlay (where mobs can spawn, like Lighty's carpet mode; F8 switches it on and off)

## Settings

The menu has a search box, which also finds settings inside the effects, and groups the effects by category.

**Colors** at the top of the menu sets one colour for everything at once: the glowing sky, fog, water, hand aura, particles and the rest.
You can still change single effects afterwards; picking a new theme colour replaces those.

Open **Mod Menu > Atlas Visuals > the settings button**, or type `/atlasvisuals` in a world. Settings are stored in
`config/atlas-visuals.json`.

## Requirements

Fabric Loader 0.20 or newer, Fabric API for 26.3 and Java 25. Mod Menu is optional. Spotify must be the desktop app.

Differences from the 1.21.11 build:

- Water Color repaints the water you can see. Water outside your view takes the new colour when it comes into view, rather than ahead of time.
- Key binds use the game's new key numbers, so binds saved by the 1.21.11 build have to be set again.
- Tested with Sodium 0.9.3 beta and Mod Menu 21.0.0, and with "Improved Transparency" on and off.

The song title and artist are sent to [LRCLIB](https://lrclib.net) to look up lyrics. Turn off "Lyrics Online" to stop that.

## Building

```
gradlew build
```

The jar is written to `build/libs/`.

- `gradlew runClientGameTest -PtestMods="sodium.jar;modmenu.jar"` loads a world automatically, checks the hex colour input and takes
  screenshots.
- `gradlew prodClient -PtestMods="fabric-api.jar;sodium.jar;modmenu.jar"` opens the built jar in a production Fabric install to play by hand.

## License and credit

GPL-3.0-only, see `LICENSE`. The visual effects are derived from [Anubis](https://github.com/4ldenz/Anubis-client) by 4ldenz
(GPL-3.0), decompiled from version 0.9.9.3. This is not an official Anubis build. Anubis' combat, movement, macro, ESP and account
features are not included. Lyrics text belongs to its rights holders and is fetched at runtime, never bundled.
Manrope font: SIL Open Font License, see `assets/atlas_visuals/fonts/`.
The Light Overlay follows the carpet mode of [Lighty](https://github.com/SchmarrnDevs/Lighty) by andi_makes and contributors (Apache-2.0).
