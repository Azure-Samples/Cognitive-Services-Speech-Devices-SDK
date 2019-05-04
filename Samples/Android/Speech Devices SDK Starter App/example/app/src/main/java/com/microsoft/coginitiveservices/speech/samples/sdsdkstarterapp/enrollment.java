package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

import android.content.Intent;
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
import com.microsoft.cognitiveservices.speech.conversation.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.conversation.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.conversation.Participant;
import com.microsoft.cognitiveservices.speech.conversation.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp.LanguageCode.getCode;

public class enrollment extends AppCompatActivity {
    private HashMap<String, String> signatureMap = new HashMap<>();
    private HashMap<String, String> colorMap = new HashMap<>();
    private MenuItem meetingItem;
    private TextView meetingTextView;
    public static final String CTSKey = "<enter CTS key here>";
    static final int SELECT_MEETING_RECOGNIZE_LANGUAGE_REQUEST = 0;
    private SpeechConfig speechConfig = null;
    public static final String inroomEndpoint = "<enter your speech cognition server endpoint info here>";
    private final ArrayList<String> attendeeName = new ArrayList<>();
    private final String logTag = "Meeting";
    private boolean meetingStarted = false;
    private ConversationTranscriber transcriber = null;
    private final HashMap<Pair<String, BigInteger>, ConversationTranscriptionEventArgs> transcriptions = new HashMap<>();
    private TranscriptionAdapter messageAdapter;
    private ListView messagesView;
    private Player player;

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.enrollmentmenu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back: {
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            }
            case R.id.startOrStopMeeting: {
                if (meetingStarted) {
                    stopClicked();
                    switchMeetingStatus("Start Meeting", item);
                    meetingStarted = false;
                    player.playText("Meeting Ended");
                    return true;
                }
                else
                {
                    player.playText("Meeting Started");
                }

                clearTextBox();
                speechConfig = SpeechConfig.fromEndpoint(URI.create(inroomEndpoint), CTSKey);
                speechConfig.setProperty("DeviceGeometry", "Circular6+1");
                speechConfig.setProperty("SelectedGeometry", "Raw");
                try {
                    transcriber = new ConversationTranscriber(speechConfig, AudioConfig.fromDefaultMicrophoneInput());


                    transcriber.setConversationId("MeetingTest");
                    Log.i(logTag, "Participants enrollment");

                    String[] keyArray = signatureMap.keySet().toArray(new String[signatureMap.size()]);
                    colorMap.put("?", getRandomColor());
                    for (int i = 1; i <= signatureMap.size(); i++)
                    {
                        while (colorMap.size() < i + 1)
                        {
                            colorMap.put(keyArray[i - 1], getRandomColor());
                        }
                    }

                    // add by user Id
                    for (String userId : signatureMap.keySet()) {
                        User user = User.fromUserId(userId);
                        transcriber.addParticipant(user);
                        Participant participant = Participant.from(userId, "en-US", signatureMap.get(userId));
                        transcriber.addParticipant(participant);
                        Log.i(logTag, "add attendee: " + userId);
                    }
                    startRecognizeMeeting(transcriber);
                    switchMeetingStatus("Stop Meeting", item);
                    meetingStarted = true;
                }
                catch (Exception ex) {
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
        setContentView(R.layout.activity_enrollment);
        Toolbar toolbar = findViewById(R.id.meetingToolbar);
        meetingItem = findViewById(R.id.startOrStopMeeting);
        meetingTextView = findViewById(R.id.meetingTextView);
        meetingTextView.setMovementMethod(new ScrollingMovementMethod());
        setSupportActionBar(toolbar);
        Properties prop = new Properties();
        InputStream attendeeIs = null;
        String attendeeList = "";
        messageAdapter = new TranscriptionAdapter(this);
        messagesView = findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);
        player = new Player();


        //add attendees
        try {
            attendeeIs = new FileInputStream("/video/attendee.properties");
            prop.load(attendeeIs);
            //test case
            attendeeList = prop.getProperty("ATTENDEELIST");
        }
        catch (Exception io) {
            io.printStackTrace();
        }
        finally{
            if (attendeeIs != null) {
                try {
                    attendeeIs.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (attendeeList.length() == 0) {
            Log.i(logTag, "Please add attendees in /video/attendees.properties");
        }
        else {
            while (attendeeList.length() != 0) {
                String aName = attendeeList.substring(attendeeList.indexOf('<') + 1, attendeeList.indexOf('@'));
                String aSign = attendeeList.substring(attendeeList.indexOf('@') + 1, attendeeList.indexOf('>'));
                attendeeName.add(aName);
                signatureMap.put(aName, aSign);
                Log.i(logTag, aName);
                Log.i(logTag, aSign);
                attendeeList = attendeeList.substring(attendeeList.indexOf('>') + 1);
            }
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void eventHandler(ConversationTranscriptionEventArgs e)
    {
        final String text = e.getResult().getText();
        final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "..." : e.getResult().getUserId();
        final BigInteger offset = e.getResult().getOffset();

        //==========DEBUG
        final String result = speakerId + ": " + text;
        Log.i(logTag, "TransResult " + "Intermediate" + " result received: " + result + "; Tick: " + offset);
        //==========END DEBUG

        Pair<String, BigInteger> key = new Pair<>(speakerId, offset);
        if (text.isEmpty() || speakerId.equals("$ref$"))
        {
            transcriptions.remove(key);
        }
        else
        {
            if (transcriptions.containsKey(key))
            {
                if (transcriptions.get(key).getResult().getReason() == ResultReason.RecognizedSpeech)
                {
                    Log.e(logTag, "Two utterances occur at the same time. Offset: " + offset + "; text: " + text);
                }
            }
            transcriptions.put(key, e);
        }

        setRecognizedText(transcriptions);
    }

    private void startRecognizeMeeting(ConversationTranscriber t) throws InterruptedException, ExecutionException, TimeoutException{
        try {
            t.sessionStarted.addEventListener((o, e) -> {
                Log.i(logTag, "Session started event. Start recognition");
            });

            t.recognizing.addEventListener((o, e) -> {
                eventHandler(e);
            });

            t.recognized.addEventListener((o, e) -> {
                final String text = e.getResult().getText();
                final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "Guest" : e.getResult().getUserId();
                final BigInteger offset = e.getResult().getOffset();

                //==========DEBUG
                final String result = speakerId + " : " + text;
                Log.i(logTag, "TransResult Recognized result received: " + result + "; Tick: " + offset);
                //==========END DEBUG

                if (!text.isEmpty() && !speakerId.equals("$ref$"))
                {
                    final SpeakerData data = new SpeakerData(speakerId, colorMap.get(speakerId.equals("Guest") ? "?" : speakerId));
                    final Transcription message = new Transcription(text, data, offset);
                    runOnUiThread(() ->
                    {
                        messageAdapter.add(message);
                        messagesView.setSelection(messagesView.getCount() - 1);
                    });
                }
            });

            t.canceled.addEventListener((o, e) -> {
                Log.i(logTag, "CANCELED: Reason=" + e.getReason());
                Log.i(logTag, "CANCELED: ErrorCode=" + e.getErrorCode());
                Log.i(logTag, "CANCELED: ErrorDetails=" + e.getErrorDetails());
            });

            t.sessionStopped.addEventListener((o, e) -> {
                Log.i(logTag, "Session stopped event. Stop recognition");
            });

            transcriptions.clear();
            messageAdapter.clear();

            final Future<Void> task = t.startTranscribingAsync();
            setOnTaskCompletedListener(task, result -> {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                Log.i(logTag, "Recognition started. " + currentTime);
            });
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
        }
    }

    private void stopClicked() {
        try {
            final Future<Void> task = transcriber.stopTranscribingAsync();
            setOnTaskCompletedListener(task, result -> {
                Log.i(logTag, "Recognition stopped.");
            });
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
        }
    }

    private String getRandomColor() {
        Random r = new Random();
        StringBuffer sb = new StringBuffer("#");
        while (sb.length() < 7) {
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, 7);
    }

    private void clearTextBox() {
        appendTextLine("", true);
    }

    private void setRecognizedText(final HashMap<Pair<String, BigInteger>, ConversationTranscriptionEventArgs> trans) {
        ArrayList<Pair<BigInteger, String>> outputEvent = new ArrayList<>();

        for (ConversationTranscriptionEventArgs event : transcriptions.values())
        {
            final String speakerId = event.getResult().getUserId().equals("Unidentified") ? "..." : event.getResult().getUserId();
            final BigInteger offset = event.getResult().getOffset();
            outputEvent.add(new Pair<>(offset, speakerId + " : " + event.getResult().getText()));
        }

        Collections.sort(outputEvent, (bigIntegerStringPair, t1)->bigIntegerStringPair.first.compareTo(t1.first));

        ArrayList<String> outputMessage = new ArrayList<>();
        for (Pair<BigInteger, String> event : outputEvent)
        {
            outputMessage.add(event.second);
        }
        appendTextLine(TextUtils.join("\n", outputMessage), true);
    }

    private void appendTextLine(final String s, final Boolean erase) {
        enrollment.this.runOnUiThread(() -> {
            if (erase) {
                meetingTextView.setText(s);
            }
            else {
                String txt = meetingTextView.getText().toString();
                meetingTextView.setText(txt + "\n" + s);
            }

            final Layout layout = meetingTextView.getLayout();
            if (layout != null) {
                int scrollDelta = layout.getLineBottom(meetingTextView.getLineCount() - 1)
                        - meetingTextView.getScrollY() - meetingTextView.getHeight();
                if (scrollDelta > 0)
                    meetingTextView.scrollBy(0, scrollDelta);
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
        meetingTextView.setText(ex.getMessage() + "\n" + TextUtils.join("\n", ex.getStackTrace()));
    }
   
}