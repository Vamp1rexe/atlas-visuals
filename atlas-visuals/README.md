# Atlas Visuals

A client-side Fabric mod for Minecraft 1.21.11 with visual effects only:

- **Spotify Sky**: the sky glows for as long as music plays, also through song changes and on songs without lyrics. Synced lyrics appear in the sky for any song LRCLIB has them for.
- **Water Color**, **Hand Aura**, **Hand Pose** (moves the held item, farther away by default), 3D Hand, Swing Animation, Weapon Inspect
- Atmosphere, Color Grading, Motion Blur, Custom Glint, Custom FOV, Time Changer
- Jump Circles (a pentagram where you jump), Hit Particles, Floating Relics, Particles, Totem Spirit

## Settings

The menu has a search box, which also finds settings inside the effects, and groups the effects by category.

**Colors** at the top of the menu sets one colour for everything at once: the glowing sky, fog, water, hand aura, particles and the rest.
You can still change single effects afterwards; picking a new theme colour replaces those.

Open **Mod Menu > Atlas Visuals > the settings button**, or type `/atlasvisuals` in a world. Settings are stored in
`config/atlas-visuals.json`.

## Requirements

Fabric Loader, Fabric API and Java 21. Mod Menu is optional. Spotify must be the desktop app.

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
