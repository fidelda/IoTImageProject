package com.example.superfuniotproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private RelativeLayout background;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        textView = findViewById(R.id.text);
        micButton = findViewById(R.id.button);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        background = findViewById(R.id.background);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(recognitionListener);

        micButton.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                speechRecognizer.stopListening();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                micButton.setImageResource(R.drawable.icons8_microphone_blue_60);
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });
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
            String[] words = data.get(0).split("\\W+");
            textView.setText(data.get(0));
            if(!words[0].equalsIgnoreCase("color") || words.length < 2) {
                return;
            }
            String curColor = words[1].toLowerCase();
            if(words.length > 2 && (curColor.equalsIgnoreCase("light") || curColor.equalsIgnoreCase("dark")))
            {
                curColor += words[2].toLowerCase();
            }
            try {
                background.setBackgroundColor(Color.parseColor(curColor));
            } catch (Exception e) {
                System.out.println(curColor + " is not a color");
                return;
            }
            ColorAsyncTaskRunner runner = new ColorAsyncTaskRunner(curColor);
            runner.execute();
        }

        @Override
        public void onPartialResults(Bundle bundle) {}

        @Override
        public void onEvent(int i, Bundle bundle) {}
    };

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

        protected String color;

        protected ColorAsyncTaskRunner(String pColor) {
            super();
            color = pColor;
        }

        @Override
        protected String doInBackground(String... params) {
            String status = "";
            try {
                run("python IoTProject/LED.py " + color);
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