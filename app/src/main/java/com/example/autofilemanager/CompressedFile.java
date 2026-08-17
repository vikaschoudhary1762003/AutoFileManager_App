package com.example.autofilemanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CompressedFile extends AppCompatActivity {

    //VARIABLE
    TextView backbtn ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compressed_file);

        //CONENCT VARIABLE WITH ID
        backbtn = findViewById(R.id.BackButton);

        //ON CLICK BACK TO HOME DASHBOARD
        backbtn.setOnClickListener(view->{
            Intent intent = new Intent(CompressedFile.this , MainActivity.class);
            startActivity(intent);
        });
    }
}