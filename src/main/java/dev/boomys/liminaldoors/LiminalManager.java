package dev.boomys.liminaldoors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import dev.boomys.liminaldoors.entity.StalkerEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * Keeps track of who is currently inside the liminal rooms, builds the next
 * room when a door is used, and cleans up when a run ends.
 */
public final class LiminalManager {
	public static final int ROOMS_PER_RUN = 7;

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final AtomicInteger AREA_COUNTER = new AtomicInteger();

	public static final class Session {
		private final UUID playerId;
		private final BlockPos areaOrigin;
		private final RegistryKey<World> returnDimension;
		private final BlockPos returnPos;
		private final float returnYaw;
		private final float returnPitch;
		private int roomIndex;
		private BlockPos exitDoor;

		private Session(ServerPlayerEntity player, BlockPos areaOrigin) {
			this.playerId = player.getUuid();
			this.areaOrigin = areaOrigin;
			this.returnDimension = player.getWorld().getRegistryKey();
			this.returnPos = player.getBlockPos();
			this.returnYaw = player.getYaw();
			this.returnPitch = player.getPitch();
		}
	}

	public static void register() {
		UseBlockCallback.EVENT.register(LiminalManager::onUseBlock);
		ServerTickEvents.END_SERVER_TICK.register(LiminalManager::onServerTick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity player) {
				dropSession(player, false);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();

			if (SESSIONS.containsKey(player.getUuid())) {
				player.sendMessage(Text.literal("Hâlâ liminal odalardasın. Devam etmek için ilerideki kapıyı kullan, çıkmak için /liminal leave yaz.")
						.formatted(Formatting.DARK_GRAY), false);
			}
		});
	}

	public static boolean isInLiminal(net.minecraft.entity.player.PlayerEntity player) {
		return SESSIONS.containsKey(player.getUuid());
	}

	private static ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player, World world,
			Hand hand, net.minecraft.util.hit.BlockHitResult hitResult) {
		if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();

		if (!(world.getBlockState(pos).getBlock() instanceof DoorBlock)) {
			return ActionResult.PASS;
		}

		Session session = SESSIONS.get(serverPlayer.getUuid());

		if (session == null) {
			// Sneak + right-click any door to step through.
			if (serverPlayer.isSneaking()) {
				enter(serverPlayer);
				return ActionResult.SUCCESS;
			}

			return ActionResult.PASS;
		}

		if (pos.equals(session.exitDoor) || pos.equals(session.exitDoor.up())) {
			advance(serverPlayer, session);
		} else {
			serverPlayer.sendMessage(Text.literal("O kapı kilitli. Arkandaki kapı kapanmıştı.")
					.formatted(Formatting.DARK_GRAY), true);
			world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 0.8F, 0.6F);
		}

		return ActionResult.SUCCESS;
	}

	public static void enter(ServerPlayerEntity player) {
		Session session = SESSIONS.get(player.getUuid());

		if (session == null) {
			int area = AREA_COUNTER.incrementAndGet();
			BlockPos origin = new BlockPos(20_000 + area * 1024, 180, 20_000 + area * 1024);
			session = new Session(player, origin);
			SESSIONS.put(player.getUuid(), session);
		}

		session.roomIndex = 0;
		buildAndTeleport(player, session);
	}

	public static void leave(ServerPlayerEntity player) {
		if (!SESSIONS.containsKey(player.getUuid())) {
			// Safety valve: a server restart forgets the run, so rescue anyone left in a sealed room.
			MinecraftServer server = player.getServer();

			if (server != null) {
				ServerWorld overworld = server.getOverworld();
				BlockPos spawn = overworld.getSpawnPos();
				player.teleport(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
						Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
				player.sendMessage(Text.literal("Liminal odalarda değildin; dünya doğuş noktasına ışınlandın.")
						.formatted(Formatting.GRAY), false);
			}

			return;
		}

		dropSession(player, true);
	}

	private static void advance(ServerPlayerEntity player, Session session) {
		if (session.roomIndex + 1 >= ROOMS_PER_RUN) {
			player.sendMessage(Text.literal("Son kapı arkanda kapandı. Dışarıdasın.")
					.formatted(Formatting.GRAY), false);
			dropSession(player, true);
			return;
		}

		session.roomIndex++;
		buildAndTeleport(player, session);
	}

	private static void buildAndTeleport(ServerPlayerEntity player, Session session) {
		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos origin = session.areaOrigin.add(0, 0, session.roomIndex * 40);
		RoomBuilder.Room room = RoomBuilder.build(world, origin, session.roomIndex, world.getRandom());
		session.exitDoor = room.exitDoor();

		BlockPos spawn = room.spawn();
		player.teleport(world, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
				Set.<PositionFlag>of(), 0.0F, 0.0F, false);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false));

		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.2F, 0.6F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 0.6F, 0.5F);

		player.sendMessage(Text.literal("Oda " + (session.roomIndex + 1) + " / " + ROOMS_PER_RUN)
				.formatted(Formatting.DARK_GRAY), true);

		// From the second room onwards something is waiting near the far door.
		if (session.roomIndex >= 1 && session.roomIndex % 2 == 1) {
			BlockPos spawnPos = room.exitDoor().add(0, 0, -1);
			spawnStalker(world, spawnPos);
		}
	}

	private static void spawnStalker(ServerWorld world, BlockPos pos) {
		StalkerEntity stalker = ModEntities.STALKER.create(world, SpawnReason.EVENT);

		if (stalker == null) {
			return;
		}

		stalker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180.0F, 0.0F);
		stalker.setPersistent();
		world.spawnEntity(stalker);
	}

	private static void onServerTick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			return;
		}

		for (Session session : Map.copyOf(SESSIONS).values()) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.playerId);

			if (player == null) {
				continue;
			}

			if (player.isRemoved() || !player.isAlive()) {
				dropSession(player, false);
				continue;
			}

			if (player.getWorld().getTime() % 260L == 0L) {
				World world = player.getWorld();
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_WARDEN_AMBIENT, SoundCategory.AMBIENT, 0.9F, 0.5F);
			}

			if (player.getWorld().getTime() % 480L == 120L) {
				World world = player.getWorld();
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_ENDERMAN_AMBIENT, SoundCategory.AMBIENT, 0.5F, 0.4F);
			}
		}
	}

	private static void dropSession(ServerPlayerEntity player, boolean teleportBack) {
		Session session = SESSIONS.remove(player.getUuid());

		if (session == null) {
			return;
		}

		World world = player.getWorld();
		Box area = new Box(session.areaOrigin).expand(512.0D, 128.0D, 512.0D);

		for (StalkerEntity stalker : world.getEntitiesByClass(StalkerEntity.class, area, Entity::isAlive)) {
			stalker.discard();
		}

		if (!teleportBack) {
			return;
		}

		MinecraftServer server = player.getServer();
		ServerWorld back = server == null ? null : server.getWorld(session.returnDimension);

		if (back != null) {
			player.teleport(back, session.returnPos.getX() + 0.5D, session.returnPos.getY(),
					session.returnPos.getZ() + 0.5D, Set.<PositionFlag>of(),
					session.returnYaw, session.returnPitch, false);
			back.playSound(null, session.returnPos, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
					SoundCategory.BLOCKS, 1.0F, 0.8F);
		}
	}

	public static int roomIndex(ServerPlayerEntity player) {
		Session session = SESSIONS.get(player.getUuid());
		return session == null ? -1 : session.roomIndex;
	}

	public static boolean isInLiminal(ServerPlayerEntity player) {
		return SESSIONS.containsKey(player.getUuid());
	}

	private LiminalManager() {
	}
}
