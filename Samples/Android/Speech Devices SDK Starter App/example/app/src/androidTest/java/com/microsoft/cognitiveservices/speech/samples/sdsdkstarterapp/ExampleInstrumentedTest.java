package com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.microsoft.cognitiveservices.speech.samples.sdsdkstarterapp", appContext.getPackageName());
    }

    @Test
    public void runSpeechSDKtests() {
        loadTestProperties("/data/local/tmp/tests/test-java-unittests.properties");
        tests.runner.Runner.mainRunner("tests.unit.AllUnitTests");
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private static void loadTestProperties(String filename) {
        File f = new File(filename);
        if(!f.exists() || !f.isFile()) {
            try {
                FileOutputStream fo = new FileOutputStream(f);
                System.getProperties().save(fo, "my comments");
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Properties p = new Properties();
        try {
            FileInputStream fi = new FileInputStream(f);
            p.load(fi);
            fi.close();

            for(Object name: p.keySet()) {
                String key = name.toString();
                System.setProperty(key, p.getProperty(key));
            }

            if (System.getProperty("TestOutputFilename", null) == null) {
                System.setProperty("TestOutputFilename", "/data/local/tmp/tests/test-java-unittests.xml");
            }

            FileOutputStream fo = new FileOutputStream(f);
            System.getProperties().save(fo, "Effective properties for Testrun.");
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
