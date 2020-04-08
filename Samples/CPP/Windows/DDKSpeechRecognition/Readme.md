
# DDK Speech Recognition App

## Introduction

This app provides a shortcut to let customers experience Speech DDK devices quickly.
There are two parts of this app, one is the C++ engine, which was compiled with cross-platform compilers for each DDK device; another is the WinForms UI, which provides device selection, device setup and speech recognition scenario functions.

## Pre-request

1. **Install ADB (Android Debug Bridge) tool**

    * Download from [https://dl.google.com/android/repository/platform-tools-latest-windows.zip](https://dl.google.com/android/repository/platform-tools-latest-windows.zip)
    * Unzip it to any folder
    * Add the folder path to System Environment variable 'Path'

2. **Firewall allow access**

    * For the first time to click any recognition button, you may see the following firewall settings window, please check all checkboxes and click 'allow access' button.
    ![firewall allow access image](FirewallAllowAccess.png)

3. **Connect device to WiFi**

    * **DDK2**
        * Run 'adb shell test_cmd_wifi -s [wifi_name] -p [wifi_password]'
    * **DDK1**
        * Install Vysor and connect WiFi through Vysor emulator UI.
    * **URbetter**
        1. Launch app, click the 'Connect to WiFi' button.
        2. Edit the pop up 'wpa_supplicant.conf' file
            * Fill WiFi SSID and password

                ```json
                network={
                    ssid=""
                    psk=""
                    key_mgmt=WPA-PSK
                    }
                ```

        3. Close the 'wpa_supplicant.conf' file and messagebox.
        4. Click the 'Connect to WiFi' button again to connect to WiFi.
    * **Eden or other devices only with microphone array**
        * Just make sure the host PC connects to internet.

## User interface

![user interface image](UserInterface.png)

* **Title:** use a number to indicate which Speech SDK version is using, like 1.9.
* **Select Device:** select the DDK device connects to Host PC.
* **Setup Device:**
    1. Click 'Connect to WiFi' button to make sure device is connected to WiFi.
    2. Click 'Setup' button to push the C++ engine and Speech SDK libs to device.
    3. Then you can click any Speech Recognition button to experience the functions.
* **Recognition buttons:** Various Speech recognition scenarios.
* **Output pane:** Show recognition result.

## Get DDK devices

DDK devices : [link to DDK devices](https://docs.microsoft.com/en-us/azure/cognitive-services/speech-service/get-speech-devices-sdk)

## Limitation

Host PC and device need connect to the same WiFi network.

## Advance user

* **Voice Signature:**
    Create your own voice signature : [link to create voice signature](https://docs.microsoft.com/en-us/azure/cognitive-services/speech-service/how-to-use-conversation-transcription#create-voice-signatures)
* **Config device:**
    1. Click the 'Edit Config' button to change the subscription key, Geometry settings, keyword settings or add your own voice signature (need also update CTS key and region, from where you generate the voice signature)
    2. Click the 'Apply Config' button to apply the config changes to device, then click any recognition button to experience functions
    3. IP address textbox shows host PC IPV4 address. Device will send recognition result to this IP address. Make sure host PC and device connect to the same WiFi network.
* **Build the C++ sample code:**
    1. On Windows: Just build the ConsoleSampleCode project, which could be used for a 6+1 microphone array connects to Windows.
    2. Build for Embedded Linux on DDK device (in future): Provide a sample build batch file (for DDK2) under docker folder in source code.
        * Install Docker, run the build.bat file from source code docker folder, the compiled file will be generated under out folder.
        * Copy the new complied file to the out folder with related Device name. Launch app, click 'Setup' button to apply the changes.
        (please add the include header files like the following picture, Linux C++ header files could get from [https://aka.ms/csspeech/linuxbinary](https://aka.ms/csspeech/linuxbinary))
        > ![Speech SDK include headers](SpeechSDKInclude.png)
