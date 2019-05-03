package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;


import android.content.Intent;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp.LanguageCode.getCode;
import static com.microsoft.cognitiveservices.speech.ResultReason.RecognizedKeyword;
import static com.microsoft.cognitiveservices.speech.ResultReason.RecognizingSpeech;


public class MainActivity extends AppCompatActivity {

    // Subscription
    private static String SpeechSubscriptionKey = "<enter your subscription info here>";
    private static String SpeechRegion = "westus2"; // You can change this, if you want to test the intent, and your LUIS region is different.
    private static String LuisSubscriptionKey = "<enter your subscription info here>";
    private static String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your LUIS region is different.
    private static String LuisAppId = "<enter your LUIS AppId>";

    private static String Keyword = "computer";
    private static String KeywordModel = "computer.zip";

    private static String DeviceGeometry = "<enter your microphone geometry>"; //"Circular6+1", "Circular3+1", "Linear4", "Linear2"
    private static String SelectedGeometry = "<enter your select geometry>"; //"Circular6+1", "Circular3+1", "Linear4", "Linear2"


    // Note: point this to a wav file in case you don't want to
    //       use the microphone. It will be used automatically, if
    //       the file exists on disk.
    private static final String SampleAudioInput = "/data/keyword/kws-computer.wav";

    private TextView recognizedTextView;
    private Button recognizeIntermediateButton;
    private Button recognizeContinuousButton;
    private Button recognizeKwsButton;
    private Button recognizeIntentButton;
    private Button recognizeIntentKwsButton;
    private Button meetingButton;
    private Button translateButton;
    private TextView recognizeLanguageTextView;
    private TextView translateLanguageTextView;
    private Toolbar mainToolbar;
    private final HashMap<String, String> intentIdMap = new HashMap<>();
    private static String languageRecognition = "en-US";
   	private static String translateLanguage = "zh-Hans";
	static final int SELECT_RECOGNIZE_LANGUAGE_REQUEST = 0;  // The request code
    static final int SELECT_TRANSLATE_LANGUAGE_REQUEST = 1;  // The request code
    static final int Setting_keys = 2;
    static final int SELECT_DEVICE_MICROPHONE_REQUEST = 3;


    private AudioConfig getAudioConfig() {
        if(new File(SampleAudioInput).exists()) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nInfo: Using AudioFile " + SampleAudioInput);

            // run from a file
            return AudioConfig.fromWavFileInput(SampleAudioInput);
        }

        // run from the microphone
        return AudioConfig.fromDefaultMicrophoneInput();
    }

    public static SpeechConfig getSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);

        // PMA parameters
        speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
        speechConfig.setProperty("SelectedGeometry", SelectedGeometry);
        speechConfig.setSpeechRecognitionLanguage(languageRecognition);

        return speechConfig;
    }
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.settingmenu,menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item){
            switch(item.getItemId()){
                case R.id.subscriptionKey:{
                    Intent subscriptionKeyIntent = new Intent(this, subcriptionKey.class );
                    startActivityForResult(subscriptionKeyIntent, Setting_keys);
                    return true;
                }
                case R.id.RecoLanguage : {
                    Intent selectLanguageIntent = new Intent(this,listLanguage.class);
                    selectLanguageIntent.putExtra("RecognizeOrTranslate", SELECT_RECOGNIZE_LANGUAGE_REQUEST);
                    startActivityForResult(selectLanguageIntent, SELECT_RECOGNIZE_LANGUAGE_REQUEST);
                    return true;
                }
                case R.id.TranLanguage :{
                    Intent selectLanguageIntent = new Intent(this, listLanguage.class);
                    selectLanguageIntent.putExtra("RecognizeOrTranslate", SELECT_TRANSLATE_LANGUAGE_REQUEST);
                    startActivityForResult(selectLanguageIntent, SELECT_TRANSLATE_LANGUAGE_REQUEST);
                    return true;
                }
                case R.id.deviceMicrophone: {
                    Intent selectLanguageIntent = new Intent(this, listLanguage.class);
                    selectLanguageIntent.putExtra("RecognizeOrTranslate", SELECT_DEVICE_MICROPHONE_REQUEST);
                    startActivityForResult(selectLanguageIntent, SELECT_DEVICE_MICROPHONE_REQUEST);
                    return true;
                }

                default:
                    return super.onContextItemSelected(item);
        }


    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognizedTextView = findViewById(R.id.recognizedText);
        recognizeIntermediateButton = findViewById(R.id.buttonRecognizeIntermediate);
        recognizeContinuousButton = findViewById(R.id.buttonRecognizeContinuous);
        recognizeKwsButton = findViewById(R.id.buttonRecognizeKws);
        recognizeIntentButton = findViewById(R.id.buttonRecognizeIntent);
        recognizeIntentKwsButton = findViewById(R.id.buttonRecognizeIntentKws);
        recognizedTextView.setMovementMethod(new ScrollingMovementMethod());
        translateButton = findViewById(R.id.buttonTranslate);
        recognizeLanguageTextView = findViewById(R.id.textViewRecognitionLanguage);
        translateLanguageTextView = findViewById(R.id.textViewTranslateLanguage);
        meetingButton = findViewById(R.id.buttonMeeting);
        mainToolbar = findViewById(R.id.mainToolbar);

        setSupportActionBar(mainToolbar);

        ///////////////////////////////////////////////////
        // check if we have a valid key
        ///////////////////////////////////////////////////
        if (SpeechSubscriptionKey.startsWith("<") || SpeechSubscriptionKey.endsWith(">")) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nError: Replace SpeechSubscriptionKey with your actual subscription key in option menu!");
            
        }
        ///////////////////////////////////////////////////
        // check if we have a valid microphone parameter
        ///////////////////////////////////////////////////
        if(DeviceGeometry.startsWith("<") || DeviceGeometry.endsWith(">") ){
            recognizedTextView.setText(recognizedTextView.getText() + "\nError: Replace DeviceGeometry with your actual microphone parameter in option menu!");
           
        }
        if(SelectedGeometry.startsWith("<") || SelectedGeometry.endsWith(">") ){
            recognizedTextView.setText(recognizedTextView.getText() + "\nError: Replace SelectedGeometry with your actual select parameter in option menu!");
           
        }

        if (LuisSubscriptionKey.startsWith("<") || LuisSubscriptionKey.endsWith(">")) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nWarning: Replace LuisSubscriptionKey with your actual Luis subscription key to use Intents!");
        }
		
		 ///////////////////////////////////////////////////
        // check if we have a CTS key
        ///////////////////////////////////////////////////

        if (enrollment.CTSKey.startsWith("<") || enrollment.CTSKey.endsWith(">")) {
            recognizedTextView.setText(recognizedTextView.getText() + "\nWarning: Replace CTS SubscriptionKey with your actual subscription key if you use the meeting function and re-compile!");
        }
       
        if(enrollment.inroomEndpoint.startsWith("<") || enrollment.inroomEndpoint.endsWith(">") ){
            recognizedTextView.setText(recognizedTextView.getText() + "\nWarning: Replace CTS endpoint with your CTS endpoint parameter if you use the meeting function and re-compile!");
        }



        // save the asset manager
        final AssetManager assets = this.getAssets();



        ///////////////////////////////////////////////////
        // recognize Once with intermediate results
        ///////////////////////////////////////////////////
        recognizeIntermediateButton.setOnClickListener(view -> {
            final String logTag = "reco 1";
            if(!checkSystemTime()) return;
            disableButtons();
            clearTextBox();


            try {
                Log.i(logTag, languageRecognition);

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
            private static final String logTag = "reco 2";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();

            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                if(!checkSystemTime()) return;
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
                if(!checkSystemTime()) return;
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

                    });

                    reco.sessionStopped.addEventListener((o, sessionEventArgs) -> Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

                    reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        ResultReason rr =intermediateResultEventArgs.getResult().getReason();
                        Log.i(logTag, "got a intermediate result: " + s + " result reason:" + rr.toString());
                        if(rr == RecognizingSpeech) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s);
                            setRecognizedText(TextUtils.join(delimiter, content));
                        }
                    });
                    reco.recognized.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();
                        ResultReason rr = finalResultEventArgs.getResult().getReason();

                        if(rr == RecognizedKeyword) {
                            content.add("");
                        }

                        if(  !s.isEmpty() ) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s);
                            content.set(0, "say `" + Keyword + "`...");
                            setRecognizedText(TextUtils.join(delimiter, content));
                            Log.i(logTag, "got a final result: " +" " + Integer.toString(index +1) + " " + s + " result reason:" + rr.toString());
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
            if(!checkSystemTime()) return;
            disableButtons();
            clearTextBox();


            content.add("");
            content.add("");
            try {
                final SpeechConfig speechIntentConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey, LuisRegion);
                speechIntentConfig.setProperty("DeviceGeometry", DeviceGeometry);
                speechIntentConfig.setProperty("SelectedGeometry", SelectedGeometry);
                speechIntentConfig.setSpeechRecognitionLanguage(languageRecognition);
                IntentRecognizer reco = new IntentRecognizer(speechIntentConfig, getAudioConfig());

                LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                    reco.addIntent(intentModel, entry.getValue(), entry.getKey());
                    Log.i(logTag, "intent: " + entry.getValue() + " Intent Id : " + entry.getKey());
                }

                reco.recognizing.addEventListener((o, intentRecognitionResultEventArgs) -> {
                    final String s = intentRecognitionResultEventArgs.getResult().getText();
                    Log.i(logTag, "Intermediate result received: " + s);
                    content.set(0, s);
                    setRecognizedText(TextUtils.join("\n", content));
                });

                final Future<IntentRecognitionResult> task = reco.recognizeOnceAsync();
                setOnTaskCompletedListener(task, result -> {
                    Log.i(logTag, "Intent recognition stopped.");
                    String s = result.getText();

                    if (result.getReason() != ResultReason.RecognizedIntent) {
                        String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                        s = "Intent failed with " + result.getReason() + ". Did you enter your Language Understanding subscription?" + System.lineSeparator() + errorDetails;
                    }
                    String intentId = result.getIntentId();
                    Log.i(logTag, "IntentId: " + intentId);
                    String intent = "";
                    if (intentIdMap.containsKey(intentId)) {
                        intent = intentIdMap.get(intentId);
                    }

                    Log.i(logTag, "S: " + s + ", intent: " + intent);
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
                if(!checkSystemTime()) return;
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
                    intentSpeechConfig.setSpeechRecognitionLanguage(languageRecognition);
                    intentSpeechConfig.setProperty("DeviceGeometry", DeviceGeometry);
                    intentSpeechConfig.setProperty("SelectedGeometry", SelectedGeometry);
                    reco = new IntentRecognizer(intentSpeechConfig, getAudioConfig());

                    LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
                    for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                        reco.addIntent(intentModel, entry.getValue(), entry.getKey());
                        Log.i(logTag, "intent: " + entry.getValue() + " Intent Id : " + entry.getKey());
                    }

                    reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
                        Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
                        content.set(0, "KeywordModel `" + Keyword + "` detected");
                        setRecognizedText(TextUtils.join(delimiter, content));

                    });

                    reco.sessionStopped.addEventListener((o, sessionEventArgs) -> Log.i(logTag, "got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

                    reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
                        final String s = intermediateResultEventArgs.getResult().getText();
                        ResultReason rr =intermediateResultEventArgs.getResult().getReason();
                        Log.i(logTag, "got a intermediate result: " + s + " result reason:" + rr.toString());
                        if(rr == RecognizingSpeech) {
                            Integer index = content.size() - 2;
                            content.set(index + 1, index.toString() + ". " + s);
                            setRecognizedText(TextUtils.join(delimiter, content));
                        }
                    });

                    reco.recognized.addEventListener((o, finalResultEventArgs) -> {
                        String s = finalResultEventArgs.getResult().getText();
                        String intentId = finalResultEventArgs.getResult().getIntentId();
                        Log.i(logTag, "IntentId: " + intentId);
                        String intent = "";
                        if (intentIdMap.containsKey(intentId)) {
                            intent = intentIdMap.get(intentId);
                        }

                        ResultReason rr = finalResultEventArgs.getResult().getReason();
                        Log.i(logTag, "got a final result: " + s + " result reason:" + rr.toString());
                        if(rr == RecognizedKeyword) {
                            content.add("");
                        }
                        if( !s.isEmpty() ) {
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
        // Meeting
        ///////////////////////////////////////////////////
        meetingButton.setOnClickListener(view ->{
            if(!checkSystemTime()) return;
            Intent meetingIntent = new Intent(this, enrollment.class);
            startActivity(meetingIntent);

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
                if(!checkSystemTime()) return;
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
                    translationSpeechConfig.addTargetLanguage(languageRecognition);
                    translationSpeechConfig.addTargetLanguage(translateLanguage);
                    translationSpeechConfig.setSpeechRecognitionLanguage(languageRecognition);
                    translationSpeechConfig.setProperty("DeviceGeometry", DeviceGeometry);
                    translationSpeechConfig.setProperty("SelectedGeometry", SelectedGeometry);
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
            recognizeIntermediateButton.setEnabled(false);
            recognizeContinuousButton.setEnabled(false);
            recognizeKwsButton.setEnabled(false);
            recognizeIntentButton.setEnabled(false);
            recognizeIntentKwsButton.setEnabled(false);
            meetingButton.setEnabled(false);
            translateButton.setEnabled(false);
        });
    }

    private void enableButtons() {
        MainActivity.this.runOnUiThread(() -> {
            recognizeIntermediateButton.setEnabled(true);
            recognizeContinuousButton.setEnabled(true);
            recognizeKwsButton.setEnabled(true);
            recognizeIntentButton.setEnabled(true);
            recognizeIntentKwsButton.setEnabled(true);
            meetingButton.setEnabled(true);
            translateButton.setEnabled(true);
        });
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SELECT_RECOGNIZE_LANGUAGE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                String language = data.getStringExtra("languageOrDevice");
                languageRecognition = getCode(0,language);
                recognizeLanguageTextView.setText(language);
            }
        }
        if (requestCode == SELECT_TRANSLATE_LANGUAGE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                String language = data.getStringExtra("languageOrDevice");
                translateLanguage = getCode(1,language);
                translateLanguageTextView.setText(language);
            }
        }
        if (requestCode == SELECT_DEVICE_MICROPHONE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                String deviceMicrophone = data.getStringExtra("languageOrDevice");
                DeviceGeometry = deviceMicrophone;
                SelectedGeometry = deviceMicrophone;
                Log.i("Setting Microphone", deviceMicrophone);
            }
        }
        if(requestCode == Setting_keys){
            if(resultCode == RESULT_OK){
                ArrayList<String> keys = new ArrayList<>();
                keys = data.getStringArrayListExtra("keys");
                SpeechSubscriptionKey = keys.get(0);
                SpeechRegion = keys.get(1);
                LuisSubscriptionKey = keys.get(2);
                LuisRegion = keys.get(3);
                LuisAppId = keys.get(4);
            }
        }
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

    //make sure the system time is synchronized.
    public boolean checkSystemTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpledateformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date = simpledateformat.format(calendar.getTime());
        String year = date.substring(6,10);
        Log.i("System time" , date);
        if(Integer.valueOf(year) < 2018){
            Log.i("System time" , "Please synchronize system time");
            setTextbox("System time is " + date + "\n" +"Please synchronize system time");
            return false;
        }
        return true;
    }

    public static String getSpeechSubscriptionKey(){
        return SpeechSubscriptionKey;
    }
    public static String getSpeechRegion(){
        return SpeechRegion;
    }
    public static String getLuisSubscriptionKey() {return LuisSubscriptionKey;}
    public static String getLuisRegion() {return LuisRegion;}
    public static String getLuisAppId() { return LuisAppId; }

}
