package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

class SpeakerData {
    private String speaker;
    private int color;

    public SpeakerData(String speaker, int color) {
        this.speaker = speaker;
        this.color = color;
    }

    public String getSpeaker() {
        return speaker;
    }

    public int getColor() {
        return color;
    }
}
