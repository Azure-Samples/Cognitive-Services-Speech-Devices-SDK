package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

public class Player {

    private Synthesizer synthesizer;

    public Player() {
        synthesizer = new Synthesizer("<enter your subscription info here>");
        Voice voice = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, Guy24kRUS)", Voice.Gender.Female, true);
        synthesizer.SetVoice(voice, null);
    }

    public void playText(final String text){
        // Use a string for speech.
        synthesizer.SpeakToAudio(text);
    }

}
