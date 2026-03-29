# The Forgotten — Project Instructions

Minecraft Fabric mod (1.21.11, Java 21). A dimension hidden beneath the ancient cities, accessed through reinforced deepslate portal frames activated with echo shards.

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
│   └── ForgottenPortalBlock.java  # Portal block — destination-typed teleport, particles, sounds, frame validation
├── portal/
│   ├── PortalDestination.java  # Enum: FORGOTTEN, OVERWORLD, NETHER, END — destination types with color/dimension mapping
│   └── PortalHelper.java       # Frame detection, destination-filtered portal search, auto-frame building
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
- **Portal activation:** `UseBlockCallback` in `TheForgotten.java` checks item + dimension → `PortalHelper.tryLightPortal(world, pos, destination)`. Echo shard outside = FORGOTTEN, echo shard inside = OVERWORLD, flint & steel inside = NETHER, eye of ender inside = END.
- **Teleportation:** `ForgottenPortalBlock.onEntityCollision()` reads `DESTINATION` property → applies coordinate scaling → searches target dimension for matching portal → teleports.
- **Portal routing:** Destination is a block state property (`PortalDestination` enum), not a block entity. Color = destination: pale/blue → Forgotten, green → Overworld, red → Nether, purple → End.
- **Coordinate scaling:** `coordinate_scale: 8.0`. The Forgotten is fast travel for the End. Nether ↔ Forgotten is 1:1.
- **Frame detection:** `PortalHelper.detectFrame()` walks outward from an interior position to find the rectangular frame. Validates all edges and corners.
- **Mixin:** `BlocksMixin` uses `@Redirect` on `Blocks.<clinit>` to change reinforced deepslate properties (makes it mineable with diamond tools).

## Conventions

- **Package:** `com.lemoneater.theforgotten`
- **Mod ID:** `the-forgotten` (with hyphen)
- **Registry IDs:** Use underscores (`the_forgotten`, `palestone_bricks`)
- **Commit style:** Conventional commits — `feat:`, `fix:`, `art:`, `docs:`
- **Branches:** `feature/<name>` for new features, merge to `main`
- **Indentation:** 4 spaces

## Dev Environment

Uses a nix flake for JBR (JetBrains Runtime) with DCEVM for hot-reload support. Run `nix develop` to enter the shell.

### Hot-Reload Workflow

`runClient` launches Minecraft with a JDWP debug port on 5005 and DCEVM enhanced class redefinition enabled. After editing code:

```bash
./reload.sh              # compile + hot-swap in one step
# or manually (inside nix develop):
./gradlew classes        # compile only (~3 sec)
javac -d build/hotswap hotswap.java
java -cp build/hotswap hotswap build/classes/java/main --changed-since 10
```

- **`hotswap.java`** — connects to JDWP on port 5005 and pushes changed `.class` files into the running JVM
- **`reload.sh`** — convenience wrapper: wraps compile + hot-swap in `nix develop`
- **Requires** `./gradlew runClient` to be running (launched from a `nix develop` shell)
- Works for logic changes, new methods/fields. Mixin changes and new registry entries require a restart.

## Notes

- Portal search (`findNearestPortal`) does a 128-block radius brute-force scan. Fine for now but could be expensive if called frequently in loaded chunks. If it becomes a problem, consider a POI-based approach or caching portal locations.
- The dimension uses `coordinate_scale: 8.0` — same as the Nether. The Forgotten is fast travel for the End. Nether ↔ Forgotten is 1:1.
- Worldgen uses the overworld noise router for natural terrain + caves, with palestone as the default block. Surface is pale moss with pale oak forest.
- The sky uses the End skybox (`skybox: "end"`) with grey fog/sky colors for a grainy void look.
