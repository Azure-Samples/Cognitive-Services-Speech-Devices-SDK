# Speech Devices SDK sample code walk through for Android

In this walk through, we will discuss demonstrate 6 six typical user scenarios of Speech Devices SDK. In each scenario, you will need to create a ```SpeechFactory``` and set PMA microphones’ parameters first.
```java
// Cognitive Serivces Speech API subscribtion info
final String SpeechSubscriptionKey = "<enter your subscription info here>";
final String SpeechRegion = "westus"; // You can change this, if you want to test the intent, and your LUIS region is different.

// Set keyword and keyword package name, the keyword package is under folder \Samples\Android\Speech Devices SDK Starter App\example\app\src\main\assets
private static final String Keyword = "Computer";
private static final String KeywordModel = "kws-computer.zip";

// Set PMA geometry parameters
final String DeviceGeometry = "Circular6+1";
final String SelectedGeometry = "Circular6+1";

// Set audio configuration
private AudioConfig getAudioConfig() {
        if(new File(SampleAudioInput).exists()) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nInfo: Using AudioFile " + SampleAudioInput);
			// run from a file
            return AudioConfig.fromWavFileInput(SampleAudioInput);
        }
		// run from the microphone
        return AudioConfig.fromDefaultMicrophoneInput();
    }

//Set speech configuration
private SpeechConfig getSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

        // PMA parameters
        speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
        speechConfig.setProperty("SelectedGeometry", SelectedGeometry);
		// set speech recognition language 
		speechConfig.setSpeechRecognitionLanguage(languageRecognition);

        return speechConfig;
    }
```
Also, we've defined a helper method to handle async ```Task```.
```java
    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }
```

1. Recognize a signle sentence with intermediate results. This is good for user facing scenarios, that would provide the users feedback during their speech. 
```java
// Create a SpeechRecognizer
final SpeechRecognizer reco = new SpeechRecognizer(this.getSpeechConfig(), this.getAudioConfig());

// Set callback for intermediate results
reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    final String s = speechRecognitionResultEventArgs.getResult().getText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    // your code goes here
                    // ...
});

// Start recognition
final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();

// Set a callback for recognizer returned
setOnTaskCompletedListener(task, result -> {
    final String s = result.getText();
    reco.close();
    Log.i(logTag, "Recognizer returned: " + s);
    // your code goes here
    // ...
});
```

2. Recognize continuously. Recognize your continuous speech with the intermediate results.
```java
// Create a SpeechRecognizer
 reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());

// Set callback for intermediate results
reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    final String s = speechRecognitionResultEventArgs.getResult().getText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    // your code goes here
                    // ...
});

// Set callback for final results
reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Final result received: " + s);
                       // your code goes here
                       // ...
});

// Start recognition
final Future<Void> task = reco.startContinuousRecognitionAsync();

// Set callback for recognition started event
setOnTaskCompletedListener(task, result -> {
            // your code goes here
            // ...
});
```

3. Recognize with a wakeup word. Recognize your wakeup word and the following speech.

```java

// Create a SpeechRecognizer
reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());

// Set callback for session events
reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
                        // wakeup word detected
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
 });

// Set callback for intermediate results, two types of intermediate result:  RecognizingSpeech and RecognizingKeyword 
reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
						ResultReason rr =intermediateResultEventArgs.getResult().getReason();
						Log.i(logTag, "got a intermediate result: " + s + " result reason:" + rr.toString());
						if(rr == RecognizingSpeech) {
                            // your code goes here
							// ...
                        }
						if(rr == RecognizingKeyword){
							// your code goes here
							// ...
                        }
                         
                    });

// Set callback for final results, two types of final result:  RecognizedSpeech and RecognizedKeyword 
reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();
                        ResultReason rr = finalResultEventArgs.getResult().getReason();
                        Log.i(logTag, "got a final result: " + s + " result reason:" + rr.toString());
                        if(rr == RecognizedKeyword) {
                            // your code goes here
							// ...
                        }
							// your code goes here
							// ...
});

// Start recognition with wakeup word
final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel.fromStream(assets.open(KeywordModel), Keyword, true);
final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);

// Set callback for recognition started event
setOnTaskCompletedListener(task, result -> {
    Log.i(logTag, "say `" + Keyword + "`...");
    // your code goes here
    // ...
});
```

4. Recognize Intent. Recognize user’s intent of the speech. This needs a LUIS subscription key and a LUIS model.
```java
// LUIS subscription info
final String LuisSubscriptionKey = "<enter your subscription info here>";
final String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your LUIS region is different.
final String LuisAppId = "<enter your LUIS AppId>";

// Create a IntentRecognizer with LUIS subscription
 final SpeechConfig speechIntentConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
 IntentRecognizer reco = new IntentRecognizer(speechIntentConfig, this.getAudioConfig());

// Create a LanguageUnderstandingModel
LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);

// Add intents to IntentRecognizer
reco.addIntent(intentModel, entry.getValue(), entry.getKey());

// Set callback for intermediate results
reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
                         // your code goes here
                         // ...
                    });

// Start recognition
final Future<IntentRecognitionResult> task = reco.recognizeOnceAsync();

// Set callback for recognizer returned
setOnTaskCompletedListener(task, result -> {
    String s = result.getText();
    String intentId = result.getIntentId();
    Log.i(logTag, "Final result received: " + s + ", intent: " + intentId);
    // your code goes here
    // ...
});
```

5. Recognize intent with wakeup word. Recognize your wakeup word and the intent of the following speech. This needs a LUIS subscription key and a LUIS model.

```java

// Create a IntentRecognizer with LUIS subscription
final SpeechConfig intentSpeechConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
reco = new IntentRecognizer(intentSpeechConfig, getAudioConfig());

// Create a LanguageUnderstandingModel
LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);

// Add intents to IntentRecognizer
reco.addIntent(intentModel, entry.getValue(), entry.getKey());

// Set callback for session events
reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
						// wakeup word detected
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
                       });

// Set callback for intermediate results, two types of intermediate result:  RecognizingSpeech and RecognizingKeyword 
reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
                         // your code goes here
                         // ...
                    });


// Set callback for recognizer returned, two types of recognized result:  RecognizedSpeech and RecognizedKeyword 
reco.recognized.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();
                         // your code goes here
                         // ...

             });

// Start recognition
final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel.fromStream(assets.open(KeywordModel), Keyword, true);
final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);


// Set callback for recognition started event
setOnTaskCompletedListener(task, result -> {
    Log.i(logTag, "say `" + Keyword + "`...");
    // your code goes here
    // ...
});
```

6. Recognize continuously and translate

```java
// Set translate language and recognition language, and Create a TranslationRecognizer
final SpeechTranslationConfig translationSpeechConfig = SpeechTranslationConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
translationSpeechConfig.addTargetLanguage(languageRecognition);
translationSpeechConfig.addTargetLanguage(translateLanguage);
translationSpeechConfig.setSpeechRecognitionLanguage(languageRecognition);
reco = new TranslationRecognizer(translationSpeechConfig, getAudioConfig());

// Set callback for intermediate results
reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    final Map<String, String> translations = speechRecognitionResultEventArgs.getResult().getTranslations();
                    // your code goes here
                    // ...
});

// Set callback for final results
reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final Map<String, String> translations = speechRecognitionResultEventArgs.getResult().getTranslations();                        Log.i(logTag, "Final result received: " + s);
                       // your code goes here
                       // ...
});

// Start recognition
final Future<Void> task = reco.startContinuousRecognitionAsync();

// Set callback for recognition started event
setOnTaskCompletedListener(task, result -> {
    // your code goes here
    // ...
});
```
