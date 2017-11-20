package com.jayway.androidthingsassistant

import android.content.Context
import android.content.res.Resources
import com.jayway.androidthingsassistant.auth.Credentials
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.shouldEqual
import org.junit.Test


class CredentialsTest {

    private val fakeClientId = "my_fake_client_id"
    private val fakeClientSecret = "my_fake_client_secret"
    private val fakeRefreshToken = "my_fake_refresh_token"
    private val credentialsJson =
            """
            {
                "scopes": ["https://www.googleapis.com/auth/assistant-sdk-prototype"],
                "token_uri": "https://accounts.google.com/o/oauth2/token",
                "client_id": "$fakeClientId",
                "client_secret": "$fakeClientSecret",
                "refresh_token": "$fakeRefreshToken"
            }
            """


    @Test
    fun credentials_json_file_can_be_correctly_interpreted() {
        val mockContext: Context = mock()
        val mockResources: Resources = mock()
        val fakeResourceId = 1
        val inputStream = credentialsJson.toByteArray(Charsets.UTF_8).inputStream()

        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.openRawResource(fakeResourceId)).thenReturn(inputStream)

        val userCredentials = Credentials.fromResource(mockContext, fakeResourceId)

        userCredentials.clientId shouldEqual fakeClientId
        userCredentials.clientSecret shouldEqual fakeClientSecret
        userCredentials.refreshToken shouldEqual fakeRefreshToken
    }
}