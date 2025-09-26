package com.kodelink.glide;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        View backToLogin = findViewById(R.id.btnBackToLogin);
        if (backToLogin != null) {
            backToLogin.setOnClickListener(v -> finish());
        }
    }
}


