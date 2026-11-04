package dev.boomys.liminaldoors;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiminalDoors implements ModInitializer {
	public static final String MOD_ID = "liminaldoors";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.register();
		LiminalManager.register();
		LiminalCommand.register();
		LOGGER.info("Liminal Doors loaded. Sneak + right-click any door to step through.");
	}
}
