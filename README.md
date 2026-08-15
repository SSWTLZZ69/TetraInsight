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
- Loads client resource JSON rules for third-party effect applicability without a Java integration.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.10 or newer in the 47.x line
- Tetra 6.17.x
- mutil 6.3.0 or newer

Tetra Insight is client-side. It does not need to be installed on a dedicated server.

## Effect applicability resources

Third-party effect applicability can be described by client resource JSON. Place one file per
effect at:

```text
assets/<effect namespace>/tetra_insight/effect_applicability/<effect path>.json
```

For example, `more_mod_tetra:beheading` uses
`assets/more_mod_tetra/tetra_insight/effect_applicability/beheading.json`:

```json
{
  "replace": false,
  "paths": [
    {
      "scopes": ["main_hand", "off_hand"],
      "triggers": ["kill_entity"],
      "stacking": "held_max"
    }
  ]
}
```

`replace: false` merges the resource paths with built-in Tetra and optional integration paths.
Set it to `true` only when the resource should completely replace built-in applicability.

Supported scope values are the lowercase names from `EffectScope`, including `held_item`,
`main_hand`, `off_hand`, `armor`, `helmet`, `curios`, `toolbelt`, `bow`, `crossbow`, `shield`,
`tool`, `weapon`, `inventory` and `modular_item`. Trigger values likewise use lowercase
`EffectTrigger` names, such as `attack`, `receive_hit`, `kill_entity`, `projectile`, `ability`
and `wear_passive`; `death` and `heal` cover lifecycle consumers.

Built-in stacking aliases are `item`, `current_item`, `inventory_max`, `armor_sum`,
`armor_max`, `single_piece`, `held_max`, `held_sum`, `curios_sum`, `curios_max` and `unknown`.
A full translation key can be used instead of a stacking alias. Manual paths do not need an
`evidence` field; a successfully loaded file automatically uses the generic manual-resource
source label. Resource reloads replace the complete manual-rule snapshot, so removed JSON files
do not leave stale definitions behind.

## Installation

1. Install Forge, Tetra and mutil for Minecraft 1.20.1.
2. Place the Tetra Insight JAR in the client `mods` folder.
3. Remove TetraClip if present; both addons modify overlapping Tetra hologram interfaces.

## Compatibility status

The first public builds are marked Alpha because Tetra Insight uses version-sensitive mixins around Tetra 6.17.x interfaces. It has been tested in a large modpack environment, with extensive third-party Tetra schematics and with ExtraHoloPage-style expanded layouts. Please include `latest.log` and the affected module or schematic name in bug reports.

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
