# The Forgotten

A Minecraft mod about what lies beneath the ancient cities.

**Fabric 1.21.4** · [Roadmap](#roadmap)

---

## What is this?

Deep beneath the overworld, the ancient cities guard a secret. Their reinforced deepslate frames aren't ruins — they're doorways. Light one with an echo shard and step through into **The Forgotten**: a vast, pale dimension of stone caverns, sculk veins, and silence.

Nobody knows who built it. Nobody knows why they left. The sculk remembers, but it isn't talking.

## Features

### The Forgotten Dimension
- **Nether-style caverns** carved from palestone — 192 blocks of vertical space, pillars, overhangs, deep voids
- **Polished palestone floors**, **palestone brick ceilings**, scattered **deepslate veins**
- **Sculk veins and patches** threading through the stone — the deep dark, everywhere
- **Glow lichen** for sparse, ambient light
- **Soul sand valley ambience** — eerie, hollow, patient
- **0.05 ambient light** — bring torches or embrace the dark

### Portal System
- Build a frame of **reinforced deepslate** (minimum 2×3 interior, like a nether portal)
- Right-click the inside face with an **echo shard** to activate
- Designed to work with the **ancient city portal structure** — clear the sculk, use an echo shard, and the frame comes alive
- **Animated portal texture** — pale spectral wisps drifting through blue-grey
- **Sculk soul particles** and **enchant glyphs** float from the portal surface
- Rare **sculk catalyst bloom** ambient sound — silence punctuated, not filled

### Palestone
- **Palestone** — the base stone of The Forgotten. Pale, cold, ancient.
- **Polished palestone** — smoother variant, found on cavern floors
- **Palestone bricks** — structured variant, found on ceilings and in ruins

## Installation

Requires [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.4.

Drop `the-forgotten-x.x.x.jar` into your `mods/` folder.

## Building from Source

```bash
git clone https://github.com/Lemon9247/the-forgotten.git
cd the-forgotten
./gradlew build
```

The built jar will be in `build/libs/`.

Requires JDK 21.

---

## Roadmap

What's done, what's next, what's dreamed about.

### ✅ Done

- Palestone blocks (textures, models, loot tables, mining tags)
- Portal system (reinforced deepslate frame + echo shard activation, bidirectional)
- Portal visuals (animated texture, particles, ambient sound)
- Noise-based worldgen (nether-style caverns, 192 blocks tall)
- Surface rules (polished palestone floors, palestone brick ceilings, deepslate patches)
- Biome: forgotten_wastes (sculk veins, sculk patches, glow lichen, ambient loop)

### 🔨 Next Up

- [ ] Fix portal landing (currently lands on roof instead of inside caverns)
- [ ] Portal-to-portal matching (build a frame on arrival, remember paired locations)
- [ ] Mob/entity teleportation (currently player-only)

### 🗺️ Worldgen

- [ ] Custom noise parameters (tune cavern size, pillar frequency, openness)
- [ ] Multiple biomes — pale forest, sculk depths, hollow halls, memory wells
- [ ] More varied surface rules (cracked palestone, mossy variants)
- [ ] Scattered deepslate veins

### 🏗️ Blocks & Items

- [ ] More palestone variants (cracked, chiseled, stairs, slabs, walls)
- [ ] Pale wood (custom wood type for dead/pale trees)
- [ ] Memory fragments (lore items)
- [ ] Echo-infused tools/blocks

### 🎨 Visuals & Audio

- [ ] Dimension fog (custom color/density)
- [ ] Custom ambient sounds
- [ ] Portal particle improvements

### 🧟 Mobs

- [ ] The Forgotten — what lives here? Something that's been waiting.
- [ ] Passive creatures — pale, bioluminescent, cautious
- [ ] Warden interaction

### 🏚️ Structures

- [ ] Ruined outposts (partially collapsed palestone brick buildings)
- [ ] Memory archives (loot rooms with lore)
- [ ] Portal ruins (broken frames scattered through the dimension)

### 💡 Wild Ideas

- Custom music disc found only in The Forgotten
- A mechanic where the dimension remembers what you've done in it
- Sculk networks that connect distant points
- Something that happens when you stay too long

---

*Built by [Willow](https://github.com/Lemon9247) and [Wren](https://lemon9247.github.io/wren/) 🐦*
