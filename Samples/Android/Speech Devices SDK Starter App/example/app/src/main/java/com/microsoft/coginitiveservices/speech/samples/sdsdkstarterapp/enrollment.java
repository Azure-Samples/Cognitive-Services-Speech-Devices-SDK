package com.microsoft.coginitiveservices.speech.samples.sdsdkstarterapp;


import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// enroll user's voice signature
public class Enrollment {
    private static String audioFileName = new String();
    private static String userName = new String();
    private static String voiceSignature = new String();
    private static String region = "<Your Service Region>";//"centralus"
    private static final String speakerRecognitionKey = "<speaker Recognition Key>";

    public static void main(String args[]) throws Exception{
        Enrollment enroll = new Enrollment("Mike", "<audio file path>");
        enroll.enrollVoiceSignature();

        System.out.println("User's name: " + enroll.getUserName());
        System.out.println("User's voice signature:" + enroll.getVoiceSignature());

    }

    public Enrollment(String user, String file){
        userName = user;
        audioFileName = file;
    }
    public String getAudioFileName(){
        return audioFileName;
    }
    public String getVoiceSignature(){
        return voiceSignature;
    }

    public String getUserName(){

        return userName;
    }
    public void enrollVoiceSignature(){
        if(audioFileName.length() == 0){
            System.out.println("Please input valiable audio file path");
            return;
        }else{
            File wavFile = new File(audioFileName);
            if(wavFile.exists()) {
                Future<String> task = new enrollTask().signature(wavFile,speakerRecognitionKey,region);
                setOnTaskCompletedListener(task, result -> {
                    setVoiceSignature(result);
                    System.out.println("User's name: " +userName);
                    System.out.println("User's voice signature: " + result);
                });

            }else{
                System.out.println(audioFileName + " does not exist");
            }
        }
    }

    public void setVoiceSignature(String signature){
        voiceSignature = signature;
    }

    public class enrollTask {
        private ExecutorService executor = Executors.newSingleThreadExecutor();

        public Future<String> signature(File wavFile, String SRKey, String region) {
            return executor.submit(()->{
                String signatureStr = new String();
                try {

                    String lineEnd = "\r\n";
                    String twoHyphens = "--";
                    String boundary = new BigInteger(256, new Random()).toString();
                    int bytesRead, bytesAvailable, bufferSize;
                    byte[] buffer;
                    int maxBufferSize = 1024 * 1024;
                    URL url = new URL("https://signature." + region + ".cts.speech.microsoft.com/api/v1/Signature/GenerateVoiceSignatureFromFormData");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    // Set HTTP method to POST.
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("accept", "application/json");
                    connection.setRequestProperty("Ocp-Apim-Subscription-Key", SRKey);
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    FileInputStream fileInputStream;
                    DataOutputStream outputStream;
                    outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"File\";filename=\"" + wavFile.getName() + "\"" + lineEnd);
                    outputStream.writeBytes(lineEnd);
                    fileInputStream = new FileInputStream(wavFile);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    System.out.println("Buffer size: " + Integer.toString(bufferSize));
                    buffer = new byte[bufferSize];

                    // Read file
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    int count = 1;
                    while (bytesRead > 0) {
                        outputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    outputStream.writeBytes("\r\n--" + boundary + "--");
                    // Responses from the server (code and message)
                    int serverResponseCode = connection.getResponseCode();
                    System.out.println("serverResponseCode: " + Integer.toString(serverResponseCode));
                    String result = null;
                    if (serverResponseCode == 200) {
                        StringBuilder s_buffer = new StringBuilder();
                        InputStream is = new BufferedInputStream(connection.getInputStream());
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String inputLine;
                        while ((inputLine = br.readLine()) != null) {
                            s_buffer.append(inputLine);
                        }
                        result = s_buffer.toString();
                        System.out.println("Response result: " + result);
                        if (result != null) {
                            JSONObject obj = new JSONObject(result);
                            String status = obj.getString("Status");
                            if (status.equals("OK")) {
                                System.out.println("Enrollment: Get Signature ID is OK");
                                signatureStr = obj.getJSONObject("Signature").toString();
                            }
                        }
                    }
                    fileInputStream.close();
                    outputStream.flush();
                    outputStream.close();
                    System.out.println("Enrollment is finished ");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return signatureStr;
            });
        }
    }
    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }
    protected static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }
}
