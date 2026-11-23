package dev.skijump.item;

import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.physics.HillGeometry;
import dev.skijump.registry.ModBlocks;
import dev.skijump.block.StartGateBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Różdżka skoczni - zamienia skocznię zbudowaną przez gracza w pełnoprawną (z fizyką, zawodami, rekordami).
 * Trzy kliknięcia: (1) szczyt rozbiegu (belka), (2) krawędź progu, (3) punkt K na zeskoku.
 * Kucanie + prawy klik w powietrze resetuje, prawy klik w powietrze pokazuje tor rozbiegu najbliższej skoczni.
 * Prawy klik na belce startowej zmienia numer belki (kucanie = niżej).
 */
public class HillWandItem extends Item {
    public HillWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;
        PlayerEntity player = ctx.getPlayer();
        if (!(player instanceof ServerPlayerEntity sp) || !(world instanceof ServerWorld sw)) return ActionResult.PASS;
        BlockPos pos = ctx.getBlockPos();
        ItemStack stack = ctx.getStack();

        // zmiana belki startowej
        if (sw.getBlockState(pos).isOf(ModBlocks.START_GATE)) {
            HillRecord h = HillState.get(sw.getServer()).byGate(sw, pos);
            if (h != null) {
                int g = h.gate + (sp.isSneaking() ? -1 : 1);
                h.gate = Math.max(1, Math.min(HillRecord.MAX_GATE, g));
                HillState.get(sw.getServer()).markDirty();
                sp.sendMessage(Text.translatable("skijump.msg.gate_set", String.valueOf(h.gate), String.valueOf(HillRecord.MAX_GATE)), true);
                return ActionResult.SUCCESS;
            }
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains("gate")) {
            nbt.putLong("gate", pos.asLong());
            sp.sendMessage(Text.translatable("skijump.msg.wand_1"), true);
        } else if (!nbt.contains("edge")) {
            nbt.putLong("edge", pos.asLong());
            sp.sendMessage(Text.translatable("skijump.msg.wand_2"), true);
        } else {
            BlockPos gate = BlockPos.fromLong(nbt.getLong("gate"));
            BlockPos edge = BlockPos.fromLong(nbt.getLong("edge"));
            nbt.remove("gate");
            nbt.remove("edge");
            create(sw, sp, gate, edge, pos);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (player.isSneaking()) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.remove("gate");
            nbt.remove("edge");
            player.sendMessage(Text.translatable("skijump.msg.wand_reset"), true);
        } else if (world instanceof ServerWorld sw && player instanceof ServerPlayerEntity sp) {
            HillRecord h = HillState.get(sw.getServer()).nearest(sw, sp.getBlockPos(), 120);
            if (h != null) {
                showGuide(sw, h);
                sp.sendMessage(Text.translatable("skijump.msg.wand_guide", h.name), true);
            } else {
                sp.sendMessage(Text.translatable("skijump.msg.no_hill_near"), true);
            }
        }
        return TypedActionResult.success(stack);
    }

    /** Podgląd obliczonego toru rozbiegu (cząsteczki). */
    public static void showGuide(ServerWorld w, HillRecord h) {
        HillGeometry g = h.geometry();
        for (double x = -g.inrunD; x <= 0; x += 2.0) {
            Vec3d p = h.toWorld(x, g.inrunY(x), 0);
            w.spawnParticles(ParticleTypes.END_ROD, p.x, p.y + 0.3, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void create(ServerWorld w, ServerPlayerEntity p, BlockPos gate, BlockPos edge, BlockPos kPos) {
        int dx = edge.getX() - gate.getX(), dz = edge.getZ() - gate.getZ();
        Direction f = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        double d = (edge.getX() - gate.getX()) * f.getOffsetX() + (edge.getZ() - gate.getZ()) * f.getOffsetZ();
        double h = (gate.getY() + 1) - (edge.getY() + 1);
        double kx = (kPos.getX() - edge.getX()) * f.getOffsetX() + (kPos.getZ() - edge.getZ()) * f.getOffsetZ();
        double ky = (kPos.getY() + 1) - (edge.getY() + 1);
        if (d < 12 || h < 4) {
            p.sendMessage(Text.translatable("skijump.msg.wand_bad_inrun"), false);
            return;
        }
        if (kx < 15) {
            p.sendMessage(Text.translatable("skijump.msg.wand_bad_k"), false);
            return;
        }
        double k = Math.round(Math.hypot(kx, ky));
        HillRecord rec = new HillRecord();
        rec.presetId = "custom";
        rec.name = p.getName().getString() + " K" + (int) k;
        rec.dim = w.getRegistryKey().getValue().toString();
        rec.edge = new BlockPos(edge.getX(), edge.getY(), edge.getZ());
        rec.edgeY = edge.getY() + 1;
        rec.facing = f;
        rec.scale = 1.0;
        rec.k = k;
        rec.hs = Math.round(k * 1.12);
        rec.customD = d;
        rec.customH = h;
        rec.rank = 0;
        rec.built = true;
        BlockPos gp = gate.up();
        if (w.getBlockState(gp).isAir()) {
            w.setBlockState(gp, ModBlocks.START_GATE.getDefaultState().with(StartGateBlock.FACING, f));
        } else if (!w.getBlockState(gp).isOf(ModBlocks.START_GATE)) {
            gp = gate;
        }
        rec.gatePos = gp;
        HillState.get(w.getServer()).add(rec);
        showGuide(w, rec);
        p.sendMessage(Text.translatable("skijump.msg.wand_created", rec.name, String.valueOf((int) rec.k),
                String.valueOf((int) rec.hs), rec.id), false);
    }
}
