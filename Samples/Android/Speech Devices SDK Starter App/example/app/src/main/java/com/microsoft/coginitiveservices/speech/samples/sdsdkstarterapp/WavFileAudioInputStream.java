package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;

public class WavFileAudioInputStream extends PullAudioInputStreamCallback {
    private FileInputStream reader;
    private int audioInputStreamAvailableLength;
    private AudioStreamFormat wavFormat;

    public WavFileAudioInputStream(String filename) {
        try {

            this.reader = new FileInputStream(filename);

            // Note: assumption about order of chunks
            // Tag "RIFF"
            byte data[] = new byte[4];
            int numRead = reader.read(data, 0, 4);
            ThrowIfFalse((numRead == 4) && (data[0] == 'R') && (data[1] == 'I') && (data[2] == 'F') && (data[3] == 'F'), "RIFF");

            // Chunk size
            /* int fileLength = */
            ReadInt32(reader);

            // Subchunk, Wave Header
            // Subchunk, Format
            // Tag: "WAVE"
            numRead = reader.read(data, 0, 4);
            ThrowIfFalse((numRead == 4) && (data[0] == 'W') && (data[1] == 'A') && (data[2] == 'V') && (data[3] == 'E'), "WAVE");

            // Tag: "fmt"
            numRead = reader.read(data, 0, 4);
            ThrowIfFalse((numRead == 4) && (data[0] == 'f') && (data[1] == 'm') && (data[2] == 't') && (data[3] == ' '), "fmt ");

            // chunk format size
            long formatSize = ReadInt32(reader);
            ThrowIfFalse(formatSize >= 16, "formatSize");

            int formatTag = ReadUInt16(reader);
            int channels = ReadUInt16(reader);
            int samplesPerSec = (int) ReadUInt32(reader);
            int avgBytesPerSec = (int) ReadUInt32(reader);
            int blockAlign = ReadUInt16(reader);
            int bitsPerSample = ReadUInt16(reader);

            this.wavFormat = AudioStreamFormat.getWaveFormatPCM(samplesPerSec, (byte)bitsPerSample, (byte)channels);

            // Until now we have read 16 bytes in format, the rest is cbSize and is ignored
            // for now.
            if (formatSize > 16) {
                numRead = reader.read(new byte[(int) (formatSize - 16)]);
                ThrowIfFalse(numRead == (int)(formatSize - 16), "could not skip extended format");
            }

            boolean foundDataChunk = false;
            while (!foundDataChunk)
            {
                reader.read(data, 0, 4);
                int size = ReadInt32(reader);
                if (data[0] == 'd' && data[1] == 'a' && data[2] == 't' && data[3] == 'a')
                {
                    this.audioInputStreamAvailableLength = size;
                    foundDataChunk = true;
                    break;
                }
                reader.read(new byte[(int) (size)]);
            }
            ThrowIfFalse(foundDataChunk == true, "Doesn't contain a data chunk!");

        } catch (IOException ex) {
            if (this.reader != null) {
                try {
                    this.reader.close();
                } catch (IOException e) {
                    Log.i("Exception", e.toString());
                }
                this.reader = null;
            }

            // Handle the error ...
            throw new IllegalArgumentException(ex);
        }
    }

    public AudioStreamFormat getWavFormat()
    {
        return this.wavFormat;
    }

    private int ReadInt32(FileInputStream inputStream) throws IOException {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            n |= inputStream.read() << (i * 8);
        }
        return n;
    }

    private long ReadUInt32(FileInputStream inputStream) throws IOException {
        long n = 0;
        for (int i = 0; i < 4; i++) {
            n |= inputStream.read() << (i * 8);
        }
        return n;
    }

    private int ReadUInt16(FileInputStream inputStream) throws IOException {
        int n = 0;
        for (int i = 0; i < 2; i++) {
            n |= inputStream.read() << (i * 8);
        }
        return n;
    }

    private static void ThrowIfFalse(Boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Reads data from audio input stream into the data buffer. The maximal number
     * of bytes to be read is determined by the size of dataBuffer.
     *
     * @param dataBuffer
     *            The byte array to store the read data.
     * @return the number of bytes have been read.
     */
    @Override
    public int read(byte[] dataBuffer) {
        if (dataBuffer == null) throw new NullPointerException("dataBuffer");

        try {
            // only read the data chunk, ignore any
            // trailing data
            int wantRead = Math.min(this.audioInputStreamAvailableLength, dataBuffer.length);
            //Log.i("length-start", "start");
            int numRead = reader.read(dataBuffer, 0, wantRead);
            int retLength = (numRead > 0) ? numRead : 0;

            this.audioInputStreamAvailableLength -= retLength;
            //Log.i("length-end", String.valueOf(retLength));
            return retLength;
        } catch (Exception e) {
            throw new IllegalAccessError(e.toString());
        }
    }

    @Override
    public String getProperty(PropertyId id) {
        String propertyIdStr = "";
        if (PropertyId.DataBuffer_UserId == id) {
            propertyIdStr = "speaker123";
        }
        else if(PropertyId.DataBuffer_TimeStamp == id) {
            propertyIdStr = "somefaketimestamp";
        }
        return propertyIdStr;
    }

    /**
     * Closes the audio input stream.
     */
    @Override
    public void close() {
        try {
            this.audioInputStreamAvailableLength = 0;
            this.reader.close();
            this.reader = null;
        } catch (IOException | NullPointerException e) {
            throw new IllegalAccessError(e.toString());
        }
    }
}
