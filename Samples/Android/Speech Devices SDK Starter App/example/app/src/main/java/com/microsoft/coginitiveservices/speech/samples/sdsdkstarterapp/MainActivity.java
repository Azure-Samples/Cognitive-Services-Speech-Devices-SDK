package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    // Subscription
    private static final String SpeechSubscriptionKey = "<enter your subscription info here>";
    private static final String SpeechRegion = "westus"; // You can change this, if you want to test the intent, and your LUIS region is different.
    private static final String LuisSubscriptionKey = "<enter your subscription info here>";
    private static final String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your LUIS region is different.
    private static final String LuisAppId = "<enter your LUIS AppId>";

    private static final String Keyword = "Computer";
    private static final String KeywordModel = "kws-computer.zip";
    private static final String DeviceGeometry = "Circular6+1";
    private static final String SelectedGeometry = "Circular6+1";

    // Note: point this to a wav file in case you don't want to
    //       use the microphone. It will be used automatically, if
    //       the file exists on disk.
    private static final String SampleAudioInput = "/data/keyword/kws-computer.wav";

    private TextView recognizedTextView;

    private Button recognizeButton;
    private Button recognizeIntermediateButton;
    private Button recognizeContinuousButton;
    private Button recognizeKwsButton;
    private Button recognizeIntentButton;
    private Button recognizeIntentKwsButton;
    private Button translateButton;
    private final HashMap<String, String> intentIdMap = new HashMap<>();

    private AudioConfig getAudioConfig() {
        if(new File(SampleAudioInput).exists()) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nInfo: Using AudioFile " + SampleAudioInput);

            // run from a file
            return AudioConfig.fromWavFileInput(SampleAudioInput);
        }

        // run from the microphone
        return AudioConfig.fromDefaultMicrophoneInput();
    }

    private SpeechConfig getSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

        // PMA parameters
        speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
        speechConfig.setProperty("SelectedGeometry", SelectedGeometry);

        return speechConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognizedTextView = findViewById(R.id.recognizedText);

        recognizeButton = findViewById(R.id.buttonRecognize);
        recognizeIntermediateButton = findViewById(R.id.buttonRecognizeIntermediate);
        recognizeContinuousButton = findViewById(R.id.buttonRecognizeContinuous);
        recognizeKwsButton = findViewById(R.id.buttonRecognizeKws);
        recognizeIntentButton = findViewById(R.id.buttonRecognizeIntent);
        recognizeIntentKwsButton = findViewById(R.id.buttonRecognizeIntentKws);
        recognizedTextView.setMovementMethod(new ScrollingMovementMethod());
        translateButton = findViewById(R.id.buttonTranslate);

        ///////////////////////////////////////////////////
        // check if we have a valid key
        ///////////////////////////////////////////////////
        if (SpeechSubscriptionKey.startsWith("<") || SpeechSubscriptionKey.endsWith(">")) {
            recognizedTextView.setText("Error: Replace SpeechSubscriptionKey with your actual subscription key and re-compile this application!");
            return;
        }

        if (LuisSubscriptionKey.startsWith("<") || LuisSubscriptionKey.endsWith(">")) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nWarning: Replace LuisSubscriptionKey with your actual Luis subscription key to use Intents!");
        }

        // save the asset manager
        final AssetManager assets = this.getAssets();

        ///////////////////////////////////////////////////
        // recognize
        ///////////////////////////////////////////////////
        recognizeButton.setOnClickListener(view -> {
            final String logTag = "reco 1";

            disableButtons();
            clearTextBox();


            try {
                final SpeechRecognizer reco = new SpeechRecognizer(this.getSpeechConfig(), this.getAudioConfig());
                final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();

                setOnTaskCompletedListener(task, result -> {
                    final String s = result.getText();
                    reco.close();
                    Log.i(logTag, "Recognizer returned: " + s);
                    setRecognizedText(s);
                    enableButtons();
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        });

        ///////////////////////////////////////////////////
        // recognize with intermediate results
        ///////////////////////////////////////////////////
        recognizeIntermediateButton.setOnClickListener(view -> {
            final String logTag = "reco 2";

            disableButtons();
            clearTextBox();

            try {
                final SpeechRecognizer reco = new SpeechRecognizer(this.getSpeechConfig(), this.getAudioConfig());
                reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    final String s = speechRecognitionResultEventArgs.getResult().getText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    setRecognizedText(s);
                });

                final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
                setOnTaskCompletedListener(task, result -> {
                    final String s = result.getText();
                    reco.close();
                    Log.i(logTag, "Recognizer returned: " + s);
                    setRecognizedText(s);
                    enableButtons();
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        });

        ///////////////////////////////////////////////////
        // recognize continuously
        ///////////////////////////////////////////////////
        recognizeContinuousButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "reco 3";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();

            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                disableButtons();
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> clickedButton.setText(buttonText));
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                try {
                    content.clear();
                    reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());
                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Intermediate result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                        content.remove(content.size() - 1);
                    });

                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Final result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });

        ///////////////////////////////////////////////////
        // recognize with wake word
        ///////////////////////////////////////////////////
        recognizeKwsButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "kws";
            private static final String delimiter = "\n";
            private final ArrayList<String> content = new ArrayList<>();
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private String buttonText = "";

            @Override
            public void onClick(View view) {
                final Button clickedButton = (Button) view;
                disableButtons();

                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopKeywordRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> clickedButton.setText(buttonText));
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                content.clear();
                content.add("");
                content.add("");
                try {
                    reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());
                    reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");

                        content.set(0, "KeywordModel `" + Keyword + "` detected");
                        setRecognizedText(TextUtils.join(delimiter, content));
                        content.add("");
                    });

                    reco.sessionStopped.addEventListener((o, sessionEventArgs) -> Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

                    reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
                        Integer index = content.size() - 2;
                        content.set(index + 1, index.toString() + ". " + s);
                        setRecognizedText(TextUtils.join(delimiter, content));
                    });

                    reco.recognized.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();

                        Log.i(logTag, "got a final result: " + s);
                        if (!s.isEmpty()) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s);
                            content.set(0, "say `" + Keyword + "`...");
                            setRecognizedText(TextUtils.join(delimiter, content));
                        }
                    });

                    final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel.fromStream(assets.open(KeywordModel), Keyword, true);
                    final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);
                    setOnTaskCompletedListener(task, result -> {
                        content.set(0, "say `" + Keyword + "`...");
                        setRecognizedText(TextUtils.join(delimiter, content));
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });


        intentIdMap.put("1", "play music");
        intentIdMap.put("2", "stop");

        ///////////////////////////////////////////////////
        // recognize intent
        ///////////////////////////////////////////////////
        recognizeIntentButton.setOnClickListener(view -> {
            final String logTag = "intent";
            final ArrayList<String> content = new ArrayList<>();

            disableButtons();
            clearTextBox();

            content.add("");
            content.add("");
            try {
                final SpeechConfig speechIntentConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
                IntentRecognizer reco = new IntentRecognizer(speechIntentConfig, this.getAudioConfig());

                LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                    reco.addIntent(intentModel, entry.getKey(), entry.getValue());
                }

                reco.recognizing.addEventListener((o, intentRecognitionResultEventArgs) -> {
                    final String s = intentRecognitionResultEventArgs.getResult().getText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    content.set(0, s);
                    setRecognizedText(TextUtils.join("\n", content));
                });

                final Future<IntentRecognitionResult> task = reco.recognizeOnceAsync();
                setOnTaskCompletedListener(task, result -> {
                    Log.i(logTag, "Continuous recognition stopped.");
                    String s = result.getText();
                    String intentId = result.getIntentId();
                    String intent = "";
                    if (intentIdMap.containsKey(intentId)) {
                        intent = intentIdMap.get(intentId);

                    }
                    Log.i(logTag, "Final result received: " + s + ", intent: " + intent);
                    content.set(0, s);
                    content.set(1, " [intent: " + intent + "]");
                    setRecognizedText(TextUtils.join("\n", content));
                    enableButtons();
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        });

        ///////////////////////////////////////////////////
        // recognize intent with wake word
        ///////////////////////////////////////////////////
        recognizeIntentKwsButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "kws intent";
            private static final String delimiter = "\n";
            private final ArrayList<String> content = new ArrayList<>();
            private boolean continuousListeningStarted = false;
            private IntentRecognizer reco = null;
            private String buttonText = "";

            @Override
            public void onClick(View view) {
                final Button clickedButton = (Button) view;
                disableButtons();

                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopKeywordRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> clickedButton.setText(buttonText));
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                content.clear();
                content.add("");
                content.add("");
                try {
                    final SpeechConfig intentSpeechConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
                    reco = new IntentRecognizer(intentSpeechConfig, getAudioConfig());

                    LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                    for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                        reco.addIntent(intentModel, entry.getKey(), entry.getValue());
                    }

                    reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
                        setRecognizedText(TextUtils.join(delimiter, content));
                        content.add("");
                    });

                    reco.sessionStopped.addEventListener((o, sessionEventArgs) -> Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

                    reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        Log.i(logTag, "got a intermediate result: " + s);
                        Integer index = content.size() - 2;
                        content.set(index + 1, index.toString() + ". " + s);
                        setRecognizedText(TextUtils.join(delimiter, content));
                    });

                    reco.recognized.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();
                        String intentId = finalResultEventArgs.getResult().getIntentId();
                        String intent = "";
                        if (intentIdMap.containsKey(intentId)) {
                            intent = intentIdMap.get(intentId);

                        }

                        Log.i(logTag, "got a final result: " + s);
                        if (!s.isEmpty()) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s + " [intent: " + intent + "]");
                            content.set(0, "say `" + Keyword + "`...");
                            setRecognizedText(TextUtils.join(delimiter, content));
                        }
                    });

                    final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel.fromStream(assets.open(KeywordModel), Keyword, true);
                    final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);
                    setOnTaskCompletedListener(task, result -> {
                        content.set(0, "say `" + Keyword + "`...");
                        setRecognizedText(TextUtils.join(delimiter, content));
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });

        ///////////////////////////////////////////////////
        // recognize and translate
        ///////////////////////////////////////////////////
        translateButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "reco t";
            private boolean continuousListeningStarted = false;
            private TranslationRecognizer reco = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();

            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                disableButtons();
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> clickedButton.setText(buttonText));
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                try {
                    content.clear();
                    final SpeechTranslationConfig translationSpeechConfig = SpeechTranslationConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
                    translationSpeechConfig.addTargetLanguage("en-US");
                    translationSpeechConfig.addTargetLanguage("de-DE");
                    translationSpeechConfig.setSpeechRecognitionLanguage("en-US");
                    reco = new TranslationRecognizer(translationSpeechConfig, getAudioConfig());

                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final Map<String, String> translations = speechRecognitionResultEventArgs.getResult().getTranslations();
                        StringBuffer sb = new StringBuffer();
                        for (String key : translations.keySet()) {
                            sb.append( key + " -> '" + translations.get(key) + "'\n");
                        }
                        final String s = sb.toString();

                        Log.i(logTag, "Intermediate result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                        content.remove(content.size() - 1);
                    });

                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final Map<String, String> translations = speechRecognitionResultEventArgs.getResult().getTranslations();
                        StringBuffer sb = new StringBuffer();
                        for (String key : translations.keySet()) {
                            sb.append( key + " -> '" + translations.get(key) + "'\n");
                        }
                        final String s = sb.toString();

                        Log.i(logTag, "Final result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });
    }

    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + "\n" + TextUtils.join("\n", ex.getStackTrace()));
    }

    private void clearTextBox() {
        setTextbox("");
    }

    private void setRecognizedText(final String s) {
        setTextbox(s);
    }

    private void setTextbox(final String s) {
        MainActivity.this.runOnUiThread(() -> {
            recognizedTextView.setText(s);

            final Layout layout = recognizedTextView.getLayout();
            if(layout != null) {
                int scrollDelta = layout.getLineBottom(recognizedTextView.getLineCount() - 1)
                        - recognizedTextView.getScrollY() - recognizedTextView.getHeight();
                if (scrollDelta > 0)
                    recognizedTextView.scrollBy(0, scrollDelta);
            }
        });
    }

    private void disableButtons() {
        MainActivity.this.runOnUiThread(() -> {
            recognizeButton.setEnabled(false);
            recognizeIntermediateButton.setEnabled(false);
            recognizeContinuousButton.setEnabled(false);
            recognizeKwsButton.setEnabled(false);
            recognizeIntentButton.setEnabled(false);
            recognizeIntentKwsButton.setEnabled(false);
            translateButton.setEnabled(false);
        });
    }

    private void enableButtons() {
        MainActivity.this.runOnUiThread(() -> {
            recognizeButton.setEnabled(true);
            recognizeIntermediateButton.setEnabled(true);
            recognizeContinuousButton.setEnabled(true);
            recognizeKwsButton.setEnabled(true);
            recognizeIntentButton.setEnabled(true);
            recognizeIntentKwsButton.setEnabled(true);
            translateButton.setEnabled(true);
        });
    }


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

    protected static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }
}
