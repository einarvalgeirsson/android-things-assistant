package com.jayway.androidthingsassistant.audio

import android.content.Context
import android.media.*
import android.util.Log
import com.google.assistant.embedded.v1alpha1.AudioInConfig
import com.google.assistant.embedded.v1alpha1.AudioOutConfig
import com.google.assistant.embedded.v1alpha1.ConverseConfig
import com.jayway.androidthingsassistant.util.TAG

/**
 * Used to handle all Audio settings for the Google Assistant
 **/
class AssistantAudioManager(private val context: Context) {

    companion object {
        const val SAMPLE_BLOCK_SIZE = 1024
    }

    private lateinit var audioManager: AudioManager
    var audioRecord: AudioRecord? = null
    var audioTrack: AudioTrack? = null

    // Audio constants for Assistant
    private val SAMPLE_RATE = 16000
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val ENCODING_INPUT = AudioInConfig.Encoding.LINEAR16
    private val ENCODING_OUTPUT = AudioOutConfig.Encoding.LINEAR16
    private var audioTrackVolume = 100

    private val ASSISTANT_AUDIO_REQUEST_CONFIG = AudioInConfig.newBuilder()
            .setEncoding(ENCODING_INPUT)
            .setSampleRateHertz(SAMPLE_RATE)
            .build()

    private val AUDIO_FORMAT_OUT_MONO = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .build()
    private val AUDIO_FORMAT_IN_MONO = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .build()

    private val AUDIO_FORMAT_STEREO = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .build()

    fun configureAudioSettings() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // turn volume all the way up for the manager, the track will manage itself.
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        val outputBufferSize = AudioTrack.getMinBufferSize(AUDIO_FORMAT_OUT_MONO.sampleRate,
                AUDIO_FORMAT_OUT_MONO.channelMask,
                AUDIO_FORMAT_OUT_MONO.encoding)

        audioTrack = AudioTrack.Builder()
                .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
                .setBufferSizeInBytes(outputBufferSize)
                .build()

        audioTrack?.play()

        val inputBufferSize = AudioRecord.getMinBufferSize(AUDIO_FORMAT_STEREO.sampleRate,
                AUDIO_FORMAT_STEREO.channelMask,
                AUDIO_FORMAT_STEREO.encoding)


        audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AUDIO_FORMAT_IN_MONO)
                .setBufferSizeInBytes(inputBufferSize)
                .build()
    }

     fun adjustVolume(percentage: Int) {
        if (percentage == 0) return

        Log.d(TAG, "setting volume to: " + percentage)
        audioTrackVolume = percentage
        val newVolume = AudioTrack.getMaxVolume() * percentage / 100f
        audioTrack?.setVolume(newVolume)
    }

    fun createConversationConfig(): ConverseConfig.Builder {
        return ConverseConfig.newBuilder()
                .setAudioInConfig(ASSISTANT_AUDIO_REQUEST_CONFIG)
                .setAudioOutConfig(AudioOutConfig.newBuilder()
                        .setEncoding(ENCODING_OUTPUT)
                        .setSampleRateHertz(SAMPLE_RATE)
                        .setVolumePercentage(audioTrackVolume)//must do this for the assistant to know it can adjust
                        .build())
    }

    fun destroy() {
        audioRecord?.stop()
        audioRecord = null
        audioTrack?.stop()
        audioTrack = null
    }
}