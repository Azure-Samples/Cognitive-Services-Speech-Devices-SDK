package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import static com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp.MainActivity.SELECT_RECOGNIZE_LANGUAGE_REQUEST;
import static com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp.MainActivity.SELECT_TRANSLATE_LANGUAGE_REQUEST;

public class ListLanguage extends AppCompatActivity {
    private ListView listViewlanguage;
    private final String[] recolanguage = {"English (United States)", "German (Germany)", "Chinese (Mandarin, simplified)", "English (India)", "Spanish (Spain)", "French (France)", "Italian (Italy)", "Portuguese (Brazil)", "Russian (Russia)"};
    private final String[] tranlanguage = {"Afrikaans", "Arabic", "Bangla", "Bosnian (Latin)", "Bulgarian", "Cantonese (Traditional)", "Catalan", "Chinese Simplified", "Chinese Traditional", "Croatian", "Czech", "Danish", "Dutch", "English", "Estonian", "Fijian", "Filipino", "Finnish", "French", "German", "Greek", "Haitian Creole", "Hebrew", "Hindi", "Hmong Daw", "Hungarian", "Indonesian", "Italian", "Japanese", "Kiswahili", "Klingon", "Klingon (plqaD)", "Korean", "Latvian", "Lithuanian", "Malagasy", "Malay", "Maltese", "Norwegian", "Persian", "Polish", "Portuguese", "Queretaro Otomi", "Romanian", "Russian", "Samoan", "Serbian (Cyrillic)", "Serbian (Latin)", "Slovak", "Slovenian", "Spanish", "Swedish", "Tahitian", "Tamil", "Thai", "Tongan", "Turkish", "Ukrainian", "Urdu", "Vietnamese", "Welsh", "Yucatec Maya"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        int recognizeOrTranslate = intent.getIntExtra("RecognizeOrTranslate", 0);
        setContentView(R.layout.activity_list_language);
        listViewlanguage = findViewById(R.id.listViewLanguage);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ArrayList<String> list = new ArrayList<String>();
        if (recognizeOrTranslate == SELECT_RECOGNIZE_LANGUAGE_REQUEST) {
            for (int i = 0; i < recolanguage.length; ++i) {
                list.add(recolanguage[i]);
            }
        }
        if (recognizeOrTranslate == SELECT_TRANSLATE_LANGUAGE_REQUEST) {
            for (int i = 0; i < tranlanguage.length; ++i) {
                list.add(tranlanguage[i]);
            }
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        listViewlanguage.setAdapter(adapter);

        listViewlanguage.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                Intent sendIntent = new Intent();
                sendIntent.putExtra("language", item);
                Log.i("reco Language List: ", item);
                setResult(RESULT_OK, sendIntent);
                finish();
            }
        });
    }
}

