package dev.skijump.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.function.Supplier;

/** Materiały sprzętu skoczka. Nazwy odpowiadają teksturom w assets/minecraft/textures/models/armor/. */
public enum GearMaterial implements ArmorMaterial {
    SKI_WOOD("skijump_ski_wood", 120, 0, () -> Ingredient.ofItems(Items.OAK_PLANKS)),
    SKI_IRON("skijump_ski_iron", 260, 0, () -> Ingredient.ofItems(Items.IRON_INGOT)),
    SKI_COMPOSITE("skijump_ski_composite", 480, 0, () -> Ingredient.ofItems(Items.QUARTZ)),
    SUIT_STANDARD("skijump_suit_standard", 220, 0, () -> Ingredient.ofItems(Items.WHITE_WOOL)),
    SUIT_PRO("skijump_suit_pro", 380, 0, () -> Ingredient.ofItems(Items.PHANTOM_MEMBRANE)),
    HELMET("skijump_helmet", 240, 2, () -> Ingredient.ofItems(Items.IRON_INGOT));

    private final String name;
    private final int durability;
    private final int protection;
    private final Supplier<Ingredient> repair;

    GearMaterial(String name, int durability, int protection, Supplier<Ingredient> repair) {
        this.name = name;
        this.durability = durability;
        this.protection = protection;
        this.repair = repair;
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return durability;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return protection;
    }

    @Override
    public int getEnchantability() {
        return 8;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return 0.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}
