package com.corpse.command;

import com.corpse.entity.CorpseEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class CorpseCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("corpse")
                        .requires(source -> source.hasPermission(2)) // operator level 2+
                        .then(Commands.literal("killall")
                                .executes(CorpseCommand::executeKillAll))
        );
    }

    private static int executeKillAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        int removed = 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof CorpseEntity) {
                entity.discard();
                removed++;
            }
        }

        int finalRemoved = removed;
        source.sendSuccess(() -> Component.translatable("command.corpse.killall", finalRemoved), true);

        return removed;
    }
}
