package com.example.autofilemanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DuplicateFiles extends AppCompatActivity {


    //TAKE VARIABLES
    TextView backbtn ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_duplicate_files);

        //CONNECT VARIABLE TO ID USING FINDVIEW BY ID
        backbtn=findViewById(R.id.BackButton);

        //ONCLICK SEND BACK TO HOME DASHOBARD
        backbtn.setOnClickListener(view->{
            Intent intent = new Intent(DuplicateFiles.this , MainActivity.class);
            startActivity(intent);
        });

    }
}