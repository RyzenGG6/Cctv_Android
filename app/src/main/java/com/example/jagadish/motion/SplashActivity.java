package com.example.jagadish.motion;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private ImageView logoImageView;
    private TextView appNameTextView;
    private View circularReveal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        circularReveal = findViewById(R.id.circular_reveal);

        // Initially hide the views
        logoImageView.setAlpha(0f);
        appNameTextView.setAlpha(0f);

        // Start animations after a short delay
        logoImageView.postDelayed(this::startAnimations, 300);
    }

    private void startAnimations() {
        // Logo animations
        ObjectAnimator logoScale = ObjectAnimator.ofFloat(logoImageView, View.SCALE_X, 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImageView, View.SCALE_Y, 0.3f, 1f);
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(logoImageView, View.ALPHA, 0f, 1f);

        // Text animations
        ObjectAnimator textFade = ObjectAnimator.ofFloat(appNameTextView, View.ALPHA, 0f, 1f);
        ObjectAnimator textSlide = ObjectAnimator.ofFloat(appNameTextView, View.TRANSLATION_Y, 50f, 0f);

        // Combine logo animations
        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoScale, logoScaleY, logoFade);
        logoAnimSet.setDuration(1000);
        logoAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Combine text animations
        AnimatorSet textAnimSet = new AnimatorSet();
        textAnimSet.playTogether(textFade, textSlide);
        textAnimSet.setDuration(800);
        textAnimSet.setStartDelay(500);
        textAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Play all animations together
        AnimatorSet allAnimSet = new AnimatorSet();
        allAnimSet.playTogether(logoAnimSet, textAnimSet);

        allAnimSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                // Start MainActivity after animations complete
                logoImageView.postDelayed(() -> {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 500);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        allAnimSet.start();
    }
}
