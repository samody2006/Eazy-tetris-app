package com.retroblocks

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

internal class SoundManager(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .build()

    private val sounds = mutableMapOf<String, Int>()

    init {
        load("move")
        load("rotate")
        load("lock")
        load("line_clear")
        load("tetris")
        load("level_up")
        load("game_over")
    }

    private fun load(name: String) {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id != 0) sounds[name] = pool.load(context, id, 1)
    }

    fun play(name: String) {
        if (!SettingsManager.get(context).sound) return
        sounds[name]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() = pool.release()
}
