package com.jayway.thingsexample

import android.app.Activity
import android.os.Bundle
import com.jayway.androidthingsassistant.ThingsAssistant

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

    private lateinit var thingsAssistant: ThingsAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thingsAssistant = ThingsAssistant(
                context = applicationContext,
                credentialsResource = R.raw.credentials,
                keyPhrase = "hey computer")
    }

    override fun onStart() {
        super.onStart()
        thingsAssistant.start()
    }

    override fun onStop() {
        super.onStop()
        thingsAssistant.destroy()
    }
}
