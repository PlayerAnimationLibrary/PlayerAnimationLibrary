/*
 * MIT License
 *
 * Copyright (c) 2024 GeckoLib
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranim.animation.keyframe.event.builtin;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.util.ClientUtil;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.event.CustomKeyFrameEvents;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.event.EventResult;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Built-in helper for a {@link CustomKeyFrameEvents.CustomKeyFrameHandler CustomKeyFrameHandler} that automatically plays the sound defined in the keyframe data
 * <p>
 * The expected keyframe data format is one of the below:
 * <pre>{@code
 * namespace:soundid
 * namespace:soundid|volume|pitch
 * }</pre>
 */
public class AutoPlayingSoundKeyframeHandler implements CustomKeyFrameEvents.CustomKeyFrameHandler<SoundKeyframeData> {
    @Override
    public EventResult handle(float animationTick, AnimationController controller, SoundKeyframeData keyFrameData, AnimationData animationData) {
        Vec3 position = controller instanceof PlayerAnimationController player ? player.getAvatar().position() : null;
        if (position == null) return EventResult.PASS;

        String[] segments = keyFrameData.getSound().split("\\|");
        Identifier rl = Identifier.tryParse(segments[0]);
        if (rl == null) return EventResult.PASS;

        Optional<Holder.Reference<SoundEvent>> soundEvent = BuiltInRegistries.SOUND_EVENT.get(rl);
        if (soundEvent.isEmpty()) return EventResult.PASS;

        float volume = segments.length > 1 ? Float.parseFloat(segments[1]) : 1;
        float pitch = segments.length > 2 ? Float.parseFloat(segments[2]) : 1;
        ClientUtil.getLevel().playSound(null, position.x, position.y, position.z, soundEvent.get(), SoundSource.PLAYERS, volume, pitch);
        return EventResult.SUCCESS;
    }
}
