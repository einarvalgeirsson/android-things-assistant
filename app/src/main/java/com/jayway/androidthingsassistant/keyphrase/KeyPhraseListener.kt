package com.jayway.androidthingsassistant.keyphrase

/**
 * Pickup the custom key phrase before sending the
 * rest of the audio data to Google Assistant
 **/
internal interface KeyPhraseListener {

    /**
     * Called when the KeyPhraseRecogniser is set up and ready
     * to listen for the Key Phrase
     **/
    fun onKeyPhraseRecogniserInitialized()

    /**
     * Called when the specified key phrase is recognised.
     **/
    fun onKeyPhraseRecognized()
}