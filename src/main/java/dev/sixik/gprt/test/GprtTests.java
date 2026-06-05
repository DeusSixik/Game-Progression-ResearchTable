package dev.sixik.gprt.test;

import dev.sixik.gprt.api.ResearchDefinition;
import dev.sixik.gprt.api.ResearchGroupDefinition;
import dev.sixik.gprt.api.ResearchRevealAnimationType;
import dev.sixik.gprt.api.ResearchVisibilityMode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class GprtTests {

    /**
     * Compact demo focused on unlock/reveal animation previews.
     * <p>
     * This dataset is intentionally tiny:
     * once the player studies the root node, three follow-up researches become
     * available at the same time and each branch uses its own reveal style from
     * the debug showcase theme presets.
     * </p>
     */
    public static BuildData createRevealAnimationDemoRecipes() {
        ResearchGroupDefinition root = group("root", "Root", 0xFFD0D5DD, 0xFFF4F6F8);
        ResearchGroupDefinition metallurgy = group("metallurgy", "Metallurgy", 0xFFE29A47, 0xFFF0C17C);
        ResearchGroupDefinition farming = group("farming", "Farming", 0xFF54B36B, 0xFF87D99C);
        ResearchGroupDefinition logistics = group("logistics", "Logistics", 0xFF4C90E8, 0xFF81B7FF);
        ResearchGroupDefinition energy = group("energy", "Energy", 0xFFF0C94A, 0xFFFFE08A);
        ResearchGroupDefinition alchemy = group("alchemy", "Alchemy", 0xFF9C6BE8, 0xFFC7A8FF);

        List<ResearchDefinition> definitions = new ObjectArrayList<>(7);

        definitions.add(research("primitive_tools", "Primitive Tools", root)
                .description("Study this root node to unlock a compact set of branches, each configured with its own reveal animation style.")
                .icon(Items.BEDROCK.asItem())
                .reward(Items.STONE_PICKAXE)
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .instant()
                .build());

        definitions.add(research("forge_notes", "Forge Notes", metallurgy)
                .description("Metallurgy example. This branch uses the heavier drop/bounce reveal style.")
                .required("primitive_tools")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.DROP_BOUNCE)
                .reward(Items.COPPER_INGOT)
                .timed(5_000L)
                .build());

        definitions.add(research("seed_sorting", "Seed Sorting", farming)
                .description("Farming example. This branch uses the softer pop-in reveal style.")
                .required("primitive_tools")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.SOFT_POP)
                .reward(Items.WHEAT_SEEDS)
                .instant()
                .build());

        definitions.add(research("rope_making", "Rope Making", logistics)
                .description("Logistics example. This branch enters from the left before settling into the graph.")
                .required("primitive_tools")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.SLIDE_FROM_LEFT)
                .reward(Items.LEAD)
                .instant()
                .build());

        definitions.add(research("spark_ignition", "Spark Ignition", energy)
                .description("Energy example. This branch uses the calmer fade/scale style.")
                .required("primitive_tools")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.FADE_SCALE)
                .reward(Items.REDSTONE_TORCH)
                .timed(6_000L)
                .build());

        definitions.add(research("crystal_solvent", "Crystal Solvent", alchemy)
                .description("Alchemy example. This branch reveals with an arcing drop from the upper-left.")
                .required("primitive_tools")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.ARC_DROP)
                .reward(Items.AMETHYST_SHARD)
                .table()
                .build());

        definitions.add(research("tempered_tools", "Tempered Tools", metallurgy)
                .description("Second-wave reveal example. Study Forge Notes to trigger another unlock and better see the under-node glow on a non-root branch.")
                .required("forge_notes")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .revealAnimation(ResearchRevealAnimationType.DROP_BOUNCE)
                .reward(Items.IRON_PICKAXE)
                .timed(4_000L)
                .build());

        return new BuildData(
                new ResearchGroupDefinition[] { root, metallurgy, farming, logistics, energy, alchemy },
                definitions.toArray(ResearchDefinition[]::new)
        );
    }

    public static BuildData createDebugRecipes() {
        ResearchGroupDefinition root = group("root", "Root", 0xFFD0D5DD, 0xFFF4F6F8);
        ResearchGroupDefinition metallurgy = group("metallurgy", "Metallurgy", 0xFFE29A47, 0xFFF0C17C);
        ResearchGroupDefinition farming = group("farming", "Farming", 0xFF54B36B, 0xFF87D99C);
        ResearchGroupDefinition logistics = group("logistics", "Logistics", 0xFF4C90E8, 0xFF81B7FF);
        ResearchGroupDefinition energy = group("energy", "Energy", 0xFFF0C94A, 0xFFFFE08A);
        ResearchGroupDefinition alchemy = group("alchemy", "Alchemy", 0xFF9C6BE8, 0xFFC7A8FF);

        List<ResearchDefinition> definitions = new ObjectArrayList<>(40);

        definitions.add(research("primitive_tools", "Primitive Tools Its very big research name because i need test", root)
                .description("Basic stone-age survival knowledge and the first usable hand tools.")
                .icon(Items.BEDROCK.asItem())
                .reward(Items.STONE_PICKAXE)
                .instant()
                .build());

        definitions.add(research("stone_working", "Stone Working", root)
                .description("Learn to shape rough stone into useful parts and foundations.")
                .required("primitive_tools")
                .condition(Items.COBBLESTONE)
                .reward(Items.STONE_AXE)
                .instant()
                .build());

        definitions.add(research("fire_making", "Fire Making", root)
                .description("Controlled fire opens the way to cooking, heat and chemistry.")
                .required("primitive_tools")
                .condition(Items.FLINT)
                .stage("my_stage")
                .reward(Items.FLINT_AND_STEEL)
                .timed(3600000)
                .build());

        definitions.add(research("observation", "Observation", root)
                .description("Watching nature carefully reveals patterns, seasons and opportunities.")
                .required("primitive_tools")
                .condition(Items.SPYGLASS)
                .reward(Items.MAP)
                .instant()
                .build());

        definitions.add(research("workbench", "Workbench", root)
                .description("A dedicated workplace lets you craft more reliable tools and components.")
                .required("primitive_tools")
                .condition(Items.CRAFTING_TABLE)
                .reward(Items.CRAFTING_TABLE)
                .instant()
                .build());

        definitions.add(research("storage_basics", "Storage Basics", root)
                .description("You begin organizing goods instead of keeping everything on hand.")
                .required("primitive_tools")
                .condition(Items.CHEST)
                .reward(Items.BARREL)
                .instant()
                .build());

        definitions.add(research("copper_processing", "Copper Processing", metallurgy)
                .description("Soft metals can be smelted and shaped into early machine parts.")
                .required("stone_working", "fire_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.RAW_COPPER)
                .reward(Items.COPPER_INGOT)
                .timed(18_000L)
                .build());

        definitions.add(research("tin_processing", "Tin Processing", metallurgy)
                .description("Tin is fragile on its own but invaluable in useful alloys.")
                .required("stone_working", "fire_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.IRON_NUGGET)
                .reward(Items.IRON_NUGGET)
                .timed(18_000L)
                .build());

        definitions.add(research("bronze_alloy", "Bronze Alloy", metallurgy)
                .description("Mixing metals together creates something stronger than each part alone.")
                .required("copper_processing", "tin_processing")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.BRICK)
                .reward(Items.LIGHTNING_ROD)
                .table()
                .build());

        definitions.add(research("charcoal_smelting", "Charcoal Smelting", metallurgy)
                .description("Hotter and cleaner fuel makes primitive furnaces far more reliable.")
                .required("fire_making")
                .condition(Items.CHARCOAL)
                .reward(Items.CHARCOAL)
                .instant()
                .build());

        definitions.add(research("bloomery", "Bloomery", metallurgy)
                .description("A true furnace structure lets you extract better metal blooms.")
                .required("bronze_alloy", "charcoal_smelting")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.FURNACE)
                .reward(Items.BLAST_FURNACE)
                .timed(28_000L)
                .build());

        definitions.add(research("iron_working", "Iron Working", metallurgy)
                .description("Iron tools and components unlock sturdier industrial progress.")
                .required("bloomery")
                .condition(Items.RAW_IRON)
                .reward(Items.IRON_INGOT)
                .timed(22_000L)
                .build());

        definitions.add(research("press", "Mechanical Press", metallurgy)
                .description("Pressure lets you form plates and precise structural components.")
                .required("bronze_alloy", "workbench")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.PISTON)
                .reward(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .timed(30_000L)
                .build());

        definitions.add(research("soil_preparation", "Soil Preparation", farming)
                .description("Prepared ground makes farming predictable instead of hopeful.")
                .required("workbench")
                .condition(Items.WOODEN_HOE)
                .reward(Items.STONE_HOE)
                .instant()
                .build());

        definitions.add(research("seed_selection", "Seed Selection", farming)
                .description("Choosing better seed stock steadily improves the harvest.")
                .required("observation", "soil_preparation")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.WHEAT_SEEDS)
                .reward(Items.BEETROOT_SEEDS)
                .instant()
                .build());

        definitions.add(research("irrigation", "Irrigation", farming)
                .description("Moving water where it is needed stabilizes crop growth.")
                .required("storage_basics", "soil_preparation")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.WATER_BUCKET)
                .reward(Items.BUCKET)
                .timed(16_000L)
                .build());

        definitions.add(research("composting", "Composting", farming)
                .description("Waste can become fertile material instead of being thrown away.")
                .required("soil_preparation", "fire_making")
                .condition(Items.ROTTEN_FLESH)
                .reward(Items.BONE_MEAL)
                .instant()
                .build());

        definitions.add(research("greenhouse", "Greenhouse", farming)
                .description("Protected growing space makes production less dependent on weather.")
                .required("irrigation", "observation")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.GLASS)
                .reward(Items.GLASS_PANE)
                .timed(26_000L)
                .build());

        definitions.add(research("animal_husbandry", "Animal Husbandry", farming)
                .description("Managing livestock gives a steady source of food and materials.")
                .required("seed_selection", "storage_basics")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.WHEAT)
                .reward(Items.LEAD)
                .table()
                .build());

        definitions.add(research("rope_making", "Rope Making", logistics)
                .description("Binding materials together makes transport and lifting possible.")
                .required("workbench")
                .condition(Items.STRING)
                .reward(Items.LEAD)
                .instant()
                .build());

        definitions.add(research("cart_frames", "Cart Frames", logistics)
                .description("The first cargo frames improve over carrying everything by hand.")
                .required("rope_making", "stone_working")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.MINECART)
                .reward(Items.MINECART)
                .timed(16_000L)
                .build());

        definitions.add(research("warehouse", "Warehouse", logistics)
                .description("Centralized storage becomes the heart of a growing settlement.")
                .required("storage_basics", "rope_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.CHEST)
                .reward(Items.HOPPER)
                .table()
                .build());

        definitions.add(research("pack_animals", "Pack Animals", logistics)
                .description("Animals can move heavier loads further than a single worker.")
                .required("animal_husbandry", "rope_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.SADDLE)
                .reward(Items.CARROT_ON_A_STICK)
                .instant()
                .build());

        definitions.add(research("logistics_assemblies", "Logistics Assemblies", logistics)
                .description("Standardized connectors and frames speed up expansion.")
                .required("cart_frames", "warehouse")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.CHAIN)
                .reward(Items.RAIL)
                .timed(24_000L)
                .build());

        definitions.add(research("roads", "Roads", logistics)
                .description("Reliable roads cut travel time and improve heavy transport.")
                .required("stone_working", "logistics_assemblies")
                .condition(Items.GRAVEL)
                .reward(Items.PACKED_MUD)
                .timed(22_000L)
                .build());

        definitions.add(research("shipping_crates", "Shipping Crates", logistics)
                .description("Purpose-built shipping containers make trade much cleaner.")
                .required("warehouse", "press")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.BARREL)
                .reward(Items.CHEST_MINECART)
                .instant()
                .build());

        definitions.add(research("crusher", "Crusher", energy)
                .description("Crushing ore and stone prepares bulk materials for automation.")
                .required("press", "stone_working")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.IRON_PICKAXE)
                .reward(Items.IRON_BLOCK)
                .timed(28_000L)
                .build());

        definitions.add(research("waterwheel", "Waterwheel", energy)
                .description("Flowing water becomes the first continuous power source.")
                .required("irrigation", "logistics_assemblies")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.WATER_BUCKET)
                .reward(Items.CLOCK)
                .timed(24_000L)
                .build());

        definitions.add(research("windmill", "Windmill", energy)
                .description("Wind can drive simple machinery when terrain allows it.")
                .required("rope_making", "observation")
                .condition(Items.WHITE_WOOL)
                .reward(Items.WHITE_BANNER)
                .timed(24_000L)
                .build());

        definitions.add(research("steam_power", "Steam Power", energy)
                .description("Stored heat becomes a powerful step toward true industry.")
                .required("iron_working", "waterwheel")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.CAULDRON)
                .reward(Items.POWERED_RAIL)
                .timed(36_000L)
                .build());

        definitions.add(research("power_transmission", "Power Transmission", energy)
                .description("Power is most useful once it can be routed to distant machines.")
                .required("steam_power", "rope_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.COPPER_INGOT)
                .reward(Items.REDSTONE)
                .timed(20_000L)
                .build());

        definitions.add(research("powered_machines", "Powered Machines", energy)
                .description("Machine networks replace repetitive manual labor.")
                .required("power_transmission", "crusher")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.REDSTONE_TORCH)
                .reward(Items.OBSERVER)
                .table()
                .build());

        definitions.add(research("herbal_extracts", "Herbal Extracts", alchemy)
                .description("Plants yield more than food if prepared with care.")
                .required("seed_selection", "fire_making")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.POTION)
                .reward(Items.GLASS_BOTTLE)
                .instant()
                .build());

        definitions.add(research("mineral_powders", "Mineral Powders", alchemy)
                .description("Ground minerals react differently than raw chunks.")
                .required("crusher", "observation")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.GUNPOWDER)
                .reward(Items.GRAY_DYE)
                .timed(18_000L)
                .build());

        definitions.add(research("saltpeter", "Saltpeter", alchemy)
                .description("Crystalline deposits reveal a path toward energetic mixtures.")
                .required("composting", "mineral_powders")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.BONE_MEAL)
                .reward(Items.SUGAR)
                .timed(18_000L)
                .build());

        definitions.add(research("acidic_solutions", "Acidic Solutions", alchemy)
                .description("Reactive liquids let you separate and clean difficult materials.")
                .required("herbal_extracts", "copper_processing")
                .visibility(ResearchVisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .condition(Items.HONEY_BOTTLE)
                .reward(Items.SLIME_BALL)
                .table()
                .build());

        definitions.add(research("lamp_oil", "Lamp Oil", alchemy)
                .description("Slow-burning liquids improve underground and nighttime work.")
                .required("composting", "storage_basics")
                .condition(Items.GLOW_BERRIES)
                .reward(Items.LANTERN)
                .instant()
                .build());

        definitions.add(research("explosives", "Explosives", alchemy)
                .description("Volatile mixtures can break ground or cause catastrophic mistakes.")
                .required("saltpeter", "mineral_powders")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.GUNPOWDER)
                .reward(Items.TNT)
                .table()
                .build());

        definitions.add(research("alchemical_salts", "Alchemical Salts", alchemy)
                .description("Refined salts become the basis for advanced chemical reactions.")
                .required("acidic_solutions", "fire_making")
                .condition(Items.QUARTZ)
                .reward(Items.AMETHYST_SHARD)
                .timed(22_000L)
                .build());

        definitions.add(research("battery_bank", "Battery Bank", alchemy)
                .description("Stored charge bridges the gap between chemistry and machinery.")
                .required("alchemical_salts", "steam_power")
                .visibility(ResearchVisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .condition(Items.COPPER_BLOCK)
                .reward(Items.LIGHTNING_ROD)
                .timed(32_000L)
                .build());

        return new BuildData(
                new ResearchGroupDefinition[] { root, metallurgy, farming, logistics, energy, alchemy },
                definitions.toArray(ResearchDefinition[]::new)
        );
    }

    private static ResearchGroupDefinition group(String id, String title, int primaryColor, int secondaryColor) {
        return ResearchGroupDefinition.builder(id)
                .title(title)
                .primaryColor(primaryColor)
                .secondaryColor(secondaryColor)
                .build();
    }

    private static ResearchDefinition.Builder research(String key, String title, ResearchGroupDefinition group) {
        return ResearchDefinition.register(key)
                .title(title)
                .group(group);
    }

    public record BuildData(ResearchGroupDefinition[] groups, ResearchDefinition[] definitions) { }
}
