package com.jayway.androidthingsassistant.state

enum class AssistantState {
    LISTENING_FOR_KEY_PHRASE,
    KEY_PHRASE_DETECTED,
    READY_FOR_COMMAND,
    GOT_COMMAND,
    PLAY_RESPONSE,
    DONE_WITH_RESPONSE,
    ERROR
}