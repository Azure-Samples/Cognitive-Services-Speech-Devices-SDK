package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

class SpeakerData {
    private String name;
    private String color;

    public SpeakerData(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public SpeakerData() {
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "SpeakerData{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
