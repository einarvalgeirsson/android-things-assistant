package com.jayway.androidthingsassistant

import android.content.Context
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RawRes
import android.util.Log
import com.google.assistant.embedded.v1alpha1.ConverseRequest
import com.google.assistant.embedded.v1alpha1.ConverseResponse
import com.google.assistant.embedded.v1alpha1.ConverseState
import com.google.assistant.embedded.v1alpha1.EmbeddedAssistantGrpc
import com.google.protobuf.ByteString
import com.jayway.androidthingsassistant.audio.AssistantAudioManager
import com.jayway.androidthingsassistant.auth.Credentials
import com.jayway.androidthingsassistant.keyphrase.KeyPhraseListener
import com.jayway.androidthingsassistant.keyphrase.KeyPhraseRecognizer
import com.jayway.androidthingsassistant.state.AssistantState
import com.jayway.androidthingsassistant.state.AssistantStateHandler
import com.jayway.androidthingsassistant.util.TAG
import io.grpc.ManagedChannelBuilder
import io.grpc.auth.MoreCallCredentials
import io.grpc.stub.StreamObserver
import org.json.JSONException
import java.io.IOException
import java.nio.ByteBuffer


class ThingsAssistant(
        private val context: Context,
        @RawRes private val credentialsResource: Int,
        private val keyPhrase: String
    ) : KeyPhraseListener {

    private val ASSISTANT_ENDPOINT = "embeddedassistant.googleapis.com"

    private lateinit var assistantAudioManager: AssistantAudioManager

    private val assistantThread = HandlerThread("assistantThread")
    private lateinit var assistantHandler: Handler

    private var conversationState: ByteString? = null

    private var startUserRequest: Runnable? = null
    private var streamUserRequest: Runnable? = null
    private var stopUserRequest: Runnable? = null

    // gRPC client and stream observers.
    private var assistantService: EmbeddedAssistantGrpc.EmbeddedAssistantStub? = null
    private var userRequestObserver: StreamObserver<ConverseRequest>? = null
    private var assistantResponseObserver: StreamObserver<ConverseResponse>? = null

    private lateinit var keyPhraseRecognizer: KeyPhraseRecognizer

    private val assistantStateHandler = AssistantStateHandler(context)

    fun start() {
        keyPhraseRecognizer = KeyPhraseRecognizer(context, keyPhrase, this)
        assistantThread.start()
        assistantHandler = Handler(assistantThread.looper)
        assistantAudioManager = AssistantAudioManager(context)
        assistantAudioManager.configureAudioSettings()
        setupGoogleAssistantService()
        setupUserRequests()
        setupAssistantResponseObserver()
    }

    fun destroy() {
        with(assistantThread) {
            quitSafely()
            join()
        }
        assistantAudioManager.destroy()
    }

    override fun onKeyPhraseRecogniserInitialized() {
        Log.d(TAG, "KeyPhraseRecognizer initialized")
        assistantStateHandler.publishState(AssistantState.LISTENING_FOR_KEY_PHRASE)
        startListeningForKeyPhrase()
    }

    override fun onKeyPhraseRecognized() {
        Log.d(TAG, "Key Phrase recognized")
        assistantStateHandler.publishState(AssistantState.KEY_PHRASE_DETECTED)
        assistantHandler.post(startUserRequest)
    }

    /**
     * Setup the Google Assistant with the provided credentials.
     **/
    private fun setupGoogleAssistantService() {
        val channel = ManagedChannelBuilder.forTarget(ASSISTANT_ENDPOINT).build()
        try {
            assistantService = EmbeddedAssistantGrpc.newStub(channel)
                    .withCallCredentials(MoreCallCredentials.from(
                            Credentials.fromResource(context, credentialsResource)
                    ))
        } catch (e: IOException) {
            Log.e(TAG, "error creating assistant service:", e)
        } catch (e: JSONException) {
            Log.e(TAG, "error creating assistant service:", e)
        }
    }

    /**
     * Method to host building the users request to the assistant.
     * Each request runnable is responsible for either starting the request by the user,
     * streaming the users audio, or stopping the request when the user is complete.
     *
     *
     * These all run on the assistant thread.
     */
    private fun setupUserRequests() {
        startUserRequest = Runnable {
            assistantAudioManager.audioRecord?.startRecording()
            assistantStateHandler.publishState(AssistantState.READY_FOR_COMMAND)

            userRequestObserver = assistantService?.converse(assistantResponseObserver)

            val converseConfigBuilder = assistantAudioManager.createConversationConfig()

            // If there exists a conversational state/context, use it
            if (conversationState != null) {
                converseConfigBuilder.converseState = ConverseState.newBuilder()
                        .setConversationState(conversationState)
                        .build()
            }

            userRequestObserver?.onNext(
                    ConverseRequest.newBuilder()
                            .setConfig(converseConfigBuilder.build())
                            .build())

            // Start passing the recording
            assistantHandler.post(streamUserRequest)
        }

        streamUserRequest = Runnable {
            val audioData = ByteBuffer.allocateDirect(AssistantAudioManager.SAMPLE_BLOCK_SIZE)
            val result = assistantAudioManager.audioRecord?.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING)

            result?.let {
                if (result < 0) {
                    Log.e(TAG, "Error reading from audio stream: $result")
                }
            }

            userRequestObserver?.onNext(ConverseRequest.newBuilder()
                    .setAudioIn(ByteString.copyFrom(audioData))
                    .build())

            // continue passing the recording
            assistantHandler.post(streamUserRequest)
        }

        stopUserRequest = Runnable {
            // The user is done making their request to the assistant.
            // Stop passing data and clean up.
            assistantHandler.removeCallbacks(streamUserRequest)

            userRequestObserver?.onCompleted()
            userRequestObserver = null

            // Stop recording the user
            assistantAudioManager.audioRecord?.stop()

            // Start telling the user what the Assistant has to say.
            assistantAudioManager.audioTrack?.play()


        }
    }

    private fun setupAssistantResponseObserver() {
        assistantResponseObserver = object : StreamObserver<ConverseResponse> {
            override fun onNext(value: ConverseResponse) {
                when (value.converseResponseCase) {
                    ConverseResponse.ConverseResponseCase.EVENT_TYPE -> {
                        if (value.eventType == ConverseResponse.EventType.END_OF_UTTERANCE) {
                            assistantHandler.post(stopUserRequest)
                        }
                    }

                    // The semantic result for the users spoken query
                    ConverseResponse.ConverseResponseCase.RESULT -> {
                        Log.d(TAG, "Detected user query: ${value.result.spokenRequestText}")
                        conversationState = value.result.conversationState
                        assistantHandler.post(stopUserRequest)

                        // In case there was a request to adjust the volume, this
                        // is done here.
                        if (value.result.volumePercentage != 0) {
                            assistantAudioManager.adjustVolume(value.result.volumePercentage)
                        }
                    }
                    ConverseResponse.ConverseResponseCase.AUDIO_OUT -> {
                        // the assistant wants to talk!
                        assistantStateHandler.publishState(AssistantState.PLAY_RESPONSE)
                        val audioData = ByteBuffer.wrap(value.audioOut.audioData.toByteArray())
                        assistantAudioManager.audioTrack?.write(audioData, audioData.remaining(), AudioTrack.WRITE_BLOCKING)
                    }
                    ConverseResponse.ConverseResponseCase.ERROR -> {
                        assistantStateHandler.publishState(AssistantState.ERROR)
                        assistantHandler.post(stopUserRequest)
                        Log.e(TAG, "Converse Response error: ${value.error}")
                    }
                    ConverseResponse.ConverseResponseCase.CONVERSERESPONSE_NOT_SET -> {
                        Log.d(TAG, "Converse Response not set")
                    }
                    null -> Log.d(TAG, "Converse response is null")
                }
            }

            override fun onError(t: Throwable) {
                assistantStateHandler.publishState(AssistantState.ERROR)
                Log.e(TAG, "Converse Error: $t")
                assistantHandler.post(stopUserRequest)
            }

            override fun onCompleted() {
                assistantStateHandler.publishState(AssistantState.DONE_WITH_RESPONSE)
                Log.d(TAG, "Assistant Response Finished")
                // Allow for activating the Assistant using the
                // Key Phrase again
                startListeningForKeyPhrase()
            }
        }
    }

    private fun startListeningForKeyPhrase() {
        assistantStateHandler.publishState(AssistantState.LISTENING_FOR_KEY_PHRASE)
        keyPhraseRecognizer.startListening()
    }
}
