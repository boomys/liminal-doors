package dev.boomys.liminaldoors.client;

import dev.boomys.liminaldoors.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class LiminalDoorsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);
	}
}
