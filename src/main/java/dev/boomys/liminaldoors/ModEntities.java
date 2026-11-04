package dev.boomys.liminaldoors;

import dev.boomys.liminaldoors.entity.StalkerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModEntities {
	public static final RegistryKey<EntityType<?>> STALKER_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, LiminalDoors.id("stalker"));

	public static final EntityType<StalkerEntity> STALKER = EntityType.Builder
			.create(StalkerEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6F, 2.2F)
			.maxTrackingRange(10)
			.build(STALKER_KEY);

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, STALKER_KEY, STALKER);
		FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createStalkerAttributes());
	}

	private ModEntities() {
	}
}
