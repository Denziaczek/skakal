package dev.skijump.registry;

import dev.skijump.SkiJumpMod;
import dev.skijump.hill.HillPreset;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModGroups {
    public static ItemGroup MAIN;

    public static void init() {
        MAIN = Registry.register(Registries.ITEM_GROUP, SkiJumpMod.id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.SKI_COMPOSITE))
                .displayName(Text.translatable("itemGroup.skijump.main"))
                .entries((ctx, entries) -> {
                    entries.add(ModItems.HILL_WAND);
                    for (HillPreset p : HillPreset.ALL) {
                        ItemStack s = new ItemStack(ModItems.HILL_BLUEPRINT);
                        s.getOrCreateNbt().putString("preset", p.id);
                        entries.add(s);
                    }
                    for (Block b : ModBlocks.ALL) entries.add(b);
                    entries.add(ModItems.SKI_WOOD);
                    entries.add(ModItems.SKI_IRON);
                    entries.add(ModItems.SKI_COMPOSITE);
                    entries.add(ModItems.SUIT_STANDARD);
                    entries.add(ModItems.SUIT_PRO);
                    entries.add(ModItems.HELMET);
                    entries.add(ModItems.MEDAL_GOLD);
                    entries.add(ModItems.MEDAL_SILVER);
                    entries.add(ModItems.MEDAL_BRONZE);
                    entries.add(ModItems.skoczekSpawnEgg);
                })
                .build());
    }

    private ModGroups() { }
}
