package com.jayway.androidthingsassistant.auth

import android.content.Context
import com.google.auth.oauth2.UserCredentials
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

internal object Credentials {

    @Throws(IOException::class, JSONException::class)
    fun fromResource(context: Context, resourceId: Int): UserCredentials {
        val inputStream = context.resources.openRawResource(resourceId)
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        val json = JSONObject(String(bytes, Charset.forName("UTF-8")))
        return UserCredentials(
                json.getString("client_id"),
                json.getString("client_secret"),
                json.getString("refresh_token")
        )
    }
}