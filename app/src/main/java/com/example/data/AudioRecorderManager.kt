package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    // Player state
    private var mediaPlayer: MediaPlayer? = null
    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _playerProgress = MutableStateFlow(0f)
    val playerProgress: StateFlow<Float> = _playerProgress.asStateFlow()

    fun startRecording(): File? {
        try {
            stopPlayback()
            val audioDir = File(context.cacheDir, "voice_notes")
            if (!audioDir.exists()) audioDir.mkdirs()
            val file = File(audioDir, "VN_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            _isRecording.value = false
            currentOutputFile = null
            return null
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val duration = if (startTimeMs > 0) System.currentTimeMillis() - startTimeMs else 0L
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
        }
        val file = currentOutputFile
        currentOutputFile = null
        startTimeMs = 0L
        return Pair(file, duration)
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error canceling recording", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }

    fun playAudio(messageId: String, url: String) {
        if (_playingMessageId.value == messageId) {
            // Toggle pause/resume
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    return
                } else {
                    player.start()
                    return
                }
            }
        }

        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                    _playingMessageId.value = messageId
                }
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to play audio", e)
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            _playingMessageId.value = null
            _playerProgress.value = 0f
        }
    }
}
