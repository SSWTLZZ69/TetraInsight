# Tetra Insight

![Tetra Insight pixel-art logo](docs/assets/tetra-insight-logo-512.png)

Tetra Insight is a client-side Forge addon for Minecraft 1.20.1 that makes Tetra's modular crafting information easier to discover and compare while preserving Tetra's existing visual style.

## Features

- Keeps the workbench detail, crafting and tweak navigation visible.
- Exposes material requirements and material-driven stat conversion without relying on hidden icon tooltips.
- Adds compact material tooltips and a holosphere dossier for intrinsic properties and compatible schematics.
- Generates display hints from actual Tetra material extracts only when an author did not provide display data.
- Adds searchable, paginated effect and attribute filtering with an explicit clear action.
- Adds a compact improvement overview with focused detail pages, pagination and module-specific ownership filtering.
- Compares the base item, the current selected combination and the hovered improvement without losing existing improvement state.
- Shows material-based improvements like module material choices, with Shift-held consumable details.
- Groups enchantment choices and exposes synergy bonuses and module aspects more clearly.
- Handles large third-party schematic collections and skips unusable material choices.
- Paginates oversized holosphere and workbench stat-bar collections before they overlap other UI.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.10 or newer in the 47.x line
- Tetra 6.9.x
- mutil 6.2.0 or newer

Tetra Insight is client-side. It does not need to be installed on a dedicated server.

## Installation

1. Install Forge, Tetra and mutil for Minecraft 1.20.1.
2. Place the Tetra Insight JAR in the client `mods` folder.
3. Remove TetraClip if present; both addons modify overlapping Tetra hologram interfaces.

## Compatibility status

The first public builds are marked Alpha because Tetra Insight uses version-sensitive mixins around Tetra 6.9.x interfaces. It has been tested in a large modpack environment, with extensive third-party Tetra schematics and with ExtraHoloPage-style expanded layouts. Please include `latest.log` and the affected module or schematic name in bug reports.

## Development

```powershell
.\gradlew.bat test build --no-daemon
```

Development probe commands:

```text
/tetrainsight probe
/tetrainsight probe dump
/tetrainsight probe candidates <query>
```

The pixel-art logo is generated reproducibly from a 32×32 logical sprite:

```powershell
.\tools\generate-logo.ps1
```

## License and attribution

Tetra Insight is licensed under the MIT License. It is not affiliated with or endorsed by Tetra. Tetra and mutil remain the property of their respective author; this project references dependency code and runtime resources but does not redistribute their source code or textures.
