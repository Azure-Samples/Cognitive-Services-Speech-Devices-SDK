package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TranscriptionView {
    private RelativeLayout head_layout;
    private View head_picture;
    private TextView head_text;
    private TextView speaker;
    private TextView transcriptionBody;
    private LinearLayout trans;

    void setHeadLayout(RelativeLayout head_layout) {
        this.head_layout = head_layout;
    }

    void setHeadPicture(View head_picture, int color) {
        this.head_picture = head_picture;
        GradientDrawable drawable = (GradientDrawable) this.head_picture.getBackground();
        drawable.setColor(color);
    }

    void setHeadText(TextView head_text, String text) {
        this.head_text = head_text;
        this.head_text.setText(text);
    }

    void setSpeaker(TextView speaker, String text) {
        this.speaker = speaker;
        this.speaker.setText(text);
    }

    void setTranscriptionBody(TextView transcriptionBody, String text) {
        this.transcriptionBody = transcriptionBody;
        this.transcriptionBody.setText(text);
    }

    void setTrans(LinearLayout trans) {
        this.trans = trans;
    }

    public RelativeLayout getHeadLayout() {
        return this.head_layout;
    }

    public View getHeadPicture() {
        return this.head_picture;
    }

    public TextView getHeadText() {
        return this.head_text;
    }

    public TextView getSpeaker() {
        return this.speaker;
    }

    public TextView getTranscriptionBody() {
        return this.transcriptionBody;
    }

    public LinearLayout getTrans() {
        return this.trans;
    }
}
