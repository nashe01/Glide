package com.kodelink.glide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity implements View.OnClickListener {

    private int currentStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding1);
        bindButtonsForStep(R.id.btnSkip1, R.id.btnNext1, 0);
    }

    private void bindButtonsForStep(@IdRes int skipId, @IdRes int nextOrGetStartedId, int getStartedFlag) {
        View skip = findViewById(skipId);
        View next = findViewById(nextOrGetStartedId);
        if (skip != null) skip.setOnClickListener(this);
        if (next != null) next.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSkip1 || id == R.id.btnSkip2 || id == R.id.btnSkip3) {
            goToLogin();
            return;
        }
        if (id == R.id.btnNext1) {
            currentStep = 2;
            setContentView(R.layout.activity_onboarding2);
            bindButtonsForStep(R.id.btnSkip2, R.id.btnNext2, 0);
            return;
        }
        if (id == R.id.btnNext2) {
            currentStep = 3;
            setContentView(R.layout.activity_onboarding3);
            bindButtonsForStep(R.id.btnSkip3, R.id.btnGetStarted, 1);
            return;
        }
        if (id == R.id.btnGetStarted) {
            goToLogin();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}


