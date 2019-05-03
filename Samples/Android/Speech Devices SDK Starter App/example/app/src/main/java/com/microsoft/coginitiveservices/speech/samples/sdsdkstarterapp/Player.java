package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;

import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

import java.io.File;
import java.util.Map;

public class Player {

    private MediaPlayer mediaPlayer = null;
    private Synthesizer synthesizer;

    public Player() {
        synthesizer = new Synthesizer("e7efabfbf235419d8951f5f382e168fa");
        //Voice voice = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, BenjaminRUS)", Voice.Gender.Male, true);
        Voice voice = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, Guy24kRUS)", Voice.Gender.Female, true);
        // Voice voice = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
        synthesizer.SetVoice(voice, null);
        mediaPlayer = new MediaPlayer();
    }

    public void playText(final String text) {
        playText(text, null);
    }

    public void playText(final String text, final Runnable callback) {
        if (text == null) {
            return;
        }

        final byte[] sound = synthesizer.Speak(text);
        if (sound != null && sound.length != 0) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    AudioTrack audioTrack = new AudioTrack(3, 16000, 2, 2, AudioTrack.getMinBufferSize(16000, 2, 2), 1);
                    if (audioTrack.getState() == 1) {
                        audioTrack.play();
                        audioTrack.write(sound, 0, sound.length);
                        audioTrack.stop();
                        audioTrack.release();
                    }

                    if (callback != null) {
                        callback.run();
                    }

                }
            });
        }
    }
}
