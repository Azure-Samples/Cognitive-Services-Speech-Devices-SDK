# Speech Devices SDK sample code walk through for Android

In this walk through, we will discuss demonstrate 6 six typical user scenarios of Speech Devices SDK. In each scenario, you will need to create a ```SpeechFactory``` and set PMA microphones’ parameters first.
```java
// Cognitive Serivces Speech API subscribtion info
final String SpeechSubscriptionKey = "<enter your subscription info here>";
final String SpeechRegion = "westus"; // You can change this, if you want to test the intent, and your LUIS region is different.

// Create s SpeechFactory
final SpeechFactory speechFactory = SpeechFactory.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

// Set PMA geometry parameters
final String DeviceGeometry = "Circular6+1";
final String SelectedGeometry = "Circular6+1";

private SpeechConfig getSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

        // PMA parameters
        speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
        speechConfig.setProperty("SelectedGeometry", SelectedGeometry);

        return speechConfig;
    }

Also, we've defined a helper method to handle async ```Task```.
```java
private <T> void setOnTaskCompletedListener(Task<T> task, OnTaskCompletedListener<T> listener) {
    TaskRunner<T> taskRunner = new TaskRunner<T>() {
        private T result;

        @Override
        public void run() {
            result = task.get();
            listener.onCompleted(result);
        }

        @Override
        public T result() {
            return result;
        }
    };

    new Task<>(taskRunner);
}
```

1. Recognize a signle sentence. This is best for sending a command or non-user facing scenarios. 

```java
// Create a SpeechRecognizer
final SpeechRecognizer reco = new SpeechRecognizer(this.getSpeechConfig(), this.getAudioConfig());
                

// Start recognition
final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();

// Set callback for recognizer returned
setOnTaskCompletedListener(task, result -> {
    final String s = result.getText();
    reco.close();
    Log.i(logTag, "Recognizer returned: " + s);
    // your code goes here
    // ...
});
```

2. Recognize a signle sentence with intermediate results. This is good for user facing scenarios, that would provide the users feedback during their speech. 
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

3. Recognize continuously. Recognize your continuous speech with the intermediate results.
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

4. Recognize with a wakeup word. Recognize your wakeup word and the following speech.

```java
// wakeup word
final String Keyword = "Computer";
// wakeup word model file path
private static final String KeywordModel = "kws-computer.zip";

// Create a SpeechRecognizer
reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());

// Set callback for session events
reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
						// wakeup word detected
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
 });

// Set callback for intermediate results
reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
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

5. Recognize Intent. Recognize user’s intent of the speech. This needs a LUIS subscription key and a LUIS model.

<details>
<summary>Create ```IntentRecognizer``` with LUIS subscription</summary>
<p>

```java
// LUIS subscription info
final String LuisSubscriptionKey = "<enter your subscription info here>";
final String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your LUIS region is different.
final String LuisAppId = "<enter your LUIS AppId>";

// Create a IntentRecognizer
 final SpeechConfig speechIntentConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
 IntentRecognizer reco = new IntentRecognizer(speechIntentConfig, this.getAudioConfig());
```

</p>
</details>

// Create a LanguageUnderstandingModel
LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);

// Add intents to IntentRecognizer
// reco.addIntent(...)

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

6. Recognize intent with wakeup word. Recognize your wakeup word and the intent of the following speech. This needs a LUIS subscription key and a LUIS model.

```java
final SpeechConfig intentSpeechConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
reco = new IntentRecognizer(intentSpeechConfig, getAudioConfig());

// Create a LanguageUnderstandingModel
LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);

// Add intents to IntentRecognizer
// reco.addIntent(...)

// Set callback for session events
reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
						// wakeup word detected
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
                       });

// Set callback for intermediate results
reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
                         // your code goes here
						// ...
                    });


// Set callback for recognizer returned
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
