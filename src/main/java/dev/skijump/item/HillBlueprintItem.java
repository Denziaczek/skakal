package dev.skijump.item;

import dev.skijump.ModConfig;
import dev.skijump.career.CareerState;
import dev.skijump.hill.HillBuilder;
import dev.skijump.hill.HillPreset;
import dev.skijump.hill.HillRecord;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Plan skoczni. Creative/operator: stawia dowolny preset za darmo. Survival: tylko małe i średnie skocznie
 * (do K z configu, domyślnie 90), wymagana ranga presetu oraz surowce z ekwipunku (śnieg, lód, kamienne cegły).
 * Prawy klik na klocku stawia skocznię (próg nad klikniętym klockiem, rozbieg za graczem), Shift + prawy klik zmienia preset.
 */
public class HillBlueprintItem extends Item {
    public HillBlueprintItem(Settings settings) {
        super(settings);
    }

    public static HillPreset presetOf(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        HillPreset p = nbt != null ? HillPreset.find(nbt.getString("preset")) : null;
        return p != null ? p : HillPreset.find("training_k40");
    }

    @Override
    public Text getName(ItemStack stack) {
        HillPreset p = presetOf(stack);
        return Text.translatable("item.skijump.hill_blueprint").append(Text.literal(" - "))
                .append(Text.translatable("skijump.hill." + p.id));
    }

    private static boolean freeBuild(PlayerEntity p) {
        return p.isCreative() || (p instanceof ServerPlayerEntity sp && sp.getCommandSource().hasPermissionLevel(2));
    }

    private static boolean survivalAllowed(HillPreset p) {
        return p.k <= ModConfig.get().survivalBlueprintMaxK;
    }

    /** Koszt budowy w survivalu: {śnieg, kamienne cegły, lód}. */
    public static int[] cost(HillPreset p) {
        int k = (int) p.k;
        return new int[]{k * 3, k * 2, Math.max(8, k / 2)};
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (player.isSneaking()) {
            HillPreset cur = presetOf(stack);
            int n = HillPreset.ALL.size();
            int i = HillPreset.ALL.indexOf(cur);
            HillPreset next = cur;
            for (int step = 1; step <= n; step++) {
                HillPreset cand = HillPreset.ALL.get((i + step) % n);
                if (freeBuild(player) || survivalAllowed(cand)) { next = cand; break; }
            }
            stack.getOrCreateNbt().putString("preset", next.id);
            int[] c = cost(next);
            player.sendMessage(Text.translatable("skijump.msg.blueprint_selected", Text.translatable("skijump.hill." + next.id),
                    String.valueOf((int) next.k), String.valueOf((int) next.hs)), true);
            if (!freeBuild(player)) {
                player.sendMessage(Text.translatable("skijump.msg.bp_cost", String.valueOf(c[0]), String.valueOf(c[1]), String.valueOf(c[2])), false);
            }
        }
        return TypedActionResult.success(stack);
    }

    private static int take(PlayerInventory inv, Item item, int need) {
        int left = need;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                int t = Math.min(s.getCount(), left);
                s.decrement(t);
                left -= t;
            }
        }
        return need - left;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;
        PlayerEntity player = ctx.getPlayer();
        if (player == null || player.isSneaking()) return ActionResult.PASS;
        if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.FAIL;
        HillPreset preset = presetOf(ctx.getStack());
        boolean free = freeBuild(player);
        if (!free) {
            if (!survivalAllowed(preset)) {
                sp.sendMessage(Text.translatable("skijump.msg.bp_too_big", String.valueOf(ModConfig.get().survivalBlueprintMaxK)), true);
                return ActionResult.FAIL;
            }
            int rank = CareerState.rankOf(CareerState.get(sw.getServer()).of(sp.getUuid()).xp);
            if (ModConfig.get().enforceRanks && rank < preset.rank) {
                sp.sendMessage(Text.translatable("skijump.msg.locked", Text.translatable("skijump.rank." + CareerState.RANK_KEYS[Math.min(5, preset.rank)])), true);
                return ActionResult.FAIL;
            }
            int[] c = cost(preset);
            PlayerInventory inv = sp.getInventory();
            int hs = inv.count(Items.SNOW_BLOCK), hb = inv.count(Items.STONE_BRICKS), hi = inv.count(Items.PACKED_ICE);
            if (hs < c[0] || hb < c[1] || hi < c[2]) {
                sp.sendMessage(Text.translatable("skijump.msg.bp_missing", hs + "/" + c[0], hb + "/" + c[1], hi + "/" + c[2]), false);
                return ActionResult.FAIL;
            }
            take(inv, Items.SNOW_BLOCK, c[0]);
            take(inv, Items.STONE_BRICKS, c[1]);
            take(inv, Items.PACKED_ICE, c[2]);
        }
        BlockPos edge = ctx.getBlockPos().up();
        Direction facing = player.getHorizontalFacing();
        HillRecord rec = HillBuilder.start(sw, preset, edge, facing, ModConfig.get().hillScale, player.getUuid());
        sp.sendMessage(Text.translatable("skijump.msg.hill_started", Text.translatable("skijump.hill." + preset.id), rec.id), false);
        return ActionResult.SUCCESS;
    }
}
