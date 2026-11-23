package dev.skijump.registry;

import dev.skijump.SkiJumpMod;
import dev.skijump.entity.JumperSeatEntity;
import dev.skijump.entity.SkoczekEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<JumperSeatEntity> SEAT = Registry.register(Registries.ENTITY_TYPE, SkiJumpMod.id("jumper_seat"),
            FabricEntityTypeBuilder.<JumperSeatEntity>create(SpawnGroup.MISC, JumperSeatEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.4f))
                    .trackRangeBlocks(320)
                    .trackedUpdateRate(1)
                    .forceTrackedVelocityUpdates(false)
                    .fireImmune()
                    .build());

    public static final EntityType<SkoczekEntity> SKOCZEK = Registry.register(Registries.ENTITY_TYPE, SkiJumpMod.id("skoczek"),
            FabricEntityTypeBuilder.<SkoczekEntity>create(SpawnGroup.CREATURE, SkoczekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(320)
                    .trackedUpdateRate(1)
                    .forceTrackedVelocityUpdates(false)
                    .build());

    public static void init() {
        FabricDefaultAttributeRegistry.register(SKOCZEK, SkoczekEntity.createAttributes());
    }

    private ModEntities() { }
}
