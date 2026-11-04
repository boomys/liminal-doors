package dev.boomys.liminaldoors;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LiminalCommand {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				register(dispatcher));
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("liminal")
				.then(CommandManager.literal("enter").executes(context -> {
					LiminalManager.enter(context.getSource().getPlayerOrThrow());
					return 1;
				}))
				.then(CommandManager.literal("leave").executes(context -> {
					LiminalManager.leave(context.getSource().getPlayerOrThrow());
					return 1;
				}))
				.then(CommandManager.literal("status").executes(context -> {
					int room = LiminalManager.roomIndex(context.getSource().getPlayerOrThrow());
					Text message = room < 0
							? Text.literal("Liminal odalarda değilsin.").formatted(Formatting.GRAY)
							: Text.literal("Oda " + (room + 1) + " / " + LiminalManager.ROOMS_PER_RUN)
									.formatted(Formatting.GRAY);
					context.getSource().sendFeedback(() -> message, false);
					return 1;
				})));
	}

	private LiminalCommand() {
	}
}
