package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import java.util.HashMap;
import java.util.Map;

public class LanguageCode {
    private static HashMap<String, String> mapRecolanguageCode = new HashMap<String, String>() {
        {
            put("English (United States)", "en-US");
            put("German (Germany)", "de-DE");
            put("Chinese (Mandarin, simplified)", "zh-CN");
            put("English (India)", "en-IN");
            put("Spanish (Spain)", "es-ES");
            put("French (France)", "fr-FR");
            put("Italian (Italy)", "it-IT");
            put("Portuguese (Brazil)", "pt-BR");
            put("Russian (Russia)", "ru-RU");
        }
    };
    private static HashMap<String, String> mapTranlanguageCode = new HashMap<String, String>() {
        {
            put("Afrikaans", "af");
            put("Arabic", "ar");
            put("Bangla", "bn");
            put("Bosnian (Latin)", "bs");
            put("Bulgarian", "bg");
            put("Cantonese (Traditional)", "yue");
            put("Catalan", "ca");
            put("Chinese Simplified", "zh-Hans");
            put("Chinese Traditional", "zh-Hant");
            put("Croatian", "hr");
            put("Czech", "cs");
            put("Danish", "da");
            put("Dutch", "nl");
            put("English", "en");
            put("Estonian", "et");
            put("Fijian", "fj");
            put("Filipino", "fil");
            put("Finnish", "fi");
            put("French", "fr");
            put("German", "de");
            put("Greek", "el");
            put("Haitian Creole", "ht");
            put("Hebrew", "he");
            put("Hindi", "hi");
            put("Hmong Daw", "mww");
            put("Hungarian", "hu");
            put("Indonesian", "id");
            put("Italian", "it");
            put("Japanese", "ja");
            put("Kiswahili", "sw");
            put("Klingon", "tlh");
            put("Klingon (plqaD)", "tlh-Qaak");
            put("Korean", "ko");
            put("Latvian", "lv");
            put("Lithuanian", "lt");
            put("Malagasy", "mg");
            put("Malay", "ms");
            put("Maltese", "mt");
            put("Norwegian", "nb");
            put("Persian", "fa");
            put("Polish", "pl");
            put("Portuguese", "pt");
            put("Queretaro Otomi", "otq");
            put("Romanian", "ro");
            put("Russian", "ru");
            put("Samoan", "sm");
            put("Serbian (Cyrillic)", "sr-Cyrl");
            put("Serbian (Latin)", "sr-Latn");
            put("Slovak", "sk");
            put("Slovenian", "sl");
            put("Spanish", "es");
            put("Swedish", "sv");
            put("Tahitian", "ty");
            put("Tamil", "ta");
            put("Thai", "th");
            put("Tongan", "to");
            put("Turkish", "tr");
            put("Ukrainian", "uk");
            put("Urdu", "ur");
            put("Vietnamese", "vi");
            put("Welsh", "cy");
            put("Yucatec Maya", "yua");
        }
    };

    public static String getCode(int recOrTran, String language) {
        switch (recOrTran) {
            case 0: {
                return mapRecolanguageCode.get(language);
            }
            case 1: {
                return mapTranlanguageCode.get(language);
            }
        }
        return null;
    }

}
