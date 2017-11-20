package com.jayway.androidthingsassistant.keyphrase

import android.content.Context
import android.content.Intent.ACTION_SEARCH
import android.util.Log
import com.jayway.androidthingsassistant.util.TAG
import edu.cmu.pocketsphinx.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.File
import java.lang.Exception

internal class KeyPhraseRecognizer(
        private val context: Context,
        private val keyPhrase: String,
        private val keyPhraseListener: KeyPhraseListener
    ) : RecognitionListener {

    private val WAKEUP_SEARCH = "wakeup"
    private lateinit var speechRecognizer: SpeechRecognizer

    init {
        setupRecognizer()
    }

    fun startListening() {
        speechRecognizer.startListening(WAKEUP_SEARCH)
    }

    private fun setupRecognizer() {
        val assets = Assets(context)
        async(UI) {
            val assetsDir: Deferred<File> = bg {
                assets.syncAssets()
            }

            speechRecognizer = SpeechRecognizerSetup.defaultSetup()
                    .setAcousticModel(File(assetsDir.await(), "en-us-ptm"))
                    .setDictionary(File(assetsDir.await(), "cmudict-en-us.dict"))
                    .recognizer
            with(speechRecognizer) {
                addListener(this@KeyPhraseRecognizer)
                addKeyphraseSearch(WAKEUP_SEARCH, keyPhrase)
                addNgramSearch(ACTION_SEARCH, File(assetsDir.await(), "predefined.lm.bin"))
            }

            keyPhraseListener.onKeyPhraseRecogniserInitialized()
        }
    }

    override fun onResult(hypothesis: Hypothesis?) {
        hypothesis?.let {
            val text = it.hypstr
            Log.d(TAG, "onResult: $text")
            if (text == keyPhrase) {
                keyPhraseListener.onKeyPhraseRecognized()
            }
        }
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        hypothesis?.let {
            val text = it.hypstr
            Log.d(TAG, "onPartialResult: $text")

            if (text == keyPhrase) {
                speechRecognizer.stop()
            }
        }
    }

    override fun onTimeout() {
        Log.d(TAG, "onTimeOut")
        speechRecognizer.stop()
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        if (speechRecognizer.searchName != WAKEUP_SEARCH) {
                speechRecognizer.stop()
        }
    }

    override fun onError(e: Exception?) {
        Log.d(TAG, "onError: $e")
    }
}