package com.retroblocks

import android.content.Context
import android.media.MediaPlayer

internal class MusicManager(private val context: Context) {
    private var menuPlayer: MediaPlayer? = null
    private var gamePlayer: MediaPlayer? = null
    private var currentType: MusicType? = null

    enum class MusicType { MENU, GAME }

    private fun getPlayer(type: MusicType): MediaPlayer? {
        val resId = when (type) {
            MusicType.MENU -> context.resources.getIdentifier("menu_music", "raw", context.packageName)
            MusicType.GAME -> context.resources.getIdentifier("game_music", "raw", context.packageName)
        }
        if (resId == 0) return null
        
        return try {
            MediaPlayer.create(context, resId).apply {
                isLooping = true
            }
        } catch (e: Exception) {
            null
        }
    }

    fun play(type: MusicType) {
        if (currentType == type && (menuPlayer?.isPlaying == true || gamePlayer?.isPlaying == true)) return
        
        stopAll()
        currentType = type
        
        if (!SettingsManager.get(context).music) return

        val player = when (type) {
            MusicType.MENU -> {
                if (menuPlayer == null) menuPlayer = getPlayer(type)
                menuPlayer
            }
            MusicType.GAME -> {
                if (gamePlayer == null) gamePlayer = getPlayer(type)
                gamePlayer
            }
        }
        
        try {
            player?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            menuPlayer?.takeIf { it.isPlaying }?.pause()
            gamePlayer?.takeIf { it.isPlaying }?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        if (!SettingsManager.get(context).music) return
        val type = currentType ?: return
        val player = if (type == MusicType.MENU) menuPlayer else gamePlayer
        try {
            player?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSettings() {
        if (SettingsManager.get(context).music) {
            val type = currentType ?: MusicType.MENU
            play(type)
        } else {
            stopAll()
        }
    }

    private fun stopAll() {
        try {
            menuPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
            gamePlayer?.let { if (it.isPlaying) it.stop(); it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        menuPlayer = null
        gamePlayer = null
    }

    fun release() {
        stopAll()
    }
}
