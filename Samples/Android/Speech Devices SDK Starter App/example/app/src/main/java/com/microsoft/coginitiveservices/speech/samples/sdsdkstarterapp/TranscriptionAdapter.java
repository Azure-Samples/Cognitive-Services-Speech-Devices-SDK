package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TranscriptionAdapter extends BaseAdapter {

    private List<Transcription> messages = new ArrayList<Transcription>();
    private Context context;

    TranscriptionAdapter(Context context) {
        this.context = context;
    }


    void add(Transcription transcription) {
        this.messages.add(transcription);
        Collections.sort(this.messages, (bigIntegerStringPair, t1)-> bigIntegerStringPair.getOffset().compareTo(t1.getOffset()));
        notifyDataSetChanged();
    }

    void clear()
    {
        this.messages.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return messages.size();
    }

    @Override
    public Object getItem(int i)
    {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup)
    {
        TranscriptionViewHolder holder = new TranscriptionViewHolder();
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Transcription message = messages.get(i);

        assert messageInflater != null;
        convertView = messageInflater.inflate(R.layout.final_transcription, null);
        holder.avatar = convertView.findViewById(R.id.avatar);
        holder.icon = convertView.findViewById(R.id.icon);
        holder.iconText = convertView.findViewById(R.id.icon_text);
        holder.name = convertView.findViewById(R.id.name);
        holder.messageBody = convertView.findViewById(R.id.transcription_body);
        convertView.setTag(holder);

        String showChar = message.getMemberData().getName().equals("Guest") ? "?" : message.getMemberData().getName();
        holder.iconText.setText(showChar.substring(0, 1).toUpperCase());
        holder.name.setText(message.getMemberData().getName());
        holder.messageBody.setText(message.getText());

        GradientDrawable drawable = (GradientDrawable) holder.icon.getBackground();
        drawable.setColor(Color.parseColor(message.getMemberData().getColor()));

        return convertView;
    }

}