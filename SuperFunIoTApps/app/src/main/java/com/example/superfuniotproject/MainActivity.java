package com.example.superfuniotproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {

    TextView txv_temp_indoor = null;
    TextView txv_outdoor_light_show = null;
    Switch lightToggle = null;
    Button btnUpdateTemp = null;
    String temp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txv_temp_indoor = (TextView) findViewById(R.id.indoorTempShow);
        txv_temp_indoor.setText("the fetched indoor temp value");

        txv_outdoor_light_show = (TextView)  findViewById(R.id.outdoorLightShow);
        txv_outdoor_light_show.setText("Off");

        lightToggle = (Switch) findViewById(R.id.btnToggle);
        lightToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // below you write code to change switch status and action to take
                if (isChecked) {
                    //do something if checked
                    new AsyncTask<Integer, Void, Void>(){
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("python group4/LightOn.py");
                            return null;
                        }
                    }.execute(1);
                    txv_outdoor_light_show.setText("On");
                } else {
                    new AsyncTask<Integer, Void, Void>(){
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("python group4/LightOff.py");
                            return null;
                        }
                    }.execute(1);
                    txv_outdoor_light_show.setText("Off");
                }
            }
        });

        btnUpdateTemp = (Button) findViewById(R.id.btnUpdateTemp);
        btnUpdateTemp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AsyncTask<Integer, Void, Void>(){
                    @Override
                    protected Void doInBackground(Integer... params) {
                        run("python group4/GetTemperature.py");
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        txv_temp_indoor.setText(temp);
                    }
                }.execute(1);
            }
        });
    }

    public void run (String command) {
        String hostname = "130.237.177.205";
        String username = "pi";
        String password = "IoT@2021";
        try
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            Connection conn = new Connection(hostname); //init connection
            conn.connect(); //start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username,
                    password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            //reads text
            while (true){
                String line = br.readLine(); // read line
                if (line == null)
                    break;
                System.out.println(line);
                if(command == "python group4/GetTemperature.py") {
                    temp = line;
                }
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        }
        catch (IOException e)
        { e.printStackTrace(System.err);
            System.exit(2); }
    }
}