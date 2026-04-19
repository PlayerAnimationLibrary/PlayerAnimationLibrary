package com.zigythebird.playeranim.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import com.zigythebird.playeranimcore.network.LegacyAnimationBinary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings({"unchecked","unused"})
public class PlayerAnimCommands {
    public static <T> void register(CommandDispatcher<T> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register((LiteralArgumentBuilder<T>) Commands.literal("testPlayerAnimation")
                .then(Commands.argument("animationID", IdentifierArgument.id())
                        .suggests(new AnimationArgumentProvider<>())
                        .executes(PlayerAnimCommands::execute)
                )
        );
        dispatcher.register((LiteralArgumentBuilder<T>) Commands.literal("testLegacyAnimationBinary")
                .then(Commands.argument("animationID", IdentifierArgument.id())
                        .suggests(new AnimationArgumentProvider<>())
                        .then(Commands.argument("version", IntegerArgumentType.integer(1, LegacyAnimationBinary.getCurrentVersion()))
                                .executes(PlayerAnimCommands::executeLegacy)
                        )
                )
        );
        dispatcher.register((LiteralArgumentBuilder<T>) Commands.literal("testAnimationBinary")
                .then(Commands.argument("animationID", IdentifierArgument.id())
                        .suggests(new AnimationArgumentProvider<>())
                        .then(Commands.argument("version", IntegerArgumentType.integer(1, AnimationBinary.getCurrentVersion()))
                                .executes(PlayerAnimCommands::executeBinary)
                        )
                )
        );
        dispatcher.register((LiteralArgumentBuilder<T>) Commands.literal("testMannequin")
                .then(Commands.argument("animationID", IdentifierArgument.id())
                        .suggests(new AnimationArgumentProvider<>())
                        .then(Commands.argument("mannequin", UuidArgument.uuid())
                                .executes(PlayerAnimCommands::executeMannequin)
                        )
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Identifier animation = IdentifierArgument.getId(context, "animationID");
        return playAnimation(PlayerAnimResources.getAnimation(animation));
    }

    private static int executeLegacy(CommandContext<CommandSourceStack> context) {
        Animation animation = Objects.requireNonNull(PlayerAnimResources.getAnimation(IdentifierArgument.getId(context, "animationID")));
        int version = IntegerArgumentType.getInteger(context, "version");

        ByteBuf byteBuffer = Unpooled.buffer(LegacyAnimationBinary.calculateSize(animation, version));
        LegacyAnimationBinary.write(animation, byteBuffer, version);

        try {
            return playAnimation(LegacyAnimationBinary.read(byteBuffer, version));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            byteBuffer.release();
        }
    }

    private static int executeBinary(CommandContext<CommandSourceStack> context) {
        Animation animation = Objects.requireNonNull(PlayerAnimResources.getAnimation(IdentifierArgument.getId(context, "animationID")));
        int version = IntegerArgumentType.getInteger(context, "version");

        ByteBuf byteBuf = Unpooled.buffer();
        AnimationBinary.write(byteBuf, version, animation);

        return playAnimation(AnimationBinary.read(byteBuf, version));
    }

    private static int playAnimation(Animation animation) {
        AnimationController controller = (AnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
                Objects.requireNonNull(Minecraft.getInstance().player), PlayerAnimLibMod.ANIMATION_LAYER_ID
        );
        if (controller == null) return 0;
        controller.triggerAnimation(RawAnimation.begin().thenPlay(animation));
        return 1;
    }

    private static int executeMannequin(CommandContext<CommandSourceStack> context) {
        Animation animation = Objects.requireNonNull(PlayerAnimResources.getAnimation(IdentifierArgument.getId(context, "animationID")));
        Avatar avatar = (Avatar) Objects.requireNonNull(Minecraft.getInstance().level.getEntity(UuidArgument.getUuid(context, "mannequin")));

        AnimationController controller = (AnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
                avatar, PlayerAnimLibMod.ANIMATION_LAYER_ID
        );
        if (controller == null) return 0;
        controller.triggerAnimation(RawAnimation.begin().thenPlay(animation));
        return 1;
    }
}
