# Android Things Assistant
### Add Google Assistant to your Android Things project with a custom key phrase

## Purpose
The purpose of this project is to make it easier to add Google Assistant functionality to 
Android Things projects, and also give it a custom key phrase to make it unique. 
The base of the project is in two parts. The [Google Assistant SDK]() and [PocketSphinx]()

## Usage
* In your Android Things project, create a module and import the `pocketsphinx-android-5prealpha-release.aar`.
  - File -> New -> New Module... -> Import .JAR/.AAR Package -> select the  `pocketsphinx-android-5prealpha-release.aar` in
    the `pocketsphinx-android-5prealpha-release` module directory of this project.
  - Make sure Android Studio added the module to your `settings.gradle`
    ```
    include ':app', ':pocketsphinx-android-5prealpha-release'
    ```
* Add the following dependencies to your app `build.gradle`
  ``` 
    dependencies {
      ...
      compile project(':pocketsphinx-android-5prealpha-release')
      compile 'com.github.einarvalgeirsson:android-things-assistant:v0.1.1-alpha'
    }
  ```
  
* Add the following permissions to your `AndroidManifest.xml` 
  ```
      <uses-permission android:name="android.permission.INTERNET"/>
      <uses-permission android:name="android.permission.RECORD_AUDIO"/>
      <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
      <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
  ```
  This is to allow the Google Assistant to record audio on your device and call its APIs.
  
* Implement the following in your MainActivity.
```
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
```

## Google Assistant API Credentials
To use the Google Assistant API's you need to get credentials. Follow the steps [here](https://developers.google.com/assistant/sdk/develop/grpc/config-dev-project-and-account).
Observe the section `Set activity controls for your account` which is necessary. 

## Acknowledgements
This project is largely based on the work by [CapTech](https://www.captechconsulting.com) 
and their excellent blog post [Building a Google Assistant Enabled Android Things Device](https://www.captechconsulting.com/blogs/building-your-first-assistant-enabled-android-things-device) 
and also the [Google Assistant Android Things sample app](https://github.com/androidthings/sample-googleassistant).

