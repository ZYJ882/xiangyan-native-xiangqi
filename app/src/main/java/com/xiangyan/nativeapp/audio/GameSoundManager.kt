package com.xiangyan.nativeapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.xiangyan.nativeapp.R

enum class GameSound(val resourceId: Int) {
    Move(R.raw.sfx_move),
    Capture(R.raw.sfx_capture),
    Check(R.raw.sfx_check),
    Terminal(R.raw.sfx_terminal),
    Start(R.raw.sfx_start),
    Pause(R.raw.sfx_pause),
    Stop(R.raw.sfx_stop),
}

/**
 * 低延迟棋局音效。音效不抢占音频焦点，跟随系统音量；系统静音或用户关闭开关时不播放。
 * SoundPool 延迟加载到第一次需要播放时，避免拖慢应用冷启动。
 */
class GameSoundManager(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val loadedIds = mutableSetOf<Int>()
    private val pending = ArrayDeque<GameSound>()
    private val soundIds: Map<GameSound, Int>
    private var enabled = true

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            loadedIds += sampleId
            val ready = pending.filter { soundIds[it] == sampleId }
            pending.removeAll(ready.toSet())
            ready.forEach { playLoaded(it) }
        }
        soundIds = GameSound.entries.associateWith { soundPool.load(context, it.resourceId, 1) }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) pending.clear()
    }

    fun play(sound: GameSound) {
        if (!enabled || audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val id = soundIds[sound] ?: return
        if (id !in loadedIds) {
            if (sound !in pending) pending.addLast(sound)
            return
        }
        playLoaded(sound)
    }

    private fun playLoaded(sound: GameSound) {
        if (!enabled || audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        soundPool.play(soundIds[sound] ?: return, 0.82f, 0.82f, 1, 0, 1.0f)
    }

    fun release() {
        pending.clear()
        soundPool.release()
    }
}
