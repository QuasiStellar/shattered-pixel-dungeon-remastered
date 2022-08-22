/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.noosa.audio

import kotlin.jvm.Synchronized
import com.watabou.noosa.audio.Sample
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.watabou.noosa.Game
import kotlin.jvm.JvmOverloads
import com.watabou.noosa.audio.Sample.DelayedSoundEffect
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.math.max

object Sample {

    private var ids = HashMap<Any?, Sound>()

    @set:JvmName("enable")
    var isEnabled = true

    @set:JvmName("volume")
    var globalVolume = 1f

    @Synchronized
    @JvmStatic fun reset() {
        ids.values.forEach(Sound::dispose)
        ids.clear()
        delayedSFX.clear()
    }

    @JvmStatic @Synchronized
    fun pause() { ids.values.forEach(Sound::pause) }

    @JvmStatic @Synchronized
    fun resume() { ids.values.forEach(Sound::resume) }

    @JvmStatic @Synchronized
    fun load(vararg assets: String) {
        val toLoad = assets.filterNot(ids::containsKey)

        //don't make a new thread of all assets are already loaded
        if (toLoad.isEmpty()) return

        //load in a separate thread to prevent this blocking the UI
        object : Thread() {
            override fun run() {
                for (asset in toLoad) {
                    val newSound = Gdx.audio.newSound(Gdx.files.internal(asset))
                    synchronized(this@Sample) { ids[asset] = newSound }
                }
            }
        }.start()
    }

    @JvmStatic @Synchronized
    fun unload(src: Any?) { ids.remove(src)?.dispose() }

    @JvmStatic @JvmOverloads
    fun play(id: Any?, volume: Float = 1f, pitch: Float = 1f) = play(id, volume, volume, pitch)
    @JvmStatic @Synchronized
    fun play(id: Any?, leftVolume: Float, rightVolume: Float, pitch: Float): Long {
        val volume = max(leftVolume, rightVolume)
        return ids[id]
            ?.takeIf {isEnabled}
            ?.play(globalVolume * volume, pitch, rightVolume - leftVolume)
            ?: -1
    }

    private data class DelayedSoundEffect(var id: Any? = null,
                                          var delay: Float = 0f,
                                          var leftVol: Float = 0f,
                                          var rightVol: Float = 0f,
                                          var pitch: Float = 0f)

    @JvmOverloads @JvmStatic
    fun playDelayed(id: Any?, delay: Float, volume: Float = 1f, pitch: Float = 1f) = playDelayed(id, delay, volume, volume, pitch)

    @JvmStatic
    fun playDelayed(id: Any?, delay: Float, leftVolume: Float, rightVolume: Float, pitch: Float) {
        if (delay <= 0) {
            play(id, leftVolume, rightVolume, pitch)
            return
        }
        synchronized(delayedSFX) { delayedSFX.add(DelayedSoundEffect(
            id = id,
            delay = delay,
            leftVol = leftVolume,
            rightVol = rightVolume,
            pitch = pitch,
        )) }
    }

    @JvmStatic
    fun update() {
        synchronized(delayedSFX) {
            if (delayedSFX.isEmpty()) return
            for (sfx in delayedSFX.toTypedArray()) {
                sfx.delay -= Game.INSTANCE.elapsed
                if (sfx.delay <= 0) {
                    delayedSFX.remove(sfx)
                    play(sfx.id, sfx.leftVol, sfx.rightVol, sfx.pitch)
                }
            }
        }
    }

    private val delayedSFX = HashSet<DelayedSoundEffect>()
}