package dev.boomys.liminaldoors.client;

import dev.boomys.liminaldoors.entity.StalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Reuses the vanilla humanoid model with the pale skeleton texture, so the mod
 * ships no copied artwork of its own.
 */
public class StalkerRenderer
		extends MobEntityRenderer<StalkerEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");

	public StalkerRenderer(EntityRendererFactory.Context context) {
		super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.4F);
	}

	@Override
	public BipedEntityRenderState createRenderState() {
		return new BipedEntityRenderState();
	}

	@Override
	public Identifier getTexture(BipedEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	protected void scale(BipedEntityRenderState state, MatrixStack matrices) {
		matrices.scale(1.12F, 1.22F, 1.12F);
	}
}
