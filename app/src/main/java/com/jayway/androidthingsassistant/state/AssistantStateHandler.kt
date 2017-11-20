package com.jayway.androidthingsassistant.state

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager

class AssistantStateHandler(val context: Context) {

    companion object {
        const val THINGS_ASSISTANT_STATE_INTENT_FILTER = "things_assistant_state"
    }

    fun publishState(state: AssistantState) {
        val intent = Intent(THINGS_ASSISTANT_STATE_INTENT_FILTER).apply {
            data = Uri.parse(state.name)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}