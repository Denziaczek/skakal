package dev.skijump.client;

import dev.skijump.SkiJumpMod;
import dev.skijump.entity.JumperSeatEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

/** Gondola skoczka jest niewidzialna - renderowany jest tylko pasażer. */
public class SeatRenderer extends EntityRenderer<JumperSeatEntity> {
    public SeatRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(JumperSeatEntity entity) {
        return SkiJumpMod.id("textures/entity/seat.png");
    }
}
