# TETRA INSIGHT PROJECT KNOWLEDGE

Tetra Insight is a client-side Minecraft 1.20.1 Forge addon for Tetra interface and material decision support.

## STACK

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Tetra 6.17.0
- mutil 6.3.0
- Gradle 8.8 wrapper

## BOUNDARIES

- Source and builds live in `D:\learnmod\TetraInsight`.
- CDR pack integration stays in `D:\game\.minecraft\versions\CDRdev` and happens only after an explicit sync task.
- The mod is client-side and must not register save data or require installation on the server.
- Do not copy or redistribute Tetra/mutil source code or textures; reference dependency classes and resources at runtime.

## ARCHITECTURE

- `integration/tetra/`: version-sensitive Tetra data capture and adapters.
- `integration/tetra/model/`: immutable read-only display models.
- `mixin/tetra/`: narrow injection points only.
- `client/`: commands and later GUI composition.
- Keep GUI code independent of Tetra private fields by consuming display models.

## UI RULE

Reuse Tetra/mutil GUI components, textures, spacing, colors, hover/selected/disabled states, and sounds wherever possible. New pagination, search, clipping, and expandable controls should be thin adaptations that retain Tetra's visual language, not a replacement theme.

- Keep custom material-dossier chrome and text in a neutral black, white, and gray hierarchy. Do not add cyan, gold, or other category colors; retain color only where it comes from a reused native Tetra glyph or stat bar.
- Use stable upstream holosphere category translations for material-usage tree roots: Tetra provides `tetra.holo.craft.modular_*`, and GeoTetraArmor provides `tetra.holo.craft.head/chest/legs/feet`. Do not use `IModularItem.getItemName` as a category label because it dynamically describes the assembled stack.
- Workbench shortcuts that target the holosphere's pre-schematic module list must enter `HoloCraftRootGui.onSlotSelect`; passing a null schematic to `HoloGui.openSchematic` reaches `HoloSchematicGui.update` and crashes. After setting the screen and navigation state, call `HoloGui.onShow()` and let the craft root animate from its computed depth instead of opening individual child animations.

## MATERIAL VISIBILITY

- Item-to-material shortcuts must never hide ambiguity: when an item matches multiple definitions, open an explicit paged definition session instead of silently committing to one result.
- Treat each H shortcut as a fresh material session captured at keypress. Because `HoloGui` is a reused singleton, reset the dossier/modal, clear stale focus before selecting that material, clear both `HoloMaterialListGui.selectedItem` and `hoveredItem` when the session closes, and cancel pending auto-open state when the dossier or screen closes.
- Match Tetra's material-detail entrance timing for the custom dossier: keep the panel at zero opacity during the native 120 ms holographic delay, then use an 80 ms fade with at most two pixels of movement so dossier text never appears ahead of the transition.
- Initialize every material-usage tree with item, slot, and improvement-bearing module branches collapsed; expansion is an explicit player action, while switching tabs preserves the current tree state.
- Collapse both global `HoloMaterialListGui` and schematic `HoloVariantListGui` categories above eight entries to seven visible entries plus a Tetra-styled expand control in the eighth native slot; show a clear title-adjacent collapse control after expansion, pin an out-of-window selection, and allow only one expanded category at a time.
- ExtraHoloPage 1.2.16 discards constructed Tetra `Holo*GroupGui` objects and adds `MyHolo*GroupGui` replacements. Keep native group mixins plus `@Pseudo` replacement mixins, make list mixins discover final children through fold interfaces instead of Tetra concrete classes, and stop only a relocated selected entry's animation before reapplying its compact slot.
- `MaterialData.hidden` only hides a material from Tetra's global material browser. Keep it in schematic-scoped candidate enumeration and expose `hiddenInGlobalMaterialBrowser` in the display model.
- `MaterialData.hiddenOutcomes` prevents material outcomes and is the visibility flag that excludes a material from schematic-scoped candidates.
- When duplicate logical materials merge, mark the result globally hidden only if every merged definition is globally hidden.
- Treat a non-`MaterialOutcomeDefinition` outcome with a valid material predicate as a special ingredient use. Match it against the exact item stack captured by the H shortcut, retain that stack while paging ambiguous material definitions, and display those uses separately from ordinary material attributes, usage counts, and compatibility statistics.
- For dossier stat previews, pass the before/after modular stacks to native `HoloStatsGui`. Resolve the outcome through `MaterialGlyphTintResolver` and require the exact `materialKey`; matching only the ingredient item is ambiguous when one item has multiple material definitions.
- Tetra 6.17 keeps each `HolosphereCraftState.ItemState.workingStack` for the lifetime of the reused `HoloGui`. Remove the root `id` tag from every stack copied into `workingStack` for previews, or Tetra's `propertyCache` and `toolCache` reuse one data key across candidate outcomes and leak remaining integrity, tool level, and efficiency. Treat the native workbench "view details" shortcut as navigation-only: reset its entry to the default stack before `openFromWorkbench`, reserve actual or synthetic overrides for explicit material-usage navigation, and restore defaults when the screen closes or normal browsing resumes because hammer and pick configurations share `tetra:modular_double`.

## DISPLAY TRANSLATION

- Existing schematic `translation` data is authoritative. Keep its display levels even when no matching actual extract coefficient can be found.
- Only generate a display translation when the schematic has no author translation. Calibrate levels from schematics that provide both author translation and linked extracts.
- Render signed display levels with Tetra's roman-numeral convention (`+I`, `-II`, `+III`), not Arabic numerals.
- Roman numerals are qualitative influence levels. Keep exact Arabic coefficients in a Shift-held formula view and never present a calibrated level as a final stat delta.
- For duplicate actual outputs, use the strongest coefficient only when all definitions agree on direction. Omit conflicting outputs instead of guessing.
- Distinguish an empty linked extract (`NO_MATERIAL_SCALING`) from an unresolvable definition (`UNAVAILABLE`).

## SORTER UI

- Paginate Tetra sorter popovers at nine items per page and keep page state contextual to one popover update.
- Contextual fallback sorters must be derived only from actual material `extract` entries linked to the current schematic. Do not use author `translation`, material-inherent effects, or a scan of final outcome stacks as sorter sources.
- Deduplicate generated sorters by getter semantics and never write them into global `StatSorters.derivedSorters`.
- Existing static, derived, and datapack sorter definitions always take precedence over generated attribute/effect sorters.
- Override inherited mutil input handlers such as `onMouseScroll`; injecting them into a Tetra subclass that does not declare the method causes a critical Mixin failure.
- Reused `HoloFilterButton` keeps text-entry focus in a private field; pagination and numeric shortcuts must check that focus before consuming keys such as digits, minus, equal, arrows, or page keys.
- Tetra's `HoloFilterButton` handles Backspace but not Delete and gives an empty field no visible click target. Sorter search should forward both deletion keys, allow clearing a retained query even after focus feedback is lost, and show a Tetra-styled idle hint only while the query is empty and unfocused.
- Sorter cancellation must remain visible regardless of the search query. Present a localized clear action backed by `StatSorters.none`, pin it in filtered results, and reset the transient search query after any sorter selection so reopening never traps the user in a filtered subset.

## IMPROVEMENT UI
- Paginate the improvement overview at nine entries per page; the three native layout groups distribute a full page as a compact 3x3 grid.
- Persistent discovery controls in `HoloSchematicGui` must forward Tetra's existing `onVariantOpen` flow instead of opening a parallel improvement screen.
- Capture the improvement count already computed by `HoloVariantDetailGui.updateVariant`; do not rescan `SchematicRegistry` just to update discovery labels.
- Keep improvement discovery as a compact overview. Open one selected schematic in a detail subpage before rendering its levels, materials, consumables, tool requirements, and experience cost.
- Material-backed improvement previews must contain at least one non-empty `ItemStack`; filter invalid previews before overview grouping and chain construction so empty levels never affect layout or selection state.
- Treat `HoloDisplaySchematic` previews as UI snapshots only. Selection recomposition and hover prediction must unwrap delegates, rebuild the full combination from the base stack, and apply book enchantments last; cached or enchant-first previews can discard later improvement state.
- When hiding or switching improvement views, clear transient UI state without calling `updateSelection` or `getPreviews` on a stale or empty stack; Tetra applies improvement outcomes by casting the stack item to `IModularItem`, so an `AirItem` recomputation crashes.
- Reuse Tetra/mutil keyframe animations, but keep dense-page transitions subtle: short fades and at most two pixels of horizontal movement; do not stagger large rows of content.
- Paginate dynamic stat collections before they exceed their native three-row bounds. Native `HoloStatsGui` uses five columns and 14 bars per page with the final grid cell reserved for controls; the full-screen material dossier uses a roomier four-column, 11-bar override. `WorkbenchStatsGui` uses 18 bars per page. All support arrow buttons and mouse-wheel paging.

## COMMANDS

```powershell
.\gradlew.bat build --no-daemon
.\gradlew.bat runClient --no-daemon
```

Runtime probe commands:

```text
/tetrainsight probe
/tetrainsight probe dump
/tetrainsight probe candidates <query>
```

## VERIFICATION

- Build before reporting implementation progress.
- After switching branches with uncommitted feature work, verify both new classes and their tracked event/mixin entrypoints; untracked classes can survive while tracked hooks revert, leaving a buildable but unreachable partial feature.
- For data capture changes, enter a development world and check `run/logs/latest.log` for all three capture counts.
- Resource reloads must replace snapshots rather than append duplicates.
- Existing author `translation` data always takes precedence over generated display data.
- Sorter verification must confirm that only actual linked `extract` attributes and effect dimensions are added; author `translation` and material-inherent effects must not add sorter entries.
