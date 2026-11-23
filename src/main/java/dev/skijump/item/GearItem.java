package dev.skijump.item;

import dev.skijump.physics.GearStats;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;

/** Narty (buty), kombinezon (kurtka) i kask (hełm) - każdy wnosi swoje parametry do fizyki. */
public class GearItem extends ArmorItem {
    public final double glide, skiStab, skiLift, suitLift, suitDrag, suitStab, helmet;

    public GearItem(ArmorMaterial material, Type type, double glide, double skiStab, double skiLift,
                    double suitLift, double suitDrag, double suitStab, double helmet, Settings settings) {
        super(material, type, settings);
        this.glide = glide;
        this.skiStab = skiStab;
        this.skiLift = skiLift;
        this.suitLift = suitLift;
        this.suitDrag = suitDrag;
        this.suitStab = suitStab;
        this.helmet = helmet;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        line(tooltip, "glide", glide);
        line(tooltip, "ski_stab", skiStab);
        line(tooltip, "ski_lift", skiLift);
        line(tooltip, "suit_lift", suitLift);
        line(tooltip, "suit_drag", suitDrag);
        line(tooltip, "suit_stab", suitStab);
        line(tooltip, "helmet", helmet);
    }

    private static void line(List<Text> tooltip, String key, double v) {
        if (v > 0) tooltip.add(Text.translatable("skijump.tooltip." + key, String.valueOf(Math.round(v * 100))));
    }

    /** Zsumowany sprzęt założony przez istotę (pusty slot = brak bonusu). */
    public static GearStats read(LivingEntity e) {
        double glide = 0, skiStab = 0, skiLift = 0, suitLift = 0, suitDrag = 0, suitStab = 0, helmet = 0;
        ItemStack feet = e.getEquippedStack(EquipmentSlot.FEET);
        ItemStack chest = e.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack head = e.getEquippedStack(EquipmentSlot.HEAD);
        if (feet.getItem() instanceof GearItem g) { glide = g.glide; skiStab = g.skiStab; skiLift = g.skiLift; }
        if (chest.getItem() instanceof GearItem g) { suitLift = g.suitLift; suitDrag = g.suitDrag; suitStab = g.suitStab; }
        if (head.getItem() instanceof GearItem g) { helmet = g.helmet; }
        return new GearStats(glide, skiStab, skiLift, suitLift, suitDrag, suitStab, helmet);
    }
}
