package com.recog.face2base;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(R.id.button == v.getId()){
            finish();
            startActivity(new Intent(MainActivity.this, CameraActivity.class));
            Toast.makeText(this, "Button Clicked", Toast.LENGTH_LONG).show();
        }
    }
}
