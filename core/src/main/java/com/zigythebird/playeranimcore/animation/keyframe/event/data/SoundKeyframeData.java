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

package com.zigythebird.playeranimcore.animation.keyframe.event.data;

import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.loading.AnimationLoader;

import java.util.Objects;

/**
 * Sound {@link Keyframe} instruction holder
 * <p>
 * The expected keyframe data format is one of the below:
 * <pre>{@code
 * namespace:soundid
 * namespace:soundid|volume|pitch
 * }</pre>
 */
public class SoundKeyframeData extends KeyFrameData {
	private final String sound;

	private final String identifier;
	private final float volume;
	private final float pitch;

	public SoundKeyframeData(Float startTick, String sound) {
		super(startTick);

		this.sound = sound;

		String[] segments = sound.split("\\|");
		this.identifier = segments[0];
		this.volume = segments.length > 1 ? AnimationLoader.parseFloat(segments[1], 1) : 1;
		this.pitch = segments.length > 2 ? AnimationLoader.parseFloat(segments[2], 1) : 1;
	}

	/**
	 * Gets the sound data given by the {@link Keyframe} instruction from the {@code animation.json}
	 */
	public String getSound() {
		return this.sound;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public float getVolume() {
		return this.volume;
	}

	public float getPitch() {
		return this.pitch;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getStartTick(), this.sound);
	}
}
