package com.kodelink.glide;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        View submit = findViewById(R.id.btnSubmitReset);
        if (submit != null) submit.setOnClickListener(v -> {
            EditText input = findViewById(R.id.etEmailOrPhone);
            String value = input != null ? input.getText().toString().trim() : "";
            if (value.isEmpty()) {
                Toast.makeText(this, "Please enter email or phone", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Reset link/code sent (mock)", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}


