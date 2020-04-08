#include <iostream> // cin, cout
#include <speechapi_cxx.h>
#include <fstream>
#include <chrono>
#include <vector>
#include <iterator>
#include <nlohmann/json.hpp>
//network header for windows
#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <stdio.h>
#include <windows.h>
// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")
#define DEFAULT_BUFLEN 1024
#else
//network header for linux
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#endif

using namespace std;
using namespace std::chrono;
using namespace Microsoft::CognitiveServices::Speech;
using namespace Microsoft::CognitiveServices::Speech::Transcription;
using namespace Microsoft::CognitiveServices::Speech::Audio;
using namespace Microsoft::CognitiveServices::Speech::Intent;
using namespace Microsoft::CognitiveServices::Speech::Translation;
using namespace Microsoft::CognitiveServices::Speech::Dialog;
using namespace nlohmann;

static string YourSubscriptionKey = "";
static string YourServiceRegion = "";
static string YourLanguageUnderstandingSubscriptionKey = "";
static string YourLanguageUnderstandingServiceRegion = "";
static string YourLanguageUnderstandingAppId = "";
static string YourCTSSubscriptionKey = "";
static string YourCTSServiceRegion = "";
static string YourSpeechCustomCommandsAppId = "";
static string YourSpeechDialogSubscriptionKey = "";
static string YourSpeechDialogRegion = "";
static string DeviceGeometry = "Circular6+1"; //Linear4 //Circular6+1
static string SelectedGeometry = "Circular6+1"; //Linear4 //Circular6+1
static string Keyword = "computer"; //machine // assistant
static string KeywordTable = "kws_computer.table"; // kws_machine // kws_assistant
const string configFilePath = "config.json";
const string resultFilePath = "result.txt";
static bool AddedVoiceSignature = false;
static int Port = 11000;
static int sockfd;
static string ServerName;


#pragma region TCP Helper
#ifdef _WIN32
//https://docs.microsoft.com/en-us/windows/win32/winsock/complete-client-code
void SendToTCPServer(string content)
{
    WSADATA wsaData;
    SOCKET ConnectSocket = INVALID_SOCKET;
    struct addrinfo *result = NULL,
        *ptr = NULL,
        hints;
    const char *sendbuf = content.c_str();
    char recvbuf[DEFAULT_BUFLEN];
    int iResult;
    int recvbuflen = DEFAULT_BUFLEN;

    try
    {
        // Initialize Winsock
        iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
        if (iResult != 0) {
            cout << "WSAStartup failed with error:" << iResult << "\n" << std::endl;
            return;
        }

        ZeroMemory(&hints, sizeof(hints));
        hints.ai_family = AF_UNSPEC;
        hints.ai_socktype = SOCK_STREAM;
        hints.ai_protocol = IPPROTO_TCP;

        // Resolve the server address and port
        iResult = getaddrinfo(ServerName.c_str(), to_string(Port).c_str(), &hints, &result);
        if (iResult != 0) {
            cout << "getaddrinfo failed with error:" << iResult << "\n" << std::endl;
            WSACleanup();
            return;
        }

        // Attempt to connect to an address until one succeeds
        for (ptr = result; ptr != NULL; ptr = ptr->ai_next) {

            // Create a SOCKET for connecting to server
            ConnectSocket = socket(ptr->ai_family, ptr->ai_socktype,
                ptr->ai_protocol);
            if (ConnectSocket == INVALID_SOCKET) {
                cout << "socket failed with error:" << WSAGetLastError() << "\n" << std::endl;
                WSACleanup();
            }

            // Connect to server.
            iResult = connect(ConnectSocket, ptr->ai_addr, (int)ptr->ai_addrlen);
            if (iResult == SOCKET_ERROR) {
                closesocket(ConnectSocket);
                ConnectSocket = INVALID_SOCKET;
                continue;
            }
            break;
        }

        freeaddrinfo(result);

        if (ConnectSocket == INVALID_SOCKET) {
            cout << "Unable to connect to server!\n" << "\n" << std::endl;
            WSACleanup();
        }

        // Send an initial buffer
        iResult = send(ConnectSocket, sendbuf, (int)strlen(sendbuf), 0);
        if (iResult == SOCKET_ERROR) {
            cout << "send failed with error:" << WSAGetLastError() << "\n" << std::endl;
            closesocket(ConnectSocket);
            WSACleanup();
        }

        printf("Bytes Sent: %ld\n", iResult);

        // shutdown the connection since no more data will be sent
        iResult = shutdown(ConnectSocket, SD_SEND);
        if (iResult == SOCKET_ERROR) {
            cout << "shutdown failed with error:" << WSAGetLastError() << "\n" << std::endl;
            closesocket(ConnectSocket);
            WSACleanup();
        }

        // cleanup
        closesocket(ConnectSocket);
        WSACleanup();
    }
    catch (const exception& e)
    {
        cout << "Failed to send result to TCP server:" << e.what() << "\n" << std::endl;
    }
}
#else
void SendToTCPServer(string content)
{
    struct sockaddr_in serv_addr;
    struct hostent *server;

    try
    {
        int sockfd = socket(AF_INET, SOCK_STREAM, 0);
        if (sockfd < 0) {
            cout << ("ERROR opening socket \n") << std::endl;
            return;
        }
        server = gethostbyname(ServerName.c_str());
        if (server == NULL) {
            cout << "ERROR, no such host to connect \n" << std::endl;
            close(sockfd);
            return;
        }
        bzero((char *)&serv_addr, sizeof(serv_addr));
        serv_addr.sin_family = AF_INET;
        bcopy((char *)server->h_addr,
            (char *)&serv_addr.sin_addr.s_addr,
            server->h_length);
        serv_addr.sin_port = htons(Port);
        if (connect(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
            cout << "ERROR connecting to host server \n" << std::endl;
            close(sockfd);
            return;
        }
        write(sockfd, content.c_str(), content.length());
        close(sockfd);
    }
    catch (const exception& e)
    {
        cout << "Failed to send result to TCP server:" << e.what() << "\n" << std::endl;
    }
}
#endif
#pragma endregion

#pragma region SpeechSamples
// Speech recognition using microphone.
void SpeechRecognitionWithMicrophone()
{
    // <SpeechRecognitionWithMicrophone>
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates a speech recognizer using microphone as audio input. The default language is "en-us".
    auto recognizer = SpeechRecognizer::FromConfig(config);
    cout << "Say something...\n";

    // Starts speech recognition, and returns after a single utterance is recognized. The end of a
    // single utterance is determined by listening for silence at the end or until a maximum of 15
    // seconds of audio is processed.  The task returns the recognition text as result.
    // Note: Since RecognizeOnceAsync() returns only a single utterance, it is suitable only for single
    // shot recognition like command or query.
    // For long-running multi-utterance recognition, use StartContinuousRecognitionAsync() instead.
    auto result = recognizer->RecognizeOnceAsync().get();

    // Checks result.
    if (result->Reason == ResultReason::RecognizedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << std::endl;
    }
    else if (result->Reason == ResultReason::NoMatch)
    {
        cout << "NOMATCH: Speech could not be recognized." << std::endl;
    }
    else if (result->Reason == ResultReason::Canceled)
    {
        auto cancellation = CancellationDetails::FromResult(result);
        cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

        if (cancellation->Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << cancellation->ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    }
    // </SpeechRecognitionWithMicrophone>
}

// Speech continuous recognition using microphone
void SpeechContinuousRecognitionWithMicrophone()
{
    // <SpeechContinuousRecognitionWithFile>
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry); //Circular6+1 //Linear4
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry); //Circular6+1 //Linear4

    // Creates a speech recognizer using microphone as audio input. The default language is "en-us".
    auto recognizer = SpeechRecognizer::FromConfig(config);
    cout << "Say something...\n";

    // promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Text.length() > 0)
        {
            cout << "Recognizing:" << e.Result->Text << std::endl;
            SendToTCPServer("ing>" + e.Result->Text + "\n");
        }
    });

    recognizer->Recognized.Connect([&recognitionEnd](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            if (e.Result->Text.length() > 0)
            {
                cout << "RECOGNIZED:" << e.Result->Text << "\n" << std::endl;
                SendToTCPServer("ed>" + e.Result->Text + "\n");
            }

            // Check if user says "stop" to stop the continuous recognition
            //string recognized = e.Result->Text;
            //transform(recognized.begin(), recognized.end(), recognized.begin(), ::tolower);
            //if (recognized == "stop.")
            //{
            //  recognitionEnd.set_value(); // Notify to stop recognition.
            //}
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;

            SendToTCPServer("cancel>" + e.ErrorDetails + "\n");

            recognitionEnd.set_value(); // Notify to stop recognition.
        }
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "Session stopped.";
        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts continuous recognition. Uses StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartContinuousRecognitionAsync().get();

    // Waits for recognition end.
    recognitionEnd.get_future().get();

    // Stops recognition.
    recognizer->StopContinuousRecognitionAsync().get();
    // </SpeechContinuousRecognitionWithMicrophone>
}

// Speech continuous recognition using microphone
void SpeechContinuousRecognitionWithFile()
{
    // <SpeechContinuousRecognitionWithFile>
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Creates a speech recognizer using file as audio input.
    // Replace with your own audio file name.
    auto audioInput = AudioConfig::FromWavFileInput("/dev/sample/whatstheweatherlike.wav");
    auto recognizer = SpeechRecognizer::FromConfig(config, audioInput);

    // promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        cout << "Recognizing:" << e.Result->Text << std::endl;
    });

    recognizer->Recognized.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << "\n"
                << "  Offset=" << e.Result->Offset() << "\n"
                << "  Duration=" << e.Result->Duration() << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;

            recognitionEnd.set_value(); // Notify to stop recognition.
        }
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "Session stopped.";
        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts continuous recognition. Uses StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartContinuousRecognitionAsync().get();

    // Waits for recognition end.
    recognitionEnd.get_future().get();

    // Stops recognition.
    recognizer->StopContinuousRecognitionAsync().get();
    // </SpeechContinuousRecognitionWithFile>
}

// Speech recognition in the specified language, using microphone, and requesting detailed output format.
void SpeechRecognitionWithLanguageAndUsingDetailedOutputFormat()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Request detailed output format.
    config->SetOutputFormat(OutputFormat::Detailed);

    // Creates a speech recognizer in the specified language using microphone as audio input.
    // Replace the language with your language in BCP-47 format, e.g. en-US.
    string input;
    cout << "\n1) English \n2) Chinese: ";
    cout.flush();

    input.empty();
    getline(cin, input);
    auto lang = "en-US";

    switch (input[0])
    {
    case '1':
        lang = "en-US";
        break;
    case '2':
        lang = "zh-CN";
        break;
    }

    auto recognizer = SpeechRecognizer::FromConfig(config, lang);
    cout << "Say something in " << lang << "...\n";

    // Starts speech recognition, and returns after a single utterance is recognized. The end of a
    // single utterance is determined by listening for silence at the end or until a maximum of 15
    // seconds of audio is processed.  The task returns the recognition text as result.
    // Note: Since RecognizeOnceAsync() returns only a single utterance, it is suitable only for single
    // shot recognition like command or query.
    // For long-running multi-utterance recognition, use StartContinuousRecognitionAsync() instead.
    auto result = recognizer->RecognizeOnceAsync().get();

    // Checks result.
    if (result->Reason == ResultReason::RecognizedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << std::endl
            << "  Speech Service JSON: " << result->Properties.GetProperty(PropertyId::SpeechServiceResponse_JsonResult)
            << std::endl;
    }
    else if (result->Reason == ResultReason::NoMatch)
    {
        cout << "NOMATCH: Speech could not be recognized." << std::endl;
    }
    else if (result->Reason == ResultReason::Canceled)
    {
        auto cancellation = CancellationDetails::FromResult(result);
        cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

        if (cancellation->Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << cancellation->ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    }
}

// Speech recognition with auto detection for source language
void SpeechRecognitionWithSourceLanguageAutoDetection()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Currently this feature only supports 2 languages
    // Replace the languages with your languages in BCP-47 format, e.g. fr-FR.
    // Please see https://docs.microsoft.com/azure/cognitive-services/speech-service/language-support for all supported langauges
    auto autoDetectSourceLanguageConfig = AutoDetectSourceLanguageConfig::FromLanguages({ "en-US", "zh-CN" });

    // The recognizer uses microphone,  to use file or stream as audio input, just construct the audioInput and pass to FromConfig API as the 3rd parameter.
    // Ex: auto recognizer = SpeechRecognizer::FromConfig(speechConfig, autoDetectSourceLanguageConfig, audioInput);
    auto recognizer = SpeechRecognizer::FromConfig(config, autoDetectSourceLanguageConfig);
    cout << "Say something in either English or Chinese...\n";

    // Starts speech recognition, and returns after a single utterance is recognized. The end of a
    // single utterance is determined by listening for silence at the end or until a maximum of 15
    // seconds of audio is processed.  The task returns the recognition text as result.
    // Note: Since RecognizeOnceAsync() returns only a single utterance, it is suitable only for single
    // shot recognition like command or query.
    // For long-running multi-utterance recognition, use StartContinuousRecognitionAsync() instead.
    auto result = recognizer->RecognizeOnceAsync().get();

    // Checks result.
    auto autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult::FromResult(result);
    auto language = autoDetectSourceLanguageResult->Language;
    if (result->Reason == ResultReason::RecognizedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << std::endl;
        cout << "RECOGNIZED: Language=" << language << std::endl;
    }
    else if (result->Reason == ResultReason::NoMatch)
    {
        if (language.empty())
        {
            // serivce cannot detect the source language
            cout << "NOMATCH: Service cannot detect the source language." << std::endl;
        }
        else
        {
            // serivce can detect the source language but cannot recongize the speech content
            cout << "NOMATCH: Service can recognize the speech." << std::endl;
        }
    }
    else if (result->Reason == ResultReason::Canceled)
    {
        auto cancellation = CancellationDetails::FromResult(result);
        cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

        if (cancellation->Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << cancellation->ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    }
}

// Keyword-triggered speech recognition using microphone.
void KeywordTriggeredSpeechRecognitionWithMicrophone()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates a speech recognizer using microphone as audio input. The default language is "en-us".
    auto recognizer = SpeechRecognizer::FromConfig(config);

    // Promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizingSpeech)
        {
            cout << "RECOGNIZING: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizingKeyword)
        {
            cout << "RECOGNIZING KEYWORD: Text=" << e.Result->Text << std::endl;
        }
    });

    recognizer->Recognized.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedKeyword)
        {
            cout << "RECOGNIZED KEYWORD: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    });

    recognizer->SessionStarted.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTARTED: SessionId=" << e.SessionId << std::endl;
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTOPPED: SessionId=" << e.SessionId << std::endl;

        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Creates an instance of a keyword recognition model. Update this to
    // point to the location of your keyword recognition model.
    auto model = KeywordRecognitionModel::FromFile(KeywordTable);

    // The phrase your keyword recognition model triggers on.
    auto keyword = Keyword;

    // Starts continuous recognition. Use StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartKeywordRecognitionAsync(model).get();

    cout << "Say something starting with '" << keyword
        << "' followed by whatever you want..." << std::endl;

    // Waits for a single successful keyword-triggered speech recognition (or error).
    recognitionEnd.get_future().get();

    // Stops recognition.
    recognizer->StopKeywordRecognitionAsync().get();
}

// Keyword-triggered speech recognition using microphone.
void KeywordContinuousTriggeredSpeechRecognitionWithMicrophoneSaveToFile()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates a speech recognizer using microphone as audio input. The default language is "en-us".
    auto recognizer = SpeechRecognizer::FromConfig(config);

    // Promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Creates an instance of a keyword recognition model. Update this to
    // point to the location of your keyword recognition model.
    auto model = KeywordRecognitionModel::FromFile(KeywordTable);

    // The phrase your keyword recognition model triggers on.
    auto keyword = Keyword;

    //ofstream log("kwsResult.log", std::ios_base::app | std::ios_base::out);

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizingSpeech)
        {
            cout << "RECOGNIZING: Text=" << e.Result->Text << std::endl;
            SendToTCPServer("ing>" + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::RecognizingKeyword)
        {
            cout << "RECOGNIZING KEYWORD: Text=" << e.Result->Text << std::endl;
        }
    });

    recognizer->Recognized.Connect([&recognitionEnd, &keyword](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedKeyword)
        {
            cout << "RECOGNIZED KEYWORD: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
            SendToTCPServer("ed>" + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;

            SendToTCPServer("cancel>" + e.ErrorDetails + "\n");
        }
    });

    recognizer->SessionStarted.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTARTED: SessionId=" << e.SessionId << std::endl;
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTOPPED: SessionId=" << e.SessionId << std::endl;

        //recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts continuous recognition. Use StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartKeywordRecognitionAsync(model).get();

    cout << "Say something starting with '" << keyword
        << "' followed by whatever you want..." << std::endl;

    // Waits for a single successful keyword-triggered speech recognition (or error).
    recognitionEnd.get_future().get();

    // Stops recognition.
    recognizer->StopKeywordRecognitionAsync().get();
}
#pragma endregion

#pragma region IntentSamples
// Intent recognition using microphone.
void IntentRecognitionWithMicrophone()
{
    // <IntentRecognitionWithMicrophone>
    // Creates an instance of a speech config with specified subscription key
    // and service region. Note that in contrast to other services supported by
    // the Cognitive Services Speech SDK, the Language Understanding service
    // requires a specific subscription key from https://www.luis.ai/.
    // The Language Understanding service calls the required key 'endpoint key'.
    // Once you've obtained it, replace with below with your own Language Understanding subscription key
    // and service region (e.g., "westus").
    // The default recognition language is "en-us".
    auto config = SpeechConfig::FromSubscription(YourLanguageUnderstandingSubscriptionKey, YourLanguageUnderstandingServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates an intent recognizer using microphone as audio input.
    auto recognizer = IntentRecognizer::FromConfig(config);

    // Creates a Language Understanding model using the app id, and adds specific intents from your model
    auto model = LanguageUnderstandingModel::FromAppId(YourLanguageUnderstandingAppId);
    recognizer->AddIntent(model, "play music", "1");
    recognizer->AddIntent(model, "stop", "2");
    //recognizer->AddIntent(model, "YourLanguageUnderstandingIntentName3", "any-IntentId-here");

    cout << "Say something (like play music or stop)...\n";

    // Starts intent recognition, and returns after a single utterance is recognized. The end of a
    // single utterance is determined by listening for silence at the end or until a maximum of 15
    // seconds of audio is processed.  The task returns the recognition text as result. 
    // Note: Since RecognizeOnceAsync() returns only a single utterance, it is suitable only for single
    // shot recognition like command or query. 
    // For long-running multi-utterance recognition, use StartContinuousRecognitionAsync() instead.
    auto result = recognizer->RecognizeOnceAsync().get();

    // Checks result.
    if (result->Reason == ResultReason::RecognizedIntent)
    {
        cout << "RECOGNIZED: Text=" << result->Text << std::endl;
        cout << "  Intent Id: " << result->IntentId << std::endl;
        cout << "  Intent Service JSON: " << result->Properties.GetProperty(PropertyId::LanguageUnderstandingServiceResponse_JsonResult) << std::endl;
    }
    else if (result->Reason == ResultReason::RecognizedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << " (intent could not be recognized)" << std::endl;
    }
    else if (result->Reason == ResultReason::NoMatch)
    {
        cout << "NOMATCH: Speech could not be recognized." << std::endl;
    }
    else if (result->Reason == ResultReason::Canceled)
    {
        auto cancellation = CancellationDetails::FromResult(result);
        cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

        if (cancellation->Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << cancellation->ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    }
    // </IntentRecognitionWithMicrophone>
}

// Intent with keyword trigger recognition using microphone.
void IntentWithKeywordRecognitionWithMicrophone()
{
    // <IntentRecognitionWithMicrophone>
    // Creates an instance of a speech config with specified subscription key
    // and service region. Note that in contrast to other services supported by
    // the Cognitive Services Speech SDK, the Language Understanding service
    // requires a specific subscription key from https://www.luis.ai/.
    // The Language Understanding service calls the required key 'endpoint key'.
    // Once you've obtained it, replace with below with your own Language Understanding subscription key
    // and service region (e.g., "westus").
    // The default recognition language is "en-us".
    auto config = SpeechConfig::FromSubscription(YourLanguageUnderstandingSubscriptionKey, YourLanguageUnderstandingServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates an intent recognizer using microphone as audio input.
    auto recognizer = IntentRecognizer::FromConfig(config);

    // Creates a Language Understanding model using the app id, and adds specific intents from your model
    auto intentModel = LanguageUnderstandingModel::FromAppId(YourLanguageUnderstandingAppId);
    recognizer->AddIntent(intentModel, "play music", "1");
    recognizer->AddIntent(intentModel, "stop", "2");
    //recognizer->AddIntent(model, "YourLanguageUnderstandingIntentName3", "any-IntentId-here");

    // Promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const IntentRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizingSpeech)
        {
            cout << "RECOGNIZING: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizingKeyword)
        {
            cout << "RECOGNIZING KEYWORD: Text=" << e.Result->Text << std::endl;
        }
    });

    recognizer->Recognized.Connect([](const IntentRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedKeyword)
        {
            cout << "RECOGNIZED KEYWORD: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedIntent)
        {
            cout << "  Intent Id: " << e.Result->IntentId << std::endl;
            cout << "  Intent Service JSON: " << e.Result->Properties.GetProperty(PropertyId::LanguageUnderstandingServiceResponse_JsonResult) << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const IntentRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    });

    recognizer->SessionStarted.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTARTED: SessionId=" << e.SessionId << std::endl;
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTOPPED: SessionId=" << e.SessionId << std::endl;

        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Creates an instance of a keyword recognition model. Update this to
    // point to the location of your keyword recognition model.
    auto keywordModel = KeywordRecognitionModel::FromFile("kws.table");

    // The phrase your keyword recognition model triggers on.
    auto keyword = Keyword;

    // Starts continuous recognition. Use StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartKeywordRecognitionAsync(keywordModel).get();

    cout << "Say something (like " + keyword + "play music or " + keyword + " stop)...\n";

    // Waits for a single successful keyword-triggered speech recognition (or error).
    recognitionEnd.get_future().get();

    // Stops recognition.
    recognizer->StopKeywordRecognitionAsync().get();

    // </IntentRecognitionWithMicrophone>
}

#pragma endregion

#pragma region TranslationSamples
// Translation with microphone input.
void TranslationWithMicrophone()
{
    // <TranslationWithMicrophone>
    // Creates an instance of a speech translation config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechTranslationConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Sets source and target languages
    // Replace with the languages of your choice.
    auto fromLanguage = "en-US";
    config->SetSpeechRecognitionLanguage(fromLanguage);
    config->AddTargetLanguage("de");
    config->AddTargetLanguage("fr");
    config->AddTargetLanguage("zh");

    // Creates a translation recognizer using microphone as audio input.
    auto recognizer = TranslationRecognizer::FromConfig(config);
    cout << "Say something...\n";

    // Starts translation, and returns after a single utterance is recognized. The end of a
    // single utterance is determined by listening for silence at the end or until a maximum of 15
    // seconds of audio is processed. The task returns the recognized text as well as the translation.
    // Note: Since RecognizeOnceAsync() returns only a single utterance, it is suitable only for single
    // shot recognition like command or query.
    // For long-running multi-utterance recognition, use StartContinuousRecognitionAsync() instead.
    auto result = recognizer->RecognizeOnceAsync().get();

    // Checks result.
    if (result->Reason == ResultReason::TranslatedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << std::endl
            << "  Language=" << fromLanguage << std::endl;

        for (const auto& it : result->Translations)
        {
            cout << "TRANSLATED into '" << it.first.c_str() << "': " << it.second.c_str() << std::endl;
        }
    }
    else if (result->Reason == ResultReason::RecognizedSpeech)
    {
        cout << "RECOGNIZED: Text=" << result->Text << " (text could not be translated)" << std::endl;
    }
    else if (result->Reason == ResultReason::NoMatch)
    {
        cout << "NOMATCH: Speech could not be recognized." << std::endl;
    }
    else if (result->Reason == ResultReason::Canceled)
    {
        auto cancellation = CancellationDetails::FromResult(result);
        cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

        if (cancellation->Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << cancellation->ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    }
    // </TranslationWithMicrophone>
}

// Continuous translation.
void TranslationContinuousRecognition()
{
    // Creates an instance of a speech translation config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechTranslationConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Sets source and target languages
    auto fromLanguage = "en-US";
    config->SetSpeechRecognitionLanguage(fromLanguage);
    config->AddTargetLanguage("de");
    config->AddTargetLanguage("fr");
    config->AddTargetLanguage("zh");

    // Creates a translation recognizer using microphone as audio input.
    auto recognizer = TranslationRecognizer::FromConfig(config);

    // Subscribes to events.
    recognizer->Recognizing.Connect([](const TranslationRecognitionEventArgs& e)
    {
        cout << "Recognizing:" << e.Result->Text << std::endl;
        for (const auto& it : e.Result->Translations)
        {
            cout << "  Translated into '" << it.first.c_str() << "': " << it.second.c_str() << std::endl;
        }
    });

    recognizer->Recognized.Connect([](const TranslationRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::TranslatedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << " (text could not be translated)" << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }

        for (const auto& it : e.Result->Translations)
        {
            cout << "  Translated into '" << it.first.c_str() << "': " << it.second.c_str() << std::endl;
        }
    });

    recognizer->Canceled.Connect([](const TranslationRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;
        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << e.ErrorDetails << std::endl;
            cout << "CANCELED: Did you update the subscription info?" << std::endl;
        }
    });

    recognizer->Synthesizing.Connect([](const TranslationSynthesisEventArgs& e)
    {
        auto size = e.Result->Audio.size();
        cout << "Translation synthesis result: size of audio data: " << size
            << (size == 0 ? "(END)" : "");
    });

    cout << "Say something...\n";

    // Starts continuos recognition. Uses StopContinuousRecognitionAsync() to stop recognition.
    recognizer->StartContinuousRecognitionAsync().get();

    cout << "Press any key to stop\n";
    string s;
    getline(cin, s);

    // Stops recognition.
    recognizer->StopContinuousRecognitionAsync().get();
}
#pragma endregion

#pragma region SpeechSynthesisSamples
// Speech synthesis to the default speaker.
void SpeechSynthesisToSpeaker()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Creates a speech synthesizer using the default speaker as audio output. The default spoken language is "en-us".
    auto synthesizer = SpeechSynthesizer::FromConfig(config);

    while (true)
    {
        // Receives a text from console input and synthesize it to speaker.
        cout << "Enter some text that you want to speak, or enter empty text to exit." << std::endl;
        cout << "> ";
        std::string text;
        getline(cin, text);
        if (text.empty())
        {
            break;
        }

        auto result = synthesizer->SpeakTextAsync(text).get();

        // Checks result.
        if (result->Reason == ResultReason::SynthesizingAudioCompleted)
        {
            cout << "Speech synthesized to speaker for text [" << text << "]" << std::endl;
        }
        else if (result->Reason == ResultReason::Canceled)
        {
            auto cancellation = SpeechSynthesisCancellationDetails::FromResult(result);
            cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

            if (cancellation->Reason == CancellationReason::Error)
            {
                cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
                cout << "CANCELED: ErrorDetails=[" << cancellation->ErrorDetails << "]" << std::endl;
                cout << "CANCELED: Did you update the subscription info?" << std::endl;
            }
        }
    }
}

// Speech synthesis in the specified spoken language.
void SpeechSynthesisWithLanguage()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Sets the synthesis language.
    // The full list of supported language can be found here:
    // https://docs.microsoft.com/azure/cognitive-services/speech-service/language-support
    auto language = "zh-CN";
    config->SetSpeechSynthesisLanguage(language);

    // Creates a speech synthesizer for the specified language, using the default speaker as audio output.
    auto synthesizer = SpeechSynthesizer::FromConfig(config);

    while (true)
    {
        // Receives a text from console input and synthesize it to speaker.
        cout << "Enter some text in Chinese that you want to speak, or enter empty text to exit." << std::endl;
        cout << "> ";
        std::string text;
        getline(cin, text);
        if (text.empty())
        {
            break;
        }

        auto result = synthesizer->SpeakTextAsync(text).get();

        // Checks result.
        if (result->Reason == ResultReason::SynthesizingAudioCompleted)
        {
            cout << "Speech synthesized to speaker for text [" << text << "] with language [" << language << "]" << std::endl;
        }
        else if (result->Reason == ResultReason::Canceled)
        {
            auto cancellation = SpeechSynthesisCancellationDetails::FromResult(result);
            cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

            if (cancellation->Reason == CancellationReason::Error)
            {
                cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
                cout << "CANCELED: ErrorDetails=[" << cancellation->ErrorDetails << "]" << std::endl;
                cout << "CANCELED: Did you update the subscription info?" << std::endl;
            }
        }
    }
}

// Speech synthesis in the specified voice.
void SpeechSynthesisWithVoice()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Sets the voice name.
    // e.g. "Microsoft Server Speech Text to Speech Voice (en-US, JessaRUS)".
    // The full list of supported voices can be found here:
    // https://docs.microsoft.com/azure/cognitive-services/speech-service/language-support
    auto voice = "Microsoft Server Speech Text to Speech Voice (en-US, BenjaminRUS)";
    config->SetSpeechSynthesisVoiceName(voice);

    // Creates a speech synthesizer for the specified voice, using the default speaker as audio output.
    auto synthesizer = SpeechSynthesizer::FromConfig(config);

    while (true)
    {
        // Receives a text from console input and synthesize it to speaker.
        cout << "Enter some text that you want to speak, or enter empty text to exit." << std::endl;
        cout << "> ";
        std::string text;
        getline(cin, text);
        if (text.empty())
        {
            break;
        }

        auto result = synthesizer->SpeakTextAsync(text).get();

        // Checks result.
        if (result->Reason == ResultReason::SynthesizingAudioCompleted)
        {
            cout << "Speech synthesized to speaker for text [" << text << "] with voice [" << voice << "]" << std::endl;
        }
        else if (result->Reason == ResultReason::Canceled)
        {
            auto cancellation = SpeechSynthesisCancellationDetails::FromResult(result);
            cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

            if (cancellation->Reason == CancellationReason::Error)
            {
                cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
                cout << "CANCELED: ErrorDetails=[" << cancellation->ErrorDetails << "]" << std::endl;
                cout << "CANCELED: Did you update the subscription info?" << std::endl;
            }
        }
    }
}

// Speech synthesis to wave file.
void SpeechSynthesisToWaveFile()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Creates a speech synthesizer using file as audio output.
    // Replace with your own audio file name.
    auto fileName = "outputaudio.wav";
    auto fileOutput = AudioConfig::FromWavFileOutput(fileName);
    auto synthesizer = SpeechSynthesizer::FromConfig(config, fileOutput);

    while (true)
    {
        // Receives a text from console input and synthesize it to wave file.
        cout << "Enter some text that you want to synthesize, or enter empty text to exit." << std::endl;
        cout << "> ";
        std::string text;
        getline(cin, text);
        if (text.empty())
        {
            break;
        }

        auto result = synthesizer->SpeakTextAsync(text).get();

        // Checks result.
        if (result->Reason == ResultReason::SynthesizingAudioCompleted)
        {
            cout << "Speech synthesized for text [" << text << "], and the audio was saved to [" << fileName << "]" << std::endl;
        }
        else if (result->Reason == ResultReason::Canceled)
        {
            auto cancellation = SpeechSynthesisCancellationDetails::FromResult(result);
            cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

            if (cancellation->Reason == CancellationReason::Error)
            {
                cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
                cout << "CANCELED: ErrorDetails=[" << cancellation->ErrorDetails << "]" << std::endl;
                cout << "CANCELED: Did you update the subscription info?" << std::endl;
            }
        }
    }
}

// Speech synthesis to mp3 file.
void SpeechSynthesisToMp3File()
{
    // Creates an instance of a speech config with specified subscription key and service region.
    // Replace with your own subscription key and service region (e.g., "westus").
    auto config = SpeechConfig::FromSubscription(YourSubscriptionKey, YourServiceRegion);

    // Sets the synthesis output format.
    // The full list of supported format can be found here:
    // https://docs.microsoft.com/azure/cognitive-services/speech-service/rest-text-to-speech#audio-outputs
    config->SetSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat::Audio16Khz32KBitRateMonoMp3);

    // Creates a speech synthesizer using file as audio output.
    // Replace with your own audio file name.
    auto fileName = "outputaudio.mp3";
    auto fileOutput = AudioConfig::FromWavFileOutput(fileName);
    auto synthesizer = SpeechSynthesizer::FromConfig(config, fileOutput);

    while (true)
    {
        // Receives a text from console input and synthesize it to mp3 file.
        cout << "Enter some text that you want to synthesize, or enter empty text to exit." << std::endl;
        cout << "> ";
        std::string text;
        getline(cin, text);
        if (text.empty())
        {
            break;
        }

        auto result = synthesizer->SpeakTextAsync(text).get();

        // Checks result.
        if (result->Reason == ResultReason::SynthesizingAudioCompleted)
        {
            cout << "Speech synthesized for text [" << text << "], and the audio was saved to [" << fileName << "]" << std::endl;
        }
        else if (result->Reason == ResultReason::Canceled)
        {
            auto cancellation = SpeechSynthesisCancellationDetails::FromResult(result);
            cout << "CANCELED: Reason=" << (int)cancellation->Reason << std::endl;

            if (cancellation->Reason == CancellationReason::Error)
            {
                cout << "CANCELED: ErrorCode=" << (int)cancellation->ErrorCode << std::endl;
                cout << "CANCELED: ErrorDetails=[" << cancellation->ErrorDetails << "]" << std::endl;
                cout << "CANCELED: Did you update the subscription info?" << std::endl;
            }
        }
    }
}
#pragma endregion

#pragma region ConversationTranscriberSamples
// Helper functions
class WavFileReader final
{
public:

    // Constructor that creates an input stream from a file.
    WavFileReader(const std::string& audioFileName)
    {
        if (audioFileName.empty())
        {
            throw std::invalid_argument("Audio filename is empty");
        }

        std::ios_base::openmode mode = std::ios_base::binary | std::ios_base::in;
        m_fs.open(audioFileName, mode);
        if (!m_fs.good())
        {
            throw std::invalid_argument("Failed to open the specified audio file.");
        }

        // Get audio format from the file header.
        GetFormatFromWavFile();
    }

    int Read(uint8_t* dataBuffer, uint32_t size)
    {
        if (m_fs.eof())
            // returns 0 to indicate that the stream reaches end.
            return 0;
        m_fs.read((char*)dataBuffer, size);
        if (!m_fs.eof() && !m_fs.good())
            // returns 0 to close the stream on read error.
            return 0;
        else
            // returns the number of bytes that have been read.
            return (int)m_fs.gcount();
    }

    void Close()
    {
        m_fs.close();
    }

private:
    // Defines common constants for WAV format.
    static constexpr uint16_t tagBufferSize = 4;
    static constexpr uint16_t chunkTypeBufferSize = 4;
    static constexpr uint16_t chunkSizeBufferSize = 4;

    // Get format data from a wav file.
    void GetFormatFromWavFile()
    {
        char tag[tagBufferSize];
        char chunkType[chunkTypeBufferSize];
        char chunkSizeBuffer[chunkSizeBufferSize];
        uint32_t chunkSize = 0;

        // Set to throw exceptions when reading file header.
        m_fs.exceptions(std::ifstream::failbit | std::ifstream::badbit);

        try
        {
            // Checks the RIFF tag
            m_fs.read(tag, tagBufferSize);
            if (memcmp(tag, "RIFF", tagBufferSize) != 0)
            {
                throw std::runtime_error("Invalid file header, tag 'RIFF' is expected.");
            }

            // The next is the RIFF chunk size, ignore now.
            m_fs.read(chunkSizeBuffer, chunkSizeBufferSize);

            // Checks the 'WAVE' tag in the wave header.
            m_fs.read(chunkType, chunkTypeBufferSize);
            if (memcmp(chunkType, "WAVE", chunkTypeBufferSize) != 0)
            {
                throw std::runtime_error("Invalid file header, tag 'WAVE' is expected.");
            }

            bool foundDataChunk = false;
            while (!foundDataChunk && m_fs.good() && !m_fs.eof())
            {
                ReadChunkTypeAndSize(chunkType, &chunkSize);
                if (memcmp(chunkType, "fmt ", chunkTypeBufferSize) == 0)
                {
                    // Reads format data.
                    m_fs.read((char *)&m_formatHeader, sizeof(m_formatHeader));

                    // Skips the rest of format data.
                    if (chunkSize > sizeof(m_formatHeader))
                    {
                        m_fs.seekg(chunkSize - sizeof(m_formatHeader), std::ios_base::cur);
                    }
                }
                else if (memcmp(chunkType, "data", chunkTypeBufferSize) == 0)
                {
                    foundDataChunk = true;
                    break;
                }
                else
                {
                    m_fs.seekg(chunkSize, std::ios_base::cur);
                }
            }

            if (!foundDataChunk)
            {
                throw std::runtime_error("Did not find data chunk.");
            }
            if (m_fs.eof() && chunkSize > 0)
            {
                throw std::runtime_error("Unexpected end of file, before any audio data can be read.");
            }
        }
        catch (std::ifstream::failure e)
        {
            throw std::runtime_error("Unexpected end of file or error when reading audio file.");
        }
        // Set to not throw exceptions when starting to read audio data
        m_fs.exceptions(std::ifstream::goodbit);
    }

    void ReadChunkTypeAndSize(char* chunkType, uint32_t* chunkSize)
    {
        // Read the chunk type
        m_fs.read(chunkType, chunkTypeBufferSize);

        // Read the chunk size
        uint8_t chunkSizeBuffer[chunkSizeBufferSize];
        m_fs.read((char*)chunkSizeBuffer, chunkSizeBufferSize);

        // chunk size is little endian
        *chunkSize = ((uint32_t)chunkSizeBuffer[3] << 24) |
            ((uint32_t)chunkSizeBuffer[2] << 16) |
            ((uint32_t)chunkSizeBuffer[1] << 8) |
            (uint32_t)chunkSizeBuffer[0];
    }

    // The format structure expected in wav files.
    struct WAVEFORMAT
    {
        uint16_t FormatTag;        // format type.
        uint16_t Channels;         // number of channels (i.e. mono, stereo...).
        uint32_t SamplesPerSec;    // sample rate.
        uint32_t AvgBytesPerSec;   // for buffer estimation.
        uint16_t BlockAlign;       // block size of data.
        uint16_t BitsPerSample;    // Number of bits per sample of mono data.
    } m_formatHeader;
    static_assert(sizeof(m_formatHeader) == 16, "unexpected size of m_formatHeader");

private:
    std::fstream m_fs;
};

// Voice signature class
class VoiceSignature final
{
public:
    static string Name;
    static string Language;
    static string Version;
    static string Tag;
    static string Data;
};


// Transcribing conversation using a pull audio stream
// Note: This is only available on the devices that can be paired with the Cognitive Services Speech Device SDK.
void ConversationWithPullAudioStream()
{
    // First, define your own pull audio input stream callback class that implements the
    // PullAudioInputStreamCallback interface. The sample here illustrates how to define such
    // a callback that reads audio data from a wav file.
    // AudioInputFromFileCallback implements PullAudioInputStreamCallback interface, and uses a wav file as source
    class AudioInputFromFileCallback final : public PullAudioInputStreamCallback
    {
    public:
        // Constructor that creates an input stream from a file.
        AudioInputFromFileCallback(const string& audioFileName)
            : m_reader(audioFileName)
        {
        }
        // Implements AudioInputStream::Read() which is called to get data from the audio stream.
        // It copies data available in the stream to 'dataBuffer', but no more than 'size' bytes.
        // If the data available is less than 'size' bytes, it is allowed to just return the amount of data that is currently available.
        // If there is no data, this function must wait until data is available.
        // It returns the number of bytes that have been copied in 'dataBuffer'.
        // It returns 0 to indicate that the stream reaches end or is closed.
        int Read(uint8_t* dataBuffer, uint32_t size) override
        {
            return m_reader.Read(dataBuffer, size);
        }
        // Implements AudioInputStream::Close() which is called when the stream needs to be closed.
        void Close() override
        {
            m_reader.Close();
        }

    private:
        WavFileReader m_reader;
    };

    // Creates an instance of a speech config with your subscription key and region.
    // Replace with your own subscription key and service region (e.g., "eastasia").
    // Conversation Transcription is currently available in eastasia and centralus region.
    auto config = SpeechConfig::FromSubscription(YourCTSSubscriptionKey, YourCTSServiceRegion);
    config->SetProperty("ConversationTranscriptionInRoomAndOnline", "true");

    // Creates a callback that will read audio data from a WAV file.
    shared_ptr<AudioInputFromFileCallback> callback;
    try
    {
        // Replace with your own audio file name.
        // The audio file should be in a format of 16 kHz sampling rate, 16 bits per sample, and 8 channels.
        callback = make_shared<AudioInputFromFileCallback>("/dev/sample/katiesteve.wav");
    }
    catch (const exception& e)
    {
        cout << "Exit due to exception: " << e.what() << endl;
    }

    // Create a pull stream that support 16kHz, 16 bits and 8 channels of PCM audio.
    auto pullStream = AudioInputStream::CreatePullStream(AudioStreamFormat::GetWaveFormatPCM(16000, 16, 8), callback);
    auto audioInput = AudioConfig::FromStreamInput(pullStream);

    // Create a conversation from a speech config and conversation Id.
    auto conversation = Conversation::CreateConversationAsync(config, "ConversationTranscriberSamples").get();

    // Create a conversation transcriber given an audio config. If you don't specify any audio input, Speech SDK opens the default microphone.
    auto recognizer = ConversationTranscriber::FromConfig(audioInput);

    // Need to join a conversation before streaming audio.
    recognizer->JoinConversationAsync(conversation).get();

    // Create voice signatures using REST API at https://signature.centralus.cts.speech.microsoft.com by using YourSubscriptionKey
    // and the provided enrollment_audio_katie.wav and enrollment_audio_steve.wav.
    // Replace the below voiceSignatureKatie and voiceSignatureSteve by copy the Signature value from the Response body. The Signature value contains Version, Tag and Data.
    // More details are at https://docs.microsoft.com/azure/cognitive-services/speech-service/how-to-use-conversation-transcription-service
    string voiceSignatureKatie = R"(
               { "Version": 0,
                 "Tag": "8/VW19IB9cjFK9qmTN00WMFTDnv/EZWDjcHyRsXpUyM=",
                 "Data": "VBjHR9CamugWLQiz9GVxV0mMaClFeO3s1+jNhr5ZGEqxslGYgajd4tqXw0i10h8fcPykwStUfVmXvf1TI969fDEmOacg8OGVw4eK6Rf07jKei+9U48UzRB9k21g/NbmyiAz54NlIUZXDX2U2TuQFNK6igl9ihce89FzDBDdRYQUjopO2UzF6sCbeGBIu10yVizpt+stdm/cmH9nmp9glCqC9TE6wQcEWA+bHb8s1HwRmyYYFM75vfPEg5X6wwSlFffD+NxBDdcpUDz8hDFXcMrM5ewViE8uu2Vg7uUpGrxTg3V6Vn2eltuKYWCjeuMRUn16RDiQ4GMJXEcRh2o2QZ7E7XjVxdMWpe2BevSn8P2jMLrywyLjcVZr657yPtFE2OAyptYRiZiOr8nOeR2cBAd9PY3f8qHN4IkbTch/oHLO6knBFTMiU8PwGjpoTHWmNy+6v82+EiqLTePO9g0Uq6KLJx21Rt6LGu/6lSKgZy6GHHCQ5HiIG8OJTuHdo+3DFjjOqyp14snGUH2l8HbNIWrNbXgMVK9s2crTDzFzwFIQMcsC611AT9+6/f0Kld5+ktmMZnQvgCzaNvh0x0lIym/VVnbzGoj9W+TPAVfzXA8ZKBMulQKd/ecHbjs/e+FxYFnJH2M7oWvtBYmc1jm5wUQaUP4Gyzy5MrWU0ybZ/jqySVcSkpsWTgTydw6/1drVY"})";
    string voiceSignatureSteve = R"(
                { "Version": 0,
                  "Tag": "INw9UqKZ8BhBuvKwhexu0ozF5CUCYB4mAZzhv7loC9Q=",
                  "Data": "f6G59CcZiQgLkwSm/tzG2qzrzoMPneEcRP7vQ2t95poI9P6udvtrRzyYQfC9nEeFwTIBUv5YAb5Xmm+EGir5+6jLxwwho5VK1nYz9wDs1BUdeq++hrFAKqsJOEWE/QpHFPesUC8rRsHFKscQwtzb+YcuKuGMqPnODGX4apON/zxhWXwSGGaMNE/8snrIWrN1Qfb3/W7QYpCXX7oN0okb3KwyiIKRz35hooKqEszyrj4+t3BbZKQR6pPQEwzwfjUKxErwA7vwyOU9KTwflv0Z/qm3P6fVM9ytUTz8cKifYXhTAV7IXpwqE393QiyXsOEEe+gWVtVcbX/1Mf8VOqWXCoflc78RC2MrXD96XgPCTc93kfqsw/3jM/ZlcLAiWxRut5kH58JKfLco89+zWwx9Uorn2kt5aidL+Q05HhhxZpd61PTxSN9M0GlFpI/EG0FKknXyo+OEV0k4Owv++audQsPYP1Cvtmk72zK2VakP50wyXKHwOxZs1/tGDWpF5ue6MWtpC4BgYXsIAu4RbOECw1x7FkKa7NgEi4WBo4Tvp2ZL1r/e9C54HzEYnb9cil9YHMzs2BqNOZWxitEI2buIL35j/jU+Bp3Enuiov6ZYh4EStxZgsXNuH7doNREkNSXKlwkmbYWS4Xe/EqywZqGm0f1RUEDIEgZFZdswZpFPsNwEDOgJT2PIcgjTeNgU6guH"})";

    // creates a participant
    auto katie = Participant::From("katie@example.com", "en-us", voiceSignatureKatie);

    // creates another participant
    auto steve = Participant::From("steve@example.com", "en-us", voiceSignatureSteve);

    // Adds katie as a participant to the conversation.
    conversation->AddParticipantAsync(katie).get();

    // Adds steve as a participant to the conversation.
    conversation->AddParticipantAsync(steve).get();

    // a promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Transcribing.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        cout << "TRANSCRIBING: Text=" << e.Result->Text << std::endl;
    });

    recognizer->Transcribed.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "Transcribed: Text=" << e.Result->Text << std::endl
                << "  Offset=" << e.Result->Offset() << std::endl
                << "  Duration=" << e.Result->Duration() << std::endl
                << "  UserId=" << e.Result->UserId << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const ConversationTranscriptionCanceledEventArgs& e)
    {
        switch (e.Reason)
        {
        case CancellationReason::EndOfStream:
            cout << "CANCELED: Reached the end of the file." << std::endl;
            break;

        case CancellationReason::Error:
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << e.ErrorDetails << std::endl;
            recognitionEnd.set_value();
            break;

        default:
            cout << "unknown reason ?!" << std::endl;
        }
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSION: " << e.SessionId << " stopped." << std::endl;
        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts transcribing.
    recognizer->StartTranscribingAsync().wait();

    // Waits for transcribing to end.
    recognitionEnd.get_future().wait();

    // Stops transcribing. This is optional.
    recognizer->StopTranscribingAsync().wait();
}

// Transcribing conversation using a push audio stream
// Note: This is only available on the devices that can be paired with the Cognitive Services Speech Device SDK.
void ConversationWithPushAudioStream()
{
    // Creates an instance of a speech config with your subscription key and region.
    // Replace with your own subscription key and service region (e.g., "eastasia").
    // Conversation Transcription is currently available in eastasia and centralus region.
    auto config = SpeechConfig::FromSubscription(YourCTSSubscriptionKey, YourCTSServiceRegion);
    config->SetProperty("ConversationTranscriptionInRoomAndOnline", "true");

    // Creates a push stream using 16kHz, 16bits per sample and 8 channels audio.
    auto pushStream = AudioInputStream::CreatePushStream(AudioStreamFormat::GetWaveFormatPCM(16000, 16, 8));
    auto audioInput = AudioConfig::FromStreamInput(pushStream);
    auto conversation = Conversation::CreateConversationAsync(config, "ConversationTranscriberSamples").get();
    auto recognizer = ConversationTranscriber::FromConfig(audioInput);
    recognizer->JoinConversationAsync(conversation).get();

    // Create voice signatures using REST API at https://signature.centralus.cts.speech.microsoft.com by using YourSubscriptionKey
    // and the provided enrollment_audio_katie.wav and enrollment_audio_steve.wav.
    // Replace the below voiceSignatureKatie and voiceSignatureSteve by copy the Signature value from the Response body. The Signature value contains Version, Tag and Data.
    // More details are at https://docs.microsoft.com/azure/cognitive-services/speech-service/how-to-use-conversation-transcription-service
    string voiceSignatureKatie = R"(
               { "Version": 0,
                 "Tag": "8/VW19IB9cjFK9qmTN00WMFTDnv/EZWDjcHyRsXpUyM=",
                 "Data": "VBjHR9CamugWLQiz9GVxV0mMaClFeO3s1+jNhr5ZGEqxslGYgajd4tqXw0i10h8fcPykwStUfVmXvf1TI969fDEmOacg8OGVw4eK6Rf07jKei+9U48UzRB9k21g/NbmyiAz54NlIUZXDX2U2TuQFNK6igl9ihce89FzDBDdRYQUjopO2UzF6sCbeGBIu10yVizpt+stdm/cmH9nmp9glCqC9TE6wQcEWA+bHb8s1HwRmyYYFM75vfPEg5X6wwSlFffD+NxBDdcpUDz8hDFXcMrM5ewViE8uu2Vg7uUpGrxTg3V6Vn2eltuKYWCjeuMRUn16RDiQ4GMJXEcRh2o2QZ7E7XjVxdMWpe2BevSn8P2jMLrywyLjcVZr657yPtFE2OAyptYRiZiOr8nOeR2cBAd9PY3f8qHN4IkbTch/oHLO6knBFTMiU8PwGjpoTHWmNy+6v82+EiqLTePO9g0Uq6KLJx21Rt6LGu/6lSKgZy6GHHCQ5HiIG8OJTuHdo+3DFjjOqyp14snGUH2l8HbNIWrNbXgMVK9s2crTDzFzwFIQMcsC611AT9+6/f0Kld5+ktmMZnQvgCzaNvh0x0lIym/VVnbzGoj9W+TPAVfzXA8ZKBMulQKd/ecHbjs/e+FxYFnJH2M7oWvtBYmc1jm5wUQaUP4Gyzy5MrWU0ybZ/jqySVcSkpsWTgTydw6/1drVY"})";
    string voiceSignatureSteve = R"(
                { "Version": 0,
                  "Tag": "INw9UqKZ8BhBuvKwhexu0ozF5CUCYB4mAZzhv7loC9Q=",
                  "Data": "f6G59CcZiQgLkwSm/tzG2qzrzoMPneEcRP7vQ2t95poI9P6udvtrRzyYQfC9nEeFwTIBUv5YAb5Xmm+EGir5+6jLxwwho5VK1nYz9wDs1BUdeq++hrFAKqsJOEWE/QpHFPesUC8rRsHFKscQwtzb+YcuKuGMqPnODGX4apON/zxhWXwSGGaMNE/8snrIWrN1Qfb3/W7QYpCXX7oN0okb3KwyiIKRz35hooKqEszyrj4+t3BbZKQR6pPQEwzwfjUKxErwA7vwyOU9KTwflv0Z/qm3P6fVM9ytUTz8cKifYXhTAV7IXpwqE393QiyXsOEEe+gWVtVcbX/1Mf8VOqWXCoflc78RC2MrXD96XgPCTc93kfqsw/3jM/ZlcLAiWxRut5kH58JKfLco89+zWwx9Uorn2kt5aidL+Q05HhhxZpd61PTxSN9M0GlFpI/EG0FKknXyo+OEV0k4Owv++audQsPYP1Cvtmk72zK2VakP50wyXKHwOxZs1/tGDWpF5ue6MWtpC4BgYXsIAu4RbOECw1x7FkKa7NgEi4WBo4Tvp2ZL1r/e9C54HzEYnb9cil9YHMzs2BqNOZWxitEI2buIL35j/jU+Bp3Enuiov6ZYh4EStxZgsXNuH7doNREkNSXKlwkmbYWS4Xe/EqywZqGm0f1RUEDIEgZFZdswZpFPsNwEDOgJT2PIcgjTeNgU6guH"})";

    // creates a participant
    auto katie = Participant::From("katie@example.com", "en-us", voiceSignatureKatie);

    // creates another participant
    auto steve = Participant::From("steve@example.com", "en-us", voiceSignatureSteve);

    // adds katie as a participant to the conversation.
    conversation->AddParticipantAsync(katie).get();

    // adds steve as a participant to the conversation.
    conversation->AddParticipantAsync(steve).get();

    // promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Transcribing.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        cout << "TRANSCRIBING: Text=" << e.Result->Text << std::endl;
    });

    recognizer->Transcribed.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl
                << "  Offset=" << e.Result->Offset() << std::endl
                << "  Duration=" << e.Result->Duration() << std::endl
                << "  UserId=" << e.Result->UserId << std::endl;
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const ConversationTranscriptionCanceledEventArgs& e)
    {
        switch (e.Reason)
        {
        case CancellationReason::EndOfStream:
            cout << "CANCELED: Reached the end of the file." << std::endl;
            break;

        case CancellationReason::Error:
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << e.ErrorDetails << std::endl;
            recognitionEnd.set_value();
            break;

        default:
            cout << "unknown reason ?!" << std::endl;
        }
    });

    // Starts transcribing conversation.
    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSION: " << e.SessionId << " stopped." << std::endl;

        // Notify transcribing ends.
        recognitionEnd.set_value();
    });

    // open and read the wave file and push the buffers into the recognizer
    recognizer->StartTranscribingAsync().wait();

    // The audio file should be in a format of 16 kHz sampling rate, 16 bits per sample, and 8 channels.
    try
    {
        WavFileReader reader("/dev/sample/katiesteve.wav");
        vector<uint8_t> buffer(1000);

        // Read data and push them into the stream
        int readSamples = 0;
        while ((readSamples = reader.Read(buffer.data(), (uint32_t)buffer.size())) != 0)
        {
            // Push a buffer into the stream
            pushStream->Write(buffer.data(), readSamples);
            this_thread::sleep_for(10ms);
        }
    }
    catch (const exception& e)
    {
        cout << "Exit due to exception " << e.what() << endl;
    }

    // Close the push stream.
    pushStream->Close();

    // Waits for completion.
    recognitionEnd.get_future().wait();

    // Leaves the conversation.
    recognizer->StopTranscribingAsync().wait();
}

// Transcribing conversation using the audio stream from microphone
// Note: This is only available on the devices that can be paired with the Cognitive Services Speech Device SDK.
void ConversationWithMicrophoneAudioStream()
{
    // Creates an instance of a speech config with your subscription key and region.
    // Replace with your own subscription key and service region (e.g., "eastasia").
    // Conversation Transcription is currently available in eastasia and centralus region.
    auto config = SpeechConfig::FromSubscription(YourCTSSubscriptionKey, YourCTSServiceRegion);
    config->SetProperty("ConversationTranscriptionInRoomAndOnline", "true");
    config->SetProperty("DeviceGeometry", DeviceGeometry);
    config->SetProperty("SelectedGeometry", "Raw");

    // Create a microphone audio stream that support 16kHz, 16 bits and 8 channels of PCM audio.
    auto audioInput = AudioConfig::FromDefaultMicrophoneInput();

    // Create a conversation from a speech config and conversation Id.
    auto conversation = Conversation::CreateConversationAsync(config, "ConversationTranscriberSamples02062020").get();

    // Create a conversation transcriber given an audio config. If you don't specify any audio input, Speech SDK opens the default microphone.
    auto recognizer = ConversationTranscriber::FromConfig(audioInput);

    // Need to join a conversation before streaming audio.
    recognizer->JoinConversationAsync(conversation).get();

    // Create voice signatures using REST API at https://signature.centralus.cts.speech.microsoft.com by using YourSubscriptionKey
    // and the provided enrollment_audio_katie.wav and enrollment_audio_steve.wav.
    // Replace the below voiceSignatureKatie and voiceSignatureSteve by copy the Signature value from the Response body. The Signature value contains Version, Tag and Data.
    // More details are at https://docs.microsoft.com/azure/cognitive-services/speech-service/how-to-use-conversation-transcription-service
    //string voiceSignatureKatie = R"(
    //           { "Version": 0,
    //           "Tag": "8/VW19IB9cjFK9qmTN00WMFTDnv/EZWDjcHyRsXpUyM=",
    //           "Data": "VBjHR9CamugWLQiz9GVxV0mMaClFeO3s1+jNhr5ZGEqxslGYgajd4tqXw0i10h8fcPykwStUfVmXvf1TI969fDEmOacg8OGVw4eK6Rf07jKei+9U48UzRB9k21g/NbmyiAz54NlIUZXDX2U2TuQFNK6igl9ihce89FzDBDdRYQUjopO2UzF6sCbeGBIu10yVizpt+stdm/cmH9nmp9glCqC9TE6wQcEWA+bHb8s1HwRmyYYFM75vfPEg5X6wwSlFffD+NxBDdcpUDz8hDFXcMrM5ewViE8uu2Vg7uUpGrxTg3V6Vn2eltuKYWCjeuMRUn16RDiQ4GMJXEcRh2o2QZ7E7XjVxdMWpe2BevSn8P2jMLrywyLjcVZr657yPtFE2OAyptYRiZiOr8nOeR2cBAd9PY3f8qHN4IkbTch/oHLO6knBFTMiU8PwGjpoTHWmNy+6v82+EiqLTePO9g0Uq6KLJx21Rt6LGu/6lSKgZy6GHHCQ5HiIG8OJTuHdo+3DFjjOqyp14snGUH2l8HbNIWrNbXgMVK9s2crTDzFzwFIQMcsC611AT9+6/f0Kld5+ktmMZnQvgCzaNvh0x0lIym/VVnbzGoj9W+TPAVfzXA8ZKBMulQKd/ecHbjs/e+FxYFnJH2M7oWvtBYmc1jm5wUQaUP4Gyzy5MrWU0ybZ/jqySVcSkpsWTgTydw6/1drVY"})";
    //string voiceSignatureSteve = R"(
    //            { "Version": 0,
    //            "Tag": "INw9UqKZ8BhBuvKwhexu0ozF5CUCYB4mAZzhv7loC9Q=",
    //            "Data": "f6G59CcZiQgLkwSm/tzG2qzrzoMPneEcRP7vQ2t95poI9P6udvtrRzyYQfC9nEeFwTIBUv5YAb5Xmm+EGir5+6jLxwwho5VK1nYz9wDs1BUdeq++hrFAKqsJOEWE/QpHFPesUC8rRsHFKscQwtzb+YcuKuGMqPnODGX4apON/zxhWXwSGGaMNE/8snrIWrN1Qfb3/W7QYpCXX7oN0okb3KwyiIKRz35hooKqEszyrj4+t3BbZKQR6pPQEwzwfjUKxErwA7vwyOU9KTwflv0Z/qm3P6fVM9ytUTz8cKifYXhTAV7IXpwqE393QiyXsOEEe+gWVtVcbX/1Mf8VOqWXCoflc78RC2MrXD96XgPCTc93kfqsw/3jM/ZlcLAiWxRut5kH58JKfLco89+zWwx9Uorn2kt5aidL+Q05HhhxZpd61PTxSN9M0GlFpI/EG0FKknXyo+OEV0k4Owv++audQsPYP1Cvtmk72zK2VakP50wyXKHwOxZs1/tGDWpF5ue6MWtpC4BgYXsIAu4RbOECw1x7FkKa7NgEi4WBo4Tvp2ZL1r/e9C54HzEYnb9cil9YHMzs2BqNOZWxitEI2buIL35j/jU+Bp3Enuiov6ZYh4EStxZgsXNuH7doNREkNSXKlwkmbYWS4Xe/EqywZqGm0f1RUEDIEgZFZdswZpFPsNwEDOgJT2PIcgjTeNgU6guH"})";

    // creates a participant
    //auto katie = Participant::From("katie@example.com", "en-us", voiceSignatureKatie);

    // creates another participant
    //auto steve = Participant::From("steve@example.com", "en-us", voiceSignatureSteve);

    // if there are participants added in the config file, pull the info and add to converstaion
    if (AddedVoiceSignature)
    {
        std::ifstream i(configFilePath, std::ifstream::in);
        json j;
        i >> j;

        auto pList = j.at("PARTICIPANTSLIST");
        for (auto const& p : pList)
        {
            json vs;
            vs["Version"] = std::stoi(p["Version"].get<string>());
            cout << "Added " + p["Name"].get<string>() << std::endl;
            vs["Tag"] = p["Tag"].get<string>();
            vs["Data"] = p["Data"].get<string>();
            auto participant = Participant::From(p["Name"], p["Language"], vs.dump());
            conversation->AddParticipantAsync(participant).get();
        }
    }

    // Adds katie as a participant to the conversation.
    //conversation->AddParticipantAsync(katie).get();

    // Adds steve as a participant to the conversation.
    //conversation->AddParticipantAsync(steve).get();

    // a promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    recognizer->Transcribing.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        cout << "TRANSCRIBING: Text=" << e.Result->UserId + " : " << e.Result->Text << std::endl;
        if (e.Result->Text.length() > 0)
        {
            SendToTCPServer("ing>" + e.Result->UserId + " : " + e.Result->Text + "\n");
        }
    });

    recognizer->Transcribed.Connect([](const ConversationTranscriptionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "Transcribed: Text=" << e.Result->Text << std::endl
                << "  Offset=" << e.Result->Offset() << std::endl
                << "  Duration=" << e.Result->Duration() << std::endl
                << "  UserId=" << e.Result->UserId << std::endl;
            if (e.Result->Text.length() > 0)
            {
                SendToTCPServer("ed>" + e.Result->UserId + " : " + e.Result->Text + "\n");
            }
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    recognizer->Canceled.Connect([&recognitionEnd](const ConversationTranscriptionCanceledEventArgs& e)
    {
        switch (e.Reason)
        {
        case CancellationReason::EndOfStream:
            cout << "CANCELED: Reached the end of the conversation." << std::endl;
            break;

        case CancellationReason::Error:
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << std::endl;
            cout << "CANCELED: ErrorDetails=" << e.ErrorDetails << std::endl;

            SendToTCPServer("cancel>" + e.ErrorDetails + "\n");

            recognitionEnd.set_value();
            break;

        default:
            cout << "unknown reason ?!" << std::endl;
        }
    });

    recognizer->SessionStopped.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSION: " << e.SessionId << " stopped." << std::endl;
        recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts transcribing.
    recognizer->StartTranscribingAsync().wait();

    // Waits for transcribing to end.
    recognitionEnd.get_future().wait();

    // Stops transcribing. This is optional.
    recognizer->StopTranscribingAsync().wait();
}
#pragma endregion

#pragma region DialogServiceSamples
// Custom Commands dialog with keyword using microphone.
void CustomCommandsWithKeywordRecognitionWithMicrophone()
{
    // Audio input config
    auto audioInput = AudioConfig::FromDefaultMicrophoneInput();

    // Create Custom Commands config
    auto config = CustomCommandsConfig::FromSubscription(YourSpeechCustomCommandsAppId, YourSpeechDialogSubscriptionKey, YourSpeechDialogRegion);
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates a DialogServiceConnector from custom commands config.
    auto connector = DialogServiceConnector::FromConfig(config, audioInput);
    connector->ConnectAsync();

    // Creates an instance of a keyword recognition model. Update this to
    // point to the location of your keyword recognition model.
    auto model = KeywordRecognitionModel::FromFile(KeywordTable);

    // The phrase your keyword recognition model triggers on.
    auto keyword = Keyword;

    // Promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    connector->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizingSpeech)
        {
            cout << "RECOGNIZING: Text=" << e.Result->Text << std::endl;
            SendToTCPServer("ing>" + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::RecognizingKeyword)
        {
            cout << "RECOGNIZING KEYWORD: Text=" << e.Result->Text << std::endl;
        }
    });

    connector->Recognized.Connect([&recognitionEnd](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedKeyword)
        {
            cout << "RECOGNIZED KEYWORD: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
            SendToTCPServer(string("ed>") + "Question: " + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            //cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    connector->ActivityReceived.Connect([&connector](const ActivityReceivedEventArgs& e)
    {
        if (json::parse(e.GetActivity()).contains("text"))
        {
            cout << "Answer: " << json::parse(e.GetActivity())["text"] << std::endl;
            SendToTCPServer(string("ed>") + "Answer: " + string(json::parse(e.GetActivity())["text"].get<string>()) + "\n");
        }

        if (e.HasAudio())
        {
            //playAudioStream()
        }

        if (json::parse(e.GetActivity()).contains("inputHint"))
        {
            string hint = string(json::parse(e.GetActivity())["inputHint"].get<string>());
            if (hint == "expectingInput")
            {
                connector->ListenOnceAsync();
            }
        }
    });

    connector->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;

            SendToTCPServer("cancel>" + e.ErrorDetails + "\n");
        }
    });

    connector->SessionStarted.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTARTED: SessionId=" << e.SessionId << std::endl;
    });

    connector->SessionStopped.Connect([&recognitionEnd, &connector](const SessionEventArgs& e)
    {
        cout << "SESSIONSTOPPED: SessionId=" << e.SessionId << std::endl;

        //connector->ListenOnceAsync();
        //recognitionEnd.set_value(); // Notify to stop recognition.
    });

    // Starts continuous recognition. Use StopContinuousRecognitionAsync() to stop recognition.
    connector->StartKeywordRecognitionAsync(model).get(); // keyword looks consume too much memory on DDK2

    //cout << "Say something starting '" << keyword
    //  << "' followed by command like turn on TV..." << std::endl;
    cout << "Say something like turn on the TV..." << std::endl;
    connector->ListenOnceAsync();

    // Waits for a single successful keyword-triggered speech recognition (or error).
    recognitionEnd.get_future().get();

    // Stops recognition.
    connector->StopKeywordRecognitionAsync().get();
}

// Direct Line dialog with keyword using microphone.
void DirectLineWithKeywordRecognitionWithMicrophone()
{
    // Audio Input config
    auto audioInput = AudioConfig::FromDefaultMicrophoneInput();

    // Create direct line config
    auto config = BotFrameworkConfig::FromSubscription(YourSpeechDialogSubscriptionKey, YourSpeechDialogRegion, "");
    config.get()->SetProperty("DeviceGeometry", DeviceGeometry);
    config.get()->SetProperty("SelectedGeometry", SelectedGeometry);

    // Creates a DialogServiceConnector from direct line config.
    auto connector = DialogServiceConnector::FromConfig(config, audioInput);
    connector->ConnectAsync();

    // Promise for synchronization of recognition end.
    promise<void> recognitionEnd;

    // Subscribes to events.
    connector->Recognizing.Connect([](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizingSpeech)
        {
            cout << "RECOGNIZING: Text=" << e.Result->Text << std::endl;
            SendToTCPServer("ing>" + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::RecognizingKeyword)
        {
            cout << "RECOGNIZING KEYWORD: Text=" << e.Result->Text << std::endl;
        }
    });

    connector->Recognized.Connect([&recognitionEnd](const SpeechRecognitionEventArgs& e)
    {
        if (e.Result->Reason == ResultReason::RecognizedKeyword)
        {
            cout << "RECOGNIZED KEYWORD: Text=" << e.Result->Text << std::endl;
        }
        else if (e.Result->Reason == ResultReason::RecognizedSpeech)
        {
            cout << "RECOGNIZED: Text=" << e.Result->Text << std::endl;
            SendToTCPServer(string("ed>") + "Question: " + e.Result->Text + "\n");
        }
        else if (e.Result->Reason == ResultReason::NoMatch)
        {
            //cout << "NOMATCH: Speech could not be recognized." << std::endl;
        }
    });

    connector->ActivityReceived.Connect([&connector](const ActivityReceivedEventArgs& e)
    {
        if (json::parse(e.GetActivity()).contains("text"))
        {
            cout << "Answer: " << json::parse(e.GetActivity())["text"] << std::endl;
            //content[0] = introduction;
            string activityText = string(json::parse(e.GetActivity())["text"].get<string>());
            // workaround for a continue Echo: return
            if (activityText != "Echo: ")
            {
                if (activityText != "Hello and welcome!")
                {
                    SendToTCPServer(string("ed>") + "Answer: " + activityText + "\n");
                }
                else
                {
                    // update the first line to introduction
                    SendToTCPServer(string("ed>") + "Greeting: " + activityText + "\n");
                }
            }
        }

        if (e.HasAudio())
        {
            //playAudioStream()
        }
    });

    connector->Canceled.Connect([&recognitionEnd](const SpeechRecognitionCanceledEventArgs& e)
    {
        cout << "CANCELED: Reason=" << (int)e.Reason << std::endl;

        if (e.Reason == CancellationReason::Error)
        {
            cout << "CANCELED: ErrorCode=" << (int)e.ErrorCode << "\n"
                << "CANCELED: ErrorDetails=" << e.ErrorDetails << "\n"
                << "CANCELED: Did you update the subscription info?" << std::endl;

            SendToTCPServer("cancel>" + e.ErrorDetails + "\n");
        }
    });

    connector->SessionStarted.Connect([&recognitionEnd](const SessionEventArgs& e)
    {
        cout << "SESSIONSTARTED: SessionId=" << e.SessionId << std::endl;
    });

    connector->SessionStopped.Connect([&recognitionEnd, &connector](const SessionEventArgs& e)
    {
        cout << "SESSIONSTOPPED: SessionId=" << e.SessionId << std::endl;

        //connector->ListenOnceAsync();
        //recognitionEnd.set_value(); // Notify to stop recognition.
    });

    //cout << "Say something starting with '" << keyword
    //  << "' followed by any sentence, echo bot will repeat it..." << std::endl;
    cout << "Say something, echo bot will repeat it..." << std::endl;
    connector->ListenOnceAsync();

    // Waits for a single successful keyword-triggered speech recognition (or error).
    recognitionEnd.get_future().get();

    // Stops recognition.
    connector->StopKeywordRecognitionAsync().get();
}
#pragma endregion

void SpeechSamples(string arg)
{
    string input;
    do
    {
        cout << "\nSPEECH RECOGNITION SAMPLES:\n";
        cout << "1.) Speech recognition with microphone input.\n";
        cout << "2.) Speech continuous recognition with microphone input.\n";
        cout << "3.) Speech continuous recognition with file input.\n";
        cout << "4.) Speech recognition in the specified language and using detailed output format.\n";
        cout << "5.) Speech recognition with auto detection for source language\n";
        cout << "6.) Speech recognition using microphone with a keyword trigger.\n";
        cout << "7.) Speech continuous recognition using microphone with a keyword trigger and save to file.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }
        //input = "2";

        switch (input[0])
        {
        case '1':
            SpeechRecognitionWithMicrophone();
            break;
        case '2':
            SpeechContinuousRecognitionWithMicrophone();
            break;
        case '3':
            SpeechContinuousRecognitionWithFile();
            break;
        case '4':
            SpeechRecognitionWithLanguageAndUsingDetailedOutputFormat();
            break;
        case '5':
            SpeechRecognitionWithSourceLanguageAutoDetection();
            break;
        case '6':
            KeywordTriggeredSpeechRecognitionWithMicrophone();
            break;
        case '7':
            KeywordContinuousTriggeredSpeechRecognitionWithMicrophoneSaveToFile();
            break;
        case '0':
            break;
        }
    } while (input[0] != '0');
}

void IntentSamples(string arg)
{
    string input;
    do
    {
        cout << "\nINTENT RECOGNITION SAMPLES:\n";
        cout << "1.) Intent recognition with microphone input.\n";
        cout << "2.) Intent with keyword trigger recognition using microphone.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }

        switch (input[0])
        {
        case '1':
            IntentRecognitionWithMicrophone();
            break;
        case '2':
            IntentWithKeywordRecognitionWithMicrophone();
            break;
        case '0':
            break;
        }
    } while (input[0] != '0');
}

void TranslationSamples(string arg)
{
    string input;
    do
    {
        cout << "\nTRANSLATION SAMPLES:\n";
        cout << "1.) Translation with microphone input.\n";
        cout << "2.) Translation continuous recognition.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }

        switch (input[0])
        {
        case '1':
            TranslationWithMicrophone();
            break;
        case '2':
            TranslationContinuousRecognition();
            break;
        case '0':
            break;
        }
    } while (input[0] != '0');
}

void SpeechSynthesisSamples(string arg)
{
    string input;
    do
    {
        cout << "\nSPEECH SYNTHESIS SAMPLES:\n";
        cout << "1.) Speech synthesis to speaker output.\n";
        cout << "2.) Speech synthesis with specified language.\n";
        cout << "3.) Speech synthesis with specified voice.\n";
        cout << "4.) Speech synthesis to wave file.\n";
        cout << "5.) Speech synthesis to mp3 file.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }

        switch (input[0])
        {
        case '1':
            SpeechSynthesisToSpeaker();
            break;
        case '2':
            SpeechSynthesisWithLanguage();
            break;
        case '3':
            SpeechSynthesisWithVoice();
            break;
        case '4':
            SpeechSynthesisToWaveFile();
            break;
        case '5':
            SpeechSynthesisToMp3File();
            break;
        case '0':
            break;
        }
    } while (input[0] != '0');
}

void ConversationTranscriberSamples(string arg)
{
    string input;
    do
    {
        cout << "\nConversationTranscriber SAMPLES:\n";
        cout << "1.) ConversationTranscriber with pull input audio stream.\n";
        cout << "2.) ConversationTranscriber with push input audio stream.\n";
        cout << "3.) ConversationTranscriber with microphone input audio stream.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }

        switch (input[0])
        {
        case '1':
            ConversationWithPullAudioStream();
            break;
        case '2':
            ConversationWithPushAudioStream();
            break;
        case '3':
            ConversationWithMicrophoneAudioStream();
            break;
        }
    } while (input[0] != '0');
}

void DialogSamples(string arg)
{
    string input;
    do
    {
        cout << "\nDialog SAMPLES:\n";
        cout << "1.) Custom commands dialog from microphone.\n";
        cout << "2.) Direct line dialog from microphone.\n";
        cout << "\nChoice (0 for MAIN MENU): ";
        cout.flush();

        input.empty();
        if (arg == "")
        {
            getline(cin, input);
        }
        else
        {
            input = arg;
        }

        switch (input[0])
        {
        case '1':
            CustomCommandsWithKeywordRecognitionWithMicrophone();
            break;
        case '2':
            DirectLineWithKeywordRecognitionWithMicrophone();
            break;
        }
    } while (input[0] != '0');
}

void RetrieveConfigInfo()
{
    // read a JSON file
    std::ifstream i(configFilePath, std::ifstream::in);
    json j;
    i >> j;

    // read each property, if the value is not empty, update the global variables
    if (j["YourSubscriptionKey"].get<string>().length() > 0)
    {
        YourSubscriptionKey = j["YourSubscriptionKey"].get<string>();
    }

    if (j["YourServiceRegion"].get<string>().length() > 0)
    {
        YourServiceRegion = j["YourServiceRegion"].get<string>();
    }

    if (j["YourLanguageUnderstandingSubscriptionKey"].get<string>().length() > 0)
    {
        YourLanguageUnderstandingSubscriptionKey = j["YourLanguageUnderstandingSubscriptionKey"].get<string>();
    }

    if (j["YourLanguageUnderstandingServiceRegion"].get<string>().length() > 0)
    {
        YourLanguageUnderstandingServiceRegion = j["YourLanguageUnderstandingServiceRegion"].get<string>();
    }

    if (j["YourLanguageUnderstandingAppId"].get<string>().length() > 0)
    {
        YourLanguageUnderstandingAppId = j["YourLanguageUnderstandingAppId"].get<string>();
    }

    if (j["YourCTSSubscriptionKey"].get<string>().length() > 0)
    {
        YourCTSSubscriptionKey = j["YourCTSSubscriptionKey"].get<string>();
    }

    if (j["YourCTSServiceRegion"].get<string>().length() > 0)
    {
        YourCTSServiceRegion = j["YourCTSServiceRegion"].get<string>();
    }

    if (j["YourSpeechCustomCommandsAppId"].get<string>().length() > 0)
    {
        YourSpeechCustomCommandsAppId = j["YourSpeechCustomCommandsAppId"].get<string>();
    }

    if (j["YourSpeechDialogSubscriptionKey"].get<string>().length() > 0)
    {
        YourSpeechDialogSubscriptionKey = j["YourSpeechDialogSubscriptionKey"].get<string>();
    }

    if (j["YourSpeechDialogRegion"].get<string>().length() > 0)
    {
        YourSpeechDialogRegion = j["YourSpeechDialogRegion"].get<string>();
    }

    if (j["DeviceGeometry"].get<string>().length() > 0)
    {
        DeviceGeometry = j["DeviceGeometry"].get<string>();
    }

    if (j["SelectedGeometry"].get<string>().length() > 0)
    {
        SelectedGeometry = j["SelectedGeometry"].get<string>();
    }

    if (j["Keyword"].get<string>().length() > 0)
    {
        Keyword = j["Keyword"].get<string>();
        KeywordTable = "kws_" + Keyword + ".table";
    }

    if (j["PARTICIPANTSLIST"].size() > 0)
    {
        AddedVoiceSignature = true;
    }
}

int main(int argc, char **argv)
{
    // read the configure info from config.json file
    RetrieveConfigInfo();

    string input;
    string secondInput;
    do
    {
        cout << "\nMAIN MENU\n";
        cout << "1.) Speech recognition samples.\n";
        cout << "2.) Intent recognition samples.\n";
        cout << "3.) Translation samples.\n";
        cout << "4.) Speech synthesis samples.\n";
        cout << "5.) Conversation transcriber samples.\n";
        cout << "6.) Dialog samples.\n";
        cout << "\nChoice (0 to Exit): ";
        cout.flush();

        input.empty();
        if (argc == 5)
        {
            input = argv[1];
            secondInput = argv[2];
            ServerName = argv[3];
            Port = stoi(argv[4]);
        }
        else
        {
            getline(cin, input);
            secondInput = "";
        }
        //input = "1";

        switch (input[0])
        {
        case '1':
            SpeechSamples(secondInput);
            break;
        case '2':
            IntentSamples(secondInput);
            break;
        case '3':
            TranslationSamples(secondInput);
            break;
        case '4':
            SpeechSynthesisSamples(secondInput);
            break;
        case '5':
            ConversationTranscriberSamples(secondInput);
            break;
        case '6':
            DialogSamples(secondInput);
            break;
        case '0':
            break;
        }
    } while (input[0] != '0');
}