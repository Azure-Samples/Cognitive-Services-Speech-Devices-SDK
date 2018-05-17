package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.SessionEventType;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.cognitiveservices.speech.SpeechFactory;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.util.Task;
import com.microsoft.cognitiveservices.speech.util.TaskRunner;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Subscription
    private static final String SpeechSubscriptionKey = "<enter your subscription info here>";
    private static final String SpeechRegion = "westus"; // You can change this, if you want to test the intent, and your LUIS region is different.
    private static final String LuisSubscriptionKey = "<enter your subscription info here>";
    private static final String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your LUIS region is different.
    private static final String LuisAppId = "<enter your LUIS AppId>";

    private static final String Keyword = "Computer";
    private static final String KeywordModel = "/data/keyword/kws.table";

    private static final String DeviceGeometry = "Circular6+1";
    private static final String SelectedGeometry = "Circular6+1";

    private TextView recognizedTextView;

    private Button recognizeButton;
    private Button recognizeIntermediateButton;
    private Button recognizeContinuousButton;
    private Button recognizeKwsButton;
    private Button recognizeIntentButton;
    private Button recognizeIntentKwsButton;

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

        // create factory
        final SpeechFactory speechFactory;
        try {
            speechFactory = SpeechFactory.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

            // PMA parameters
            speechFactory.getParameters().set("DeviceGeometry", DeviceGeometry);
            speechFactory.getParameters().set("SelectedGeometry", SelectedGeometry);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }

        ///////////////////////////////////////////////////
        // recognize
        ///////////////////////////////////////////////////
        recognizeButton.setOnClickListener(view -> {
            final String logTag = "reco 1";

            disableButtons();
            clearTextBox();


            try {
                final SpeechRecognizer reco = speechFactory.createSpeechRecognizer();

                final Task<SpeechRecognitionResult> task = reco.recognizeAsync();
                setOnTaskCompletedListener(task, result -> {
                    final String s = result.getRecognizedText();
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
                final SpeechRecognizer reco = speechFactory.createSpeechRecognizer();

                reco.IntermediateResultReceived.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    final String s = speechRecognitionResultEventArgs.getResult().getRecognizedText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    setRecognizedText(s);
                });

                final Task<SpeechRecognitionResult> task = reco.recognizeAsync();
                setOnTaskCompletedListener(task, result -> {
                    final String s = result.getRecognizedText();
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
                        final Task<?> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
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
                    reco = speechFactory.createSpeechRecognizer();

                    reco.IntermediateResultReceived.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getRecognizedText();
                        Log.i(logTag, "Intermediate result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                        content.remove(content.size() - 1);
                    });

                    reco.FinalResultReceived.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getRecognizedText();
                        Log.i(logTag, "Final result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    });

                    final Task<?> task = reco.startContinuousRecognitionAsync();
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
                        final Task<?> task = reco.stopKeywordRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
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
                    reco = speechFactory.createSpeechRecognizer();

                    reco.SessionEvent.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: " + sessionEventArgs.getEventType());
                        if (sessionEventArgs.getEventType() == SessionEventType.SessionStartedEvent) {
                            content.set(0, "KeywordModel `" + Keyword + "` detected");
                            setRecognizedText(TextUtils.join(delimiter, content));
                            content.add("");
                        }
                    });

                    reco.IntermediateResultReceived.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getRecognizedText();
                        Log.i(logTag, "got a intermediate result: " + s);
                        Integer index = content.size() - 2;
                        content.set(index + 1, index.toString() + ". " + s);
                        setRecognizedText(TextUtils.join(delimiter, content));
                    });

                    reco.FinalResultReceived.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getRecognizedText();

                        Log.i(logTag, "got a final result: " + s);
                        if (!s.isEmpty()) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s);
                            content.set(0, "say `" + Keyword + "`...");
                            setRecognizedText(TextUtils.join(delimiter, content));
                        }
                    });

                    final Task<?> task = reco.startKeywordRecognitionAsync(KeywordRecognitionModel.fromFile(KeywordModel));
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

        final HashMap<String, String> intentIdMap = new HashMap<>();
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
                final SpeechFactory intentFactory = SpeechFactory.fromSubscription(LuisSubscriptionKey, LuisRegion);
                final IntentRecognizer reco = intentFactory.createIntentRecognizer();

                LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                    reco.addIntent(entry.getKey(), intentModel, entry.getValue());
                }

                reco.IntermediateResultReceived.addEventListener((o, intentRecognitionResultEventArgs) -> {
                    final String s = intentRecognitionResultEventArgs.getResult().getRecognizedText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    content.set(0, s);
                    setRecognizedText(TextUtils.join("\n", content));
                });

                final Task<IntentRecognitionResult> task = reco.recognizeAsync();
                setOnTaskCompletedListener(task, result -> {
                    Log.i(logTag, "Continuous recognition stopped.");
                    String s = result.getRecognizedText();
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
                        final Task<?> task = reco.stopKeywordRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
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
                    final SpeechFactory intentFactory = SpeechFactory.fromSubscription(LuisSubscriptionKey, LuisRegion);
                    final IntentRecognizer reco = intentFactory.createIntentRecognizer();

                    LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                    for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                        reco.addIntent(entry.getKey(), intentModel, entry.getValue());
                    }

                    reco.SessionEvent.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: " + sessionEventArgs.getEventType());
                        if (sessionEventArgs.getEventType() == SessionEventType.SessionStartedEvent) {
                            content.set(0, "KeywordModel `" + Keyword + "` detected");
                            setRecognizedText(TextUtils.join(delimiter, content));
                            content.add("");
                        }
                    });

                    reco.IntermediateResultReceived.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getRecognizedText();
                        Log.i(logTag, "got a intermediate result: " + s);
                        Integer index = content.size() - 2;
                        content.set(index + 1, index.toString() + ". " + s);
                        setRecognizedText(TextUtils.join(delimiter, content));
                    });

                    reco.FinalResultReceived.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getRecognizedText();
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

                    final Task<?> task = reco.startKeywordRecognitionAsync(KeywordRecognitionModel.fromFile(KeywordModel));
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
    }

    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + "\n" + TextUtils.join("\n", ex.getStackTrace()));
    }

    private void clearTextBox() {
        AppendTextLine("", true);
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, true);
    }

    private void AppendTextLine(final String s, final Boolean erase) {
        MainActivity.this.runOnUiThread(() -> {
            if (erase) {
                recognizedTextView.setText(s);
            } else {
                String txt = recognizedTextView.getText().toString();
                recognizedTextView.setText(txt + "\n" + s);
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
        });
    }

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

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }
}
