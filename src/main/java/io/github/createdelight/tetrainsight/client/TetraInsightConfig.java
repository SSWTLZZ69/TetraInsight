package io.github.createdelight.tetrainsight.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Client-side feature switches for Tetra Insight. All options default to the
 * mod's enhanced behavior; every entry can be disabled independently to fall
 * back to a plainer presentation.
 */
public final class TetraInsightConfig {
    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    /**
     * Master switch for the compact improvement overview. When disabled the
     * native three-row improvement list is shown again; overview-only
     * additions (right-click deselect, compact rows, requirement tooltips)
     * have no effect then.
     */
    public static final ForgeConfigSpec.BooleanValue improvementOverview =
            BUILDER.comment(
                    "Replace Tetra's improvement page with a compact overview "
                            + "that opens details on demand.",
                    "Disable to restore the native improvement list.")
                    .define("improvementOverview", true);

    /** Right click in the improvement overview removes a selected variant. */
    public static final ForgeConfigSpec.BooleanValue improvementRightClickDeselect =
            BUILDER.comment(
                    "Right-clicking a selected entry in the improvement "
                            + "overview removes it without opening details.")
                    .define("improvementRightClickDeselect", true);

    /**
     * Show crafting requirements (materials, tool levels, experience) in the
     * overview entry tooltip instead of only on the detail page.
     */
    public static final ForgeConfigSpec.BooleanValue improvementOverviewRequirements =
            BUILDER.comment(
                    "Add material, tool and experience requirements to the "
                            + "improvement overview tooltip.")
                    .define("improvementOverviewRequirements", true);

    /**
     * Include enchantment schematics (book_enchant) in the improvement
     * overview. These have no material extracts, so they are skipped by the
     * material-driven listing without this fallback.
     */
    public static final ForgeConfigSpec.BooleanValue enchantmentImprovements =
            BUILDER.comment(
                    "List book enchantments inside the improvement overview.")
                    .define("enchantmentImprovements", true);

    /**
     * Collapse material and module-variant categories that exceed eight
     * entries into seven entries plus an expand control.
     */
    public static final ForgeConfigSpec.BooleanValue materialGroupFolding =
            BUILDER.comment(
                    "Collapse holosphere material categories with more than "
                            + "eight entries behind an expand control.",
                    "Disable to always render every entry in a category.")
                    .define("materialGroupFolding", true);

    /**
     * Restrict collapsed category groups to a single expanded category at a
     * time. When disabled, every category can be expanded independently.
     */
    public static final ForgeConfigSpec.BooleanValue materialSingleExpansion =
            BUILDER.comment(
                    "Only one collapsed material category may be expanded at "
                            + "a time.",
                    "Disable to expand multiple categories simultaneously.")
                    .define("materialSingleExpansion", true);

    /**
     * Extended item tooltip for Tetra materials. When disabled the tooltip
     * keeps only the material category line and the holosphere shortcut hint.
     */
    public static final ForgeConfigSpec.BooleanValue materialTooltipDetails =
            BUILDER.comment(
                    "Show tendency, axes and intrinsic counts in material "
                            + "item tooltips.",
                    "Disable to only show the material category and the "
                            + "holosphere shortcut hint.")
                    .define("materialTooltipDetails", true);

    /**
     * Adds the effect search field and the hardness/density/flexibility sort
     * controls to the holosphere material list.
     */
    public static final ForgeConfigSpec.BooleanValue materialBrowserSearch =
            BUILDER.comment(
                    "Show the search field and material-axis sort buttons in "
                            + "the holosphere material browser.")
                    .define("materialBrowserSearch", true);

    /**
     * Augment workbench schematic list entries with crafting material and
     * requirement information on hover.
     */
    public static final ForgeConfigSpec.BooleanValue workbenchSchematicDetails =
            BUILDER.comment(
                    "Show crafting materials, tool levels and experience cost "
                            + "when hovering schematics in the workbench list.")
                    .define("workbenchSchematicDetails", true);

    private static ForgeConfigSpec spec;

    private TetraInsightConfig() {
    }

    public static void register() {
        spec = BUILDER.build();
        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.CLIENT, spec,
                        "tetra_insight-client.toml");
    }
}
