# Changelog

## 0.1.3

- Add compact material identity, tendency and intrinsic-data summaries to matching item tooltips.
- Open a hovered material directly in the holosphere with the configurable material-dossier key.
- Add Tetra-styled material and compatible-schematic dossier pages to the global material browser.
- Delay and fade material-dossier content with Tetra's native holosphere transition timing.
- Start material-usage trees fully collapsed while preserving expansion state across dossier tabs.
- Add subtle transitions when opening, closing, switching dossier tabs, and paging ambiguous material definitions.

## 0.1.2

- Initialize the native holosphere screen lifecycle when opening an empty workbench slot through the shortcut.
- Let the craft root animate from its computed navigation depth, preventing the item-level module overview from rendering as a black page.

## 0.1.1

- Collapse schematic material categories larger than eight entries behind a category-level expand control, while keeping the selected material visible.
- Reduce improvement overview pages to nine entries so a full page uses a compact 3x3 layout.
- Detect non-honing improvement chains from explicit same-key improvement prerequisites, while leaving parallel level-scaled schematics such as Quick Latch separate.
- Group material and fixed-consumable chain levels in one detail page with chain-wide mutual exclusion and material-aware selection visuals.
- Hide material-backed improvement levels that resolve to no actual item, and show schematic names in chain-level tooltips while retaining compact roman-numeral buttons.
- Avoid recomputing improvement previews from stale or empty stacks when switching views, preventing `AirItem` cast crashes.
- Keep the development layout probe diagnostic when toolbar measurements change instead of terminating the client.

## 0.1.0-alpha.3

- Added explicit base-to-selected and selected-to-hovered stat comparison modes for improvement previews.
- Rebuilt combined improvement previews from the base item so selected improvements contribute to later hover predictions.
- Applied book enchantments after ordinary improvements and honing, preventing enchantment previews from hiding or discarding other improvement state.
- Fixed stale improvement, honing and hover state when changing selections or closing the improvement browser.
- Kept the comparison label in its original stat header position and separated the detail-page back action from third-row stat bars.

## 0.1.0-alpha.2

- Reworked improvement browsing into a compact overview and a focused detail subpage.
- Kept material choices, consumables, enchantment levels, honing levels and crafting requirements out of the overview until requested.
- Added restrained Tetra-style fades for overview, detail and pagination transitions.
- Added paginated dynamic stat bars to the holosphere and workbench so large third-party stat collections no longer overlap adjacent UI.
- Added mouse-wheel navigation for stat-bar pages.

## 0.1.0-alpha.1

- Added persistent Tetra workbench navigation labels and clearer material-information entry points.
- Added material-impact explanations derived from actual Tetra material extracts when author display data is absent.
- Added paginated effect and attribute filtering with searchable, removable selections.
- Added paginated improvement and honing browsing with module-ownership filtering.
- Added material-based improvement previews, fixed-consumable summaries, enchantment grouping, synergy details and module-aspect details.
- Added compatibility safeguards for missing material definitions and large third-party schematic collections.
