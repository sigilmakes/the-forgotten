# The Forgotten — Project Instructions

Minecraft Fabric mod (1.21.4, Java 21). A dimension hidden beneath the ancient cities, accessed through reinforced deepslate portal frames activated with echo shards.

## Build & Test

```bash
./gradlew build          # compile + jar → build/libs/
./gradlew runClient      # launch Minecraft with mod loaded (requires display)
./gradlew runDatagen     # regenerate data (loot tables, tags, etc.)
```

No automated test suite — testing is in-game. If you change Java code, at minimum verify `./gradlew build` succeeds.

## Architecture

```
src/main/java/com/lemoneater/theforgotten/
├── TheForgotten.java           # Mod entrypoint — block registration, portal activation event
├── TheForgottenClient.java     # Client entrypoint — render layer registration
├── TheForgottenDataGenerator.java  # Data generation entrypoint
├── block/
│   ├── ModBlocks.java          # Block definitions (palestone family + portal)
│   └── ForgottenPortalBlock.java  # Portal block — collision teleport, particles, sounds, frame validation
├── portal/
│   └── PortalHelper.java       # Frame detection — finds rectangular reinforced deepslate frames, fills with portal
├── world/
│   └── ModDimensions.java      # Dimension registry key
└── mixin/
    └── BlocksMixin.java        # Makes reinforced deepslate mineable (redirects strength settings)

src/main/resources/
├── data/the-forgotten/
│   ├── dimension/the_forgotten.json       # Dimension definition
│   ├── dimension_type/the_forgotten.json  # Dimension type (0.05 ambient light, no ceiling, etc.)
│   ├── worldgen/
│   │   ├── noise_settings/the_forgotten.json  # Nether-style cavern noise (192 blocks, palestone)
│   │   └── biome/forgotten_wastes.json        # Biome — sculk veins, glow lichen, soul sand valley ambience
│   └── loot_table/blocks/                     # Palestone block drops
├── data/minecraft/
│   ├── loot_table/block/reinforced_deepslate.json  # Override: makes it drop itself
│   └── tags/block/                                  # Mining tags for palestone + reinforced deepslate
└── assets/the-forgotten/
    ├── textures/block/          # Block textures (palestone variants + animated portal)
    ├── models/block/            # Block models
    ├── blockstates/             # Blockstate definitions
    ├── items/                   # Item models
    └── lang/en_us.json          # Translations
```

## Key Patterns

- **Block registration:** Use `ModBlocks.register()` helper — takes name, factory, settings, and whether to create a BlockItem.
- **Portal activation:** `UseBlockCallback` event in `TheForgotten.java` → `PortalHelper.tryLightPortal()` → fills frame with portal blocks.
- **Teleportation:** `ForgottenPortalBlock.onEntityCollision()` → `TeleportTarget` API. Currently player-only (`instanceof ServerPlayerEntity` check).
- **Frame detection:** `PortalHelper.detectFrame()` walks outward from an interior position to find the rectangular frame. Validates all edges and corners.
- **Mixin:** `BlocksMixin` uses `@Redirect` on `Blocks.<clinit>` to change reinforced deepslate properties (makes it mineable with diamond tools).

## Conventions

- **Package:** `com.lemoneater.theforgotten`
- **Mod ID:** `the-forgotten` (with hyphen)
- **Registry IDs:** Use underscores (`the_forgotten`, `palestone_bricks`)
- **Commit style:** Conventional commits — `feat:`, `fix:`, `art:`, `docs:`
- **Branches:** `feature/<name>` for new features, merge to `main`
- **Indentation:** 4 spaces

## Notes

- Portal search (`findNearestPortal`) does a 128-block radius brute-force scan. Fine for now but could be expensive if called frequently in loaded chunks. If it becomes a problem, consider a POI-based approach or caching portal locations.
- The dimension uses `coordinate_scale: 1.0` — no nether-style 8:1 coordinate mapping. Portals map 1:1 between overworld and The Forgotten.
- `findSafePosition()` searches upward from y=24 through the cavern range. The noise settings create caverns between roughly y=24 and y=160.
