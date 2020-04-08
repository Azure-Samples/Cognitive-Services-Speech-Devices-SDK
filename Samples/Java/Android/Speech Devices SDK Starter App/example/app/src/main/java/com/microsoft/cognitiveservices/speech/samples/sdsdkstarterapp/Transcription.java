package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import java.math.BigInteger;

public class Transcription {
    private String text;
    private SpeakerData speakerData;
    private BigInteger offset;

    public Transcription(String text, SpeakerData data, BigInteger offset) {
        this.text = text;
        this.speakerData = data;
        this.offset = offset;
    }

    public String getText() {
        return text;
    }

    public SpeakerData getMemberData() {
        return speakerData;
    }

    public BigInteger getOffset() {
        return offset;
    }
}
