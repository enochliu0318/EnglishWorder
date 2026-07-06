package com.englishworder.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * 单词发音：优先有道在线音频（国内手机更可靠），失败时回退系统 TTS。
 */
class WordSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private val pending = AtomicReference<PendingSpeak?>(null)

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            var result = engine.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = engine.setLanguage(Locale.ENGLISH)
            }
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            pending.getAndSet(null)?.let { playWithTts(it.word) }
        }
    }

    fun speak(word: String, audioUrl: String? = null) {
        val text = word.trim()
        if (text.isEmpty()) return

        stopMediaPlayer()
        val url = audioUrl?.takeIf { it.isNotBlank() } ?: youdaoVoiceUrl(text)
        playUrl(url) { playWithTts(text) }
    }

    fun stop() {
        stopMediaPlayer()
        tts?.stop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    private fun playUrl(url: String, onFail: () -> Unit) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stopMediaPlayer() }
                setOnErrorListener { _, _, _ ->
                    stopMediaPlayer()
                    onFail()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopMediaPlayer()
            onFail()
        }
    }

    private fun playWithTts(word: String) {
        if (ttsReady) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_$word")
            return
        }
        pending.set(PendingSpeak(word))
        mainHandler.postDelayed({
            if (ttsReady) {
                pending.getAndSet(null)?.let { playWithTts(it.word) }
            }
        }, 600)
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private data class PendingSpeak(val word: String)

    companion object {
        fun youdaoVoiceUrl(word: String, usAccent: Boolean = true): String {
            val encoded = URLEncoder.encode(word.trim(), Charsets.UTF_8.name())
            val type = if (usAccent) 2 else 1
            return "https://dict.youdao.com/dictvoice?audio=$encoded&type=$type"
        }
    }
}
