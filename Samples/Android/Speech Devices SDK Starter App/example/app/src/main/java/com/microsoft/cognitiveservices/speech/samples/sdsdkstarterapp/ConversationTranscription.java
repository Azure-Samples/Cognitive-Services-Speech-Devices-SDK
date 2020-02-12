package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.transcription.Conversation;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.Participant;
import com.microsoft.cognitiveservices.speech.transcription.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConversationTranscription extends AppCompatActivity {
    private HashMap<String, String> signatureMap = new HashMap<>();
    private HashMap<String, Integer> colorMap = new HashMap<>();
    private TextView intermediateTextView;
    private static final String CTSKey = "<Conversation Transcription Service Key>";
    private static final String CTSRegion = "<Conversation Transcription Service Region>";// Region may be "centralus" or "eastasia"
    private SpeechConfig speechConfig = null;
    private final String logTag = "CTS";
    private boolean meetingStarted = false;
    private ConversationTranscriber transcriber = null;
    private Conversation conversation = null;
    private final HashMap<Pair<String, BigInteger>, ConversationTranscriptionEventArgs> transcriptions = new HashMap<>();
    private TranscriptionAdapter transcriptionAdapter;
    private ListView transcriptionView;
    private Menu optionMenu;

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ctsmenu, menu);
        optionMenu = menu;
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back: {
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                if (meetingStarted) {
                    stopClicked(optionMenu.findItem(R.id.startOrStopMeeting));
                }
                return true;
            }
            case R.id.startOrStopMeeting: {
                if (meetingStarted) {
                    stopClicked(item);
                    return true;
                }
                clearTextBox();
                speechConfig = speechConfig.fromSubscription(CTSKey, CTSRegion);
                speechConfig.setProperty("DeviceGeometry", "Circular6+1");
                speechConfig.setProperty("SelectedGeometry", "Raw");
                speechConfig.setProperty("ConversationTranscriptionInRoomAndOnline", "true");
                try {
                    conversation = Conversation.createConversationAsync(speechConfig, "MeetingTest").get();
                    transcriber = new ConversationTranscriber(AudioConfig.fromDefaultMicrophoneInput());

                    transcriber.joinConversationAsync(conversation);
                    Log.i(logTag, "Participants enrollment");

                    String[] keyArray = signatureMap.keySet().toArray(new String[signatureMap.size()]);
                    colorMap.put("Guest", getColor());
                    for (int i = 1; i <= signatureMap.size(); i++) {
                        while (colorMap.size() < i + 1) {
                            colorMap.put(keyArray[i - 1], getColor());
                        }
                    }

                    for (String userId : signatureMap.keySet()) {
                        Participant participant = Participant.from(userId, "en-US", signatureMap.get(userId));
                        conversation.addParticipantAsync(participant);
                        Log.i(logTag, "add participant: " + userId);
                    }
                    startRecognizeMeeting(transcriber);
                    switchMeetingStatus("End session", item);
                    meetingStarted = true;
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        Toolbar toolbar = findViewById(R.id.CTStoolbar);
        intermediateTextView = findViewById(R.id.IntermediateView);
        intermediateTextView.setMovementMethod(new ScrollingMovementMethod());
        setSupportActionBar(toolbar);
        Properties prop = new Properties();
        InputStream participantIs = null;
        String participantList = "";
        transcriptionAdapter = new TranscriptionAdapter(this);
        transcriptionView = findViewById(R.id.transcription_view);
        transcriptionView.setAdapter(transcriptionAdapter);

        ///////////////////////////////////////////////////
        // check if we have a valid key
        ///////////////////////////////////////////////////
        if (CTSKey.startsWith("<") || CTSKey.endsWith(">")) {
            appendTextLine("Error: Replace CTSKey with your actual speech subscription key and re-compile!", true);
            return;
        }

        ///////////////////////////////////////////////////
        // check if we have a valid endpoint
        ///////////////////////////////////////////////////
        if (CTSRegion.startsWith("<") || CTSRegion.endsWith(">")) {
            appendTextLine("Error: Replace CTSRegion with your actual speech subscription key's service region and re-compile!", true);
            return;
        }

        try {
            // example/participants.properties is for storing participants' voice signatures, please push the file under folder /video on DDK device.
            participantIs = new FileInputStream("/video/participants.properties");
            prop.load(participantIs);
            participantList = prop.getProperty("PARTICIPANTSLIST");
        } catch (Exception io) {
            io.printStackTrace();
        } finally {
            if (participantIs != null) {
                try {
                    participantIs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (participantList.length() == 0) {
            Log.i(logTag, "Please put participants file in /video/participants.properties");
            appendTextLine("Please save the participants' voice signatures in file named participants.properties, and push the file under folder /video", true);
        } else {
            while (participantList.length() != 0) {
                String aName = participantList.substring(participantList.indexOf('<') + 1, participantList.indexOf('@'));
                String aSign = participantList.substring(participantList.indexOf('@') + 1, participantList.indexOf('>'));
                signatureMap.put(aName, aSign);
                Log.i(logTag, aName);
                Log.i(logTag, aSign);
                participantList = participantList.substring(participantList.indexOf('>') + 1);
            }
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void recognizingEventHandler(ConversationTranscriptionEventArgs e) {
        final String text = e.getResult().getText();
        final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "..." : e.getResult().getUserId();
        final BigInteger offset = e.getResult().getOffset();

        final String result = speakerId + ": " + text;
        Log.i(logTag, "TransResult " + "Intermediate" + " result received: " + result + "; Tick: " + offset);

        Pair<String, BigInteger> key = new Pair<>(speakerId, offset);
        if (text.isEmpty() || speakerId.equals("$ref$")) {
            transcriptions.remove(key);
        } else {
            if (transcriptions.containsKey(key)) {
                if (transcriptions.get(key).getResult().getReason() == ResultReason.RecognizedSpeech) {
                    Log.e(logTag, "Two utterances occurred at the same time. Offset: " + offset + "; text: " + text);
                }
            }
            transcriptions.put(key, e);
        }
        setRecognizedText();
    }

    private void startRecognizeMeeting(ConversationTranscriber t) {
        try {
            t.sessionStarted.addEventListener((o, e) -> Log.i(logTag, "Session started event. Start recognition"));

            t.transcribing.addEventListener((o, e) -> recognizingEventHandler(e));

            t.transcribed.addEventListener((o, e) -> {
                final String text = e.getResult().getText();
                final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "Guest" : e.getResult().getUserId();
                final BigInteger offset = e.getResult().getOffset();

                final String result = speakerId + " : " + text;
                Log.i(logTag, "TransResult Recognized result received: " + result + "; Tick: " + offset);

                if (!text.isEmpty() && !speakerId.equals("$ref$")) {
                    final SpeakerData data = new SpeakerData(speakerId, colorMap.get(speakerId));
                    final Transcription transcription = new Transcription(text, data, offset);
                    runOnUiThread(() ->
                    {
                        transcriptionAdapter.add(transcription);
                        transcriptionView.setSelection(transcriptionView.getCount() - 1);
                    });
                }
            });

            t.canceled.addEventListener((o, e) -> {
                Log.i(logTag, "CANCELED: Reason=" + e.getReason() + ", ErrorCode=" + e.getErrorCode() + ", ErrorDetails=" + e.getErrorDetails());
            });

            t.sessionStopped.addEventListener((o, e) -> Log.i(logTag, "Session stopped event. Stop recognition"));

            transcriptions.clear();
            transcriptionAdapter.clear();

            final Future<Void> task = t.startTranscribingAsync();
            setOnTaskCompletedListener(task, result -> {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                Log.i(logTag, "Recognition started. " + currentTime);
            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
        }
    }

    private void stopClicked(MenuItem item) {
        try {
            final Future<Void> task = transcriber.stopTranscribingAsync();
            setOnTaskCompletedListener(task, result -> {
                Log.i(logTag, "Recognition stopped.");
                meetingStarted = false;
            });
            switchMeetingStatus("Start session", item);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
        }
    }

    private int getColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }

    private void clearTextBox() {
        appendTextLine("", true);
    }

    private void setRecognizedText(String s) {
        appendTextLine(s, true);
    }

    private void setRecognizedText() {
        ArrayList<Pair<BigInteger, String>> outputEvent = new ArrayList<>();

        for (ConversationTranscriptionEventArgs event : transcriptions.values()) {
            final String speakerId = event.getResult().getUserId().equals("Unidentified") ? "..." : event.getResult().getUserId();
            final BigInteger offset = event.getResult().getOffset();
            outputEvent.add(new Pair<>(offset, speakerId + " : " + event.getResult().getText()));
        }

        Collections.sort(outputEvent, (bigIntegerStringPair, t1) -> bigIntegerStringPair.first.compareTo(t1.first));

        ArrayList<String> outputMessage = new ArrayList<>();
        for (Pair<BigInteger, String> event : outputEvent) {
            outputMessage.add(event.second);
        }
        appendTextLine(TextUtils.join("\n", outputMessage), true);
    }

    private void appendTextLine(final String s, final Boolean erase) {
        this.runOnUiThread(() -> {
            if (erase) {
                intermediateTextView.setText(s);
            } else {
                String txt = intermediateTextView.getText().toString();
                intermediateTextView.setText(String.format("%s\n%s", txt, s));
            }

            final Layout layout = intermediateTextView.getLayout();
            if (layout != null) {
                int scrollDelta = layout.getLineBottom(intermediateTextView.getLineCount() - 1)
                        - intermediateTextView.getScrollY() - intermediateTextView.getHeight();
                if (scrollDelta > 0)
                    intermediateTextView.scrollBy(0, scrollDelta);
            }
        });
    }

    public void switchMeetingStatus(String text, MenuItem item) {
        item.setTitle(text);
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

    private void displayException(Exception ex) {
        intermediateTextView.setText(String.format("%s\n%s", ex.getMessage(), TextUtils.join("\n", ex.getStackTrace())));
    }
}
