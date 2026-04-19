package com.zigythebird.playeranim.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnimationArgumentProvider<S> implements SuggestionProvider<S> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> suggestions = new LinkedList<>();
        for (Identifier animation : PlayerAnimResources.getAnimations().keySet()) {
            suggestions.add(animation.toString()); // TODO by names
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }
}
