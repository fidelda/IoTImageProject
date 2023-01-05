package com.example.superfuniotproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class InstructionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        // Find the button view and set an OnClickListener
        Button goBackButton = findViewById(R.id.button_go_back);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go back to the main layout
                goBackToMainLayout();
            }
        });
    }

    // Navigate back to the main layout file
    private void goBackToMainLayout() {
        // Finish the current activity
        finish();
    }
}