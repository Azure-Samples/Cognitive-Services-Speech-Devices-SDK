package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TranscriptionAdapter extends BaseAdapter {
    private List<Transcription> transcriptions = new ArrayList<>();
    private Context context;

    TranscriptionAdapter(Context context) {
        this.context = context;
    }

    void add(Transcription transcription) {
        this.transcriptions.add(transcription);
        Collections.sort(this.transcriptions, (bigIntegerStringPair, t1) -> bigIntegerStringPair.getOffset().compareTo(t1.getOffset()));
        notifyDataSetChanged();
    }

    void clear() {
        this.transcriptions.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return transcriptions.size();
    }

    @Override
    public Object getItem(int i) {
        return transcriptions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TranscriptionView transcriptionView = new TranscriptionView();
        LayoutInflater transcriptionInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Transcription transcription = transcriptions.get(i);

        assert transcriptionInflater != null;
        convertView = transcriptionInflater.inflate(R.layout.final_transcription, null);
        String showChar = transcription.getMemberData().getSpeaker().equals("Guest") ? "?" : transcription.getMemberData().getSpeaker();

        transcriptionView.setHeadLayout(convertView.findViewById(R.id.head_layout));
        transcriptionView.setHeadPicture(convertView.findViewById(R.id.head_picture), transcription.getMemberData().getColor());
        transcriptionView.setHeadText(convertView.findViewById(R.id.head_text), showChar.substring(0, 1).toUpperCase());
        transcriptionView.setSpeaker(convertView.findViewById(R.id.speaker), transcription.getMemberData().getSpeaker());
        transcriptionView.setTranscriptionBody(convertView.findViewById(R.id.transcription_body), transcription.getText());
        transcriptionView.setTrans(convertView.findViewById(R.id.trans));

        convertView.setTag(transcriptionView);

        return convertView;
    }

}
