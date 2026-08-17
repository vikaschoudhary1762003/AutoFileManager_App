package com.example.autofilemanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScanStorage extends AppCompatActivity {


    //VARIABLE FOR BACK BTN ON TOP BAR
    TextView backButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_storage);

       //CONNECT VARIABLE WITH ID
        backButton = findViewById(R.id.BackButton);

        //ONCLICK GO BACK TO THE HOME PAGE
        backButton.setOnClickListener(v -> {

            Intent intent = new  Intent(ScanStorage.this ,MainActivity.class);
            startActivity(intent);

        });

    }
}