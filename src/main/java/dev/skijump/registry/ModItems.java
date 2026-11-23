package dev.skijump.registry;

import dev.skijump.SkiJumpMod;
import dev.skijump.item.GearItem;
import dev.skijump.item.GearMaterial;
import dev.skijump.item.HillBlueprintItem;
import dev.skijump.item.HillWandItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final Item SKI_WOOD = reg("ski_wood", new GearItem(GearMaterial.SKI_WOOD, ArmorItem.Type.BOOTS,
            0.25, 0.30, 0.15, 0, 0, 0, 0, new FabricItemSettings()));
    public static final Item SKI_IRON = reg("ski_iron", new GearItem(GearMaterial.SKI_IRON, ArmorItem.Type.BOOTS,
            0.50, 0.50, 0.35, 0, 0, 0, 0, new FabricItemSettings()));
    public static final Item SKI_COMPOSITE = reg("ski_composite", new GearItem(GearMaterial.SKI_COMPOSITE, ArmorItem.Type.BOOTS,
            0.80, 0.70, 0.60, 0, 0, 0, 0, new FabricItemSettings()));
    public static final Item SUIT_STANDARD = reg("suit_standard", new GearItem(GearMaterial.SUIT_STANDARD, ArmorItem.Type.CHESTPLATE,
            0, 0, 0, 0.30, 0.25, 0.35, 0, new FabricItemSettings()));
    public static final Item SUIT_PRO = reg("suit_pro", new GearItem(GearMaterial.SUIT_PRO, ArmorItem.Type.CHESTPLATE,
            0, 0, 0, 0.65, 0.55, 0.60, 0, new FabricItemSettings()));
    public static final Item HELMET = reg("helmet", new GearItem(GearMaterial.HELMET, ArmorItem.Type.HELMET,
            0, 0, 0, 0, 0, 0, 0.5, new FabricItemSettings()));

    public static final Item HILL_WAND = reg("hill_wand", new HillWandItem(new FabricItemSettings().maxCount(1)));
    public static final Item HILL_BLUEPRINT = reg("hill_blueprint", new HillBlueprintItem(new FabricItemSettings().maxCount(1)));

    public static final Item MEDAL_GOLD = reg("medal_gold", new Item(new FabricItemSettings()));
    public static final Item MEDAL_SILVER = reg("medal_silver", new Item(new FabricItemSettings()));
    public static final Item MEDAL_BRONZE = reg("medal_bronze", new Item(new FabricItemSettings()));

    public static Item skoczekSpawnEgg;

    private static Item reg(String name, Item item) {
        return Registry.register(Registries.ITEM, SkiJumpMod.id(name), item);
    }

    /** Jajko tworzymy po zarejestrowaniu encji. */
    public static void initSpawnEgg() {
        skoczekSpawnEgg = reg("skoczek_spawn_egg",
                new SpawnEggItem(ModEntities.SKOCZEK, 0xE8F0FF, 0xD02030, new FabricItemSettings()));
    }

    public static void init() { }

    private ModItems() { }
}
