# Atlas Visuals

**Purely visual effects for Fabric: a glowing night sky that sings along to your Spotify, hand auras, coloured water and a gothic look. No cheats, no ESP. Everything you see is only on your screen.**

## Spotify Sky

Play a song in the Spotify desktop app and the sky lights up.

- **Synced lyrics in the sky.** The current line is shown across the sky, in time with the song. This works for any song [LRCLIB](https://lrclib.net) has synced lyrics for, not only selected artists.
- **The glow stays on through the whole session.** It doesn't flicker off between tracks, and it keeps going on songs that have no lyrics.
- **Gothic sky symbols.** Pentagrams, inverted crosses, bats, skulls, spider webs, coffins, crescent moons and gravestones drift across the sky, together with stars, roses and hearts.
- **Emoji for the words being sung.** When a line sings about a heart, fire or a broken heart, a matching emoji appears next to it, in your theme colour.
- Shooting stars, meteors, glitter and soft fog while the music plays.

## Hands and items

- **Hand Aura**: a soft glow around your hand and held item.
- **Hand Pose**: moves the held item. It sits farther away by default, so your pickaxe no longer covers half the screen.
- **3D Hand**, **Swing Animation** (several swing styles) and **Weapon Inspect** (press **F** for a knife inspect animation).
- **Custom Glint**: your own colour for the enchantment glint.

## World and particles

- **Water Color**: water in any colour, including the fog underwater.
- **Jump Circles**: a glowing pentagram ring where you land.
- **Atmosphere**: a themed sky and clouds, with or without music.
- **Hit Particles**, **Floating Relics**, **Particles** (pentagram, sand and spark trails), **Totem Spirit**.
- **Color Grading**, **Motion Blur**, **Custom FOV**, **Time Changer** (client-side only, the server time is unaffected).

## One colour for everything

The **Colors** tab sets one theme colour for the whole mod: sky, fog, water, aura, particles, lyrics and symbols. You can type any hex code, for example `#FF50B0`. Single effects can still be changed afterwards.

## Settings

Everything is set up through **Mod Menu → Atlas Visuals**, or with `/atlasvisuals` in a world. The menu is sorted into categories and has a search box. The search also finds settings inside the effects.

There is no extra keybind that opens a menu, and nothing is added to the title screen. The game starts exactly as it would without the mod.

## Performance

Measured on an RTX 5070, with everything switched on at the same time, the mod added about 0.7 ms per frame. The default set added about 0.25 ms, and most single effects stay under 0.1 ms. Tested together with Sodium, and works in Lunar Client.

## Requirements

| | Minecraft 1.21.11 | Minecraft 26.3 |
|---|---|---|
| Loader | Fabric Loader | Fabric Loader 0.20+ |
| Java | 21 | 25 |
| Needed | Fabric API | Fabric API |
| Optional | Mod Menu, Sodium | Mod Menu, Sodium |

**Lunar Client compatible:** add the jar to a Fabric profile in Lunar Client and it works there too, settings included.

The lyrics need the **Spotify desktop app** (tested on Windows). Browser Spotify is not supported.

## Privacy

To find lyrics, the mod sends the **song title and artist** to [LRCLIB](https://lrclib.net). Nothing else is sent: no account, no login, no Minecraft data. Found lyrics are cached on your PC. You can turn this off with **Lyrics Online** in the Spotify settings. The sky still glows without it.

## Credits and license

Atlas Visuals is licensed under **GPL-3.0-only**. The visual effects are based on [Anubis](https://github.com/4ldenz/Anubis-client) by 4ldenz (GPL-3.0). This is not an official Anubis build. None of Anubis' combat, movement, macro, ESP or account features are included.

Lyrics belong to their rights holders. They are loaded while you play and are never included in the mod file. The font is Manrope, under the SIL Open Font License.
