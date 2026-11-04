package dev.boomys.liminaldoors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Procedurally builds one cramped, empty "liminal" room: pale walls, a flat
 * ceiling, a few half-hearted lights and a far door that leads to the next room.
 */
public final class RoomBuilder {
	private static final Block[] WALL_BLOCKS = {
			Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.LIGHT_GRAY_CONCRETE,
			Blocks.WHITE_TERRACOTTA, Blocks.QUARTZ_BRICKS
	};

	private static final Block[] FLOOR_BLOCKS = {
			Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.POLISHED_DIORITE,
			Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ
	};

	public record Room(BlockPos spawn, BlockPos exitDoor) {
	}

	public static Room build(ServerWorld world, BlockPos origin, int index, Random random) {
		int width = 9 + random.nextInt(9);
		int depth = 9 + random.nextInt(9);
		int height = 5 + random.nextInt(3);

		int x0 = origin.getX();
		int y0 = origin.getY();
		int z0 = origin.getZ();

		BlockState wall = WALL_BLOCKS[random.nextInt(WALL_BLOCKS.length)].getDefaultState();
		BlockState floor = FLOOR_BLOCKS[random.nextInt(FLOOR_BLOCKS.length)].getDefaultState();

		// Wipe the area so rooms never intersect terrain or each other.
		for (int x = x0 - 2; x < x0 + width + 2; x++) {
			for (int z = z0 - 2; z < z0 + depth + 2; z++) {
				for (int y = y0 - 4; y < y0 + height + 4; y++) {
					world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
				}
			}
		}

		// Floor and ceiling.
		for (int x = x0; x < x0 + width; x++) {
			for (int z = z0; z < z0 + depth; z++) {
				world.setBlockState(new BlockPos(x, y0 - 1, z), floor, 2);
				world.setBlockState(new BlockPos(x, y0 + height, z), floor, 2);
			}
		}

		// Walls.
		for (int y = y0; y < y0 + height; y++) {
			for (int x = x0; x < x0 + width; x++) {
				for (int z = z0; z < z0 + depth; z++) {
					boolean edge = x == x0 || x == x0 + width - 1 || z == z0 || z == z0 + depth - 1;

					if (edge) {
						world.setBlockState(new BlockPos(x, y, z), wall, 2);
					}
				}
			}
		}

		// A handful of pillars so the room has blind spots.
		int pillars = random.nextInt(3);

		for (int i = 0; i < pillars; i++) {
			int px = x0 + 3 + random.nextInt(Math.max(1, width - 6));
			int pz = z0 + 4 + random.nextInt(Math.max(1, depth - 7));

			for (int y = y0; y < y0 + height; y++) {
				world.setBlockState(new BlockPos(px, y, pz), wall, 2);
			}
		}

		// Lights: sometimes the room is almost completely dark.
		int lights = random.nextInt(4);

		for (int i = 0; i < lights; i++) {
			int lx = x0 + 2 + random.nextInt(Math.max(1, width - 4));
			int lz = z0 + 2 + random.nextInt(Math.max(1, depth - 4));
			world.setBlockState(new BlockPos(lx, y0 + height - 1, lz), Blocks.SEA_LANTERN.getDefaultState(), 2);
		}

		// The door you came in through. It does nothing.
		int entryX = x0 + width / 2;
		placeDoor(world, new BlockPos(entryX, y0, z0));

		// The door that leads onwards.
		int exitX = x0 + 2 + random.nextInt(Math.max(1, width - 5));
		BlockPos exitDoor = new BlockPos(exitX, y0, z0 + depth - 1);
		placeDoor(world, exitDoor);

		BlockPos spawn = new BlockPos(x0 + width / 2, y0, z0 + 2);
		return new Room(spawn, exitDoor);
	}

	private static void placeDoor(ServerWorld world, BlockPos pos) {
		BlockState lower = Blocks.OAK_DOOR.getDefaultState()
				.with(DoorBlock.FACING, Direction.NORTH)
				.with(DoorBlock.HALF, DoubleBlockHalf.LOWER);

		BlockState upper = Blocks.OAK_DOOR.getDefaultState()
				.with(DoorBlock.FACING, Direction.NORTH)
				.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);

		world.setBlockState(pos, lower, 3);
		world.setBlockState(pos.up(), upper, 3);
	}

	private RoomBuilder() {
	}
}
