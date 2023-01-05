package com.example.superfuniotproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {

    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private TextView textView;
    private ImageView micButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        Button goToInstructionsButton = findViewById(R.id.button_go_to_instructions);
        textView = findViewById(R.id.text_speech);
        micButton = findViewById(R.id.button_mic);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a position, then 'color', and then a color. Or say 'all', then 'color', and then a color. Or say 'draw' and a keyword");
        speechRecognizer.setRecognitionListener(recognitionListener);

        goToInstructionsButton.setOnClickListener((view) -> goToInstructionsLayout());

        micButton.setOnClickListener((view) -> {
            micButton.setImageResource(R.drawable.icons8_microphone_red_60);
            speechRecognizer.startListening(speechRecognizerIntent);
        });
    }

    /** Instruction button -----------------------------------------------------------------------*/
    // Navigate to the second layout file
    private void goToInstructionsLayout() {
        // Create an intent to start the second activity
        Intent intent = new Intent(this, InstructionsActivity.class);
        // Start the activity
        startActivity(intent);
    }

    /** Speech Recognizer ------------------------------------------------------------------------*/

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {}

        @Override
        public void onBeginningOfSpeech() {
            textView.setText("");
            textView.setHint("Listening...");
        }

        @Override
        public void onRmsChanged(float v) {}

        @Override
        public void onBufferReceived(byte[] bytes) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int i) {
            micButton.setImageResource(R.drawable.icons8_microphone_60);
            textView.setHint("Tap to Speak");
        }

        @Override
        public void onResults(Bundle bundle) {
            micButton.setImageResource(R.drawable.icons8_microphone_60);
            ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (data != null && !data.isEmpty()) {

                // Get the first recognized word
                String input = data.get(0);
                textView.setText(data.get(0));

                if (input.startsWith("draw")) {
                    String keyword = extractKeyword(input);
                    DrawAsyncTaskRunner runner2 = new DrawAsyncTaskRunner(keyword);
                    runner2.execute();
                } else {
                    String[] words = input.split("\\s+");
                    if (words.length >= 3) {
                        // Get the position, the color keyword, and the color
                        String position = words[0];
                        String colorWord = words[1];
                        String color = words[2];

                        // Check if the color keyword is "color" or "colour"
                        if (!"color".equalsIgnoreCase(colorWord) && !"colour".equalsIgnoreCase(colorWord)) {
                            return;
                        }
                        // Combine words like "light blue" into "lightblue"
                        for (int i = 3; i < words.length; i++) {
                            color += words[i];
                        }

                        color = color.toLowerCase();
                        ColorAsyncTaskRunner runner = new ColorAsyncTaskRunner(position, color);
                        runner.execute();
                    }
                }
            }
        }

        @Override
        public void onPartialResults(Bundle bundle) {}

        @Override
        public void onEvent(int i, Bundle bundle) {}
    };

    private String extractKeyword(String input) {
        String keyword = input.substring(5).trim();
        keyword = keyword.replaceAll("^(a |an )", "");
        keyword = keyword.replaceAll("\\s", "");
        keyword = keyword.replaceAll("-", "");
        return keyword;
    }

    /** Python Run -------------------------------------------------------------------------------*/

    public void run (String command) {
        String hostname = "130.237.177.208";
        String username = "pi";
        String password = "IoT@2021";
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            Connection conn = new Connection(hostname); //init connection
            conn.connect(); //start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username,
                    password);
            if (!isAuthenticated)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            //reads text
            while (true) {
                String line = br.readLine(); // read line
                if (line == null)
                    break;
                System.out.println(line);
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    /** Async Runner -----------------------------------------------------------------------------*/

    @SuppressLint("StaticFieldLeak")
    private class ColorAsyncTaskRunner extends AsyncTask<String, String, String> {

        protected String position;
        protected String color;

        protected ColorAsyncTaskRunner(String pPosition, String pColor) {
            super();
            position = pPosition;
            color = pColor;
        }

        @Override
        protected String doInBackground(String... params) {
            String status = "";
            try {
                run("sudo python3 IoTProject/ColorChessboard.py " + position + " " + color);
                status = "success";
            } catch (Exception e) {
                e.printStackTrace();
                status = "error";
            }
            return status;
        }

        @Override
        protected void onPostExecute(String result) {}
    }

    @SuppressLint("StaticFieldLeak")
    private class DrawAsyncTaskRunner extends AsyncTask<String, String, String> {

        protected String keyword;

        protected DrawAsyncTaskRunner(String pKeyword) {
            super();
            keyword = pKeyword;
        }

        @Override
        protected String doInBackground(String... params) {
            String status = "";
            try {
                System.out.println("sudo python3 IoTProject/ImageChessboard.py " + keyword);
                run("sudo python3 IoTProject/ImageChessboard.py " + keyword);
                status = "success";
            } catch (Exception e) {
                e.printStackTrace();
                status = "error";
            }
            return status;
        }

        @Override
        protected void onPostExecute(String result) {}
    }

    /** Check Permission -------------------------------------------------------------------------*/

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

    /** On Destroy -------------------------------------------------------------------------------*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

}
