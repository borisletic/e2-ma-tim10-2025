package com.example.ma2025.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import com.example.ma2025.R;

/**
 * Jednostavna Boss animacija koja koristi samo jedan sprite
 * i animacije transformacije (scale, translate, rotate, alpha)
 */
public class BossAnimationManager {

    private static final String TAG = "SimpleBossAnimation";

    private Context context;
    private ImageView bossImageView;
    private Handler animationHandler;

    // Animation states
    public enum BossState {
        IDLE,
        HIT,
        ATTACK,
        DEATH
    }

    private BossState currentState = BossState.IDLE;

    public BossAnimationManager(Context context, ImageView bossImageView) {
        this.context = context;
        this.bossImageView = bossImageView;
        this.animationHandler = new Handler(Looper.getMainLooper());

        // Set default boss sprite
        bossImageView.setImageResource(R.drawable.ic_boss_idle);

        // Start with idle animation
        playIdleAnimation();
    }

    /**
     * Play idle animation - gentle breathing effect
     */
    public void playIdleAnimation() {
        if (currentState == BossState.IDLE) return;

        currentState = BossState.IDLE;

        // Reset any transformations
        bossImageView.clearAnimation();
        bossImageView.setScaleX(1.0f);
        bossImageView.setScaleY(1.0f);
        bossImageView.setAlpha(1.0f);
        bossImageView.setRotation(0);
        bossImageView.setTranslationX(0);
        bossImageView.setTranslationY(0);

        // Breathing animation - subtle scale in/out
        ScaleAnimation breathe = new ScaleAnimation(
                1.0f, 1.05f, // X scale from 100% to 105%
                1.0f, 1.05f, // Y scale from 100% to 105%
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
                Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
        );
        breathe.setDuration(2000); // 2 seconds
        breathe.setRepeatCount(Animation.INFINITE);
        breathe.setRepeatMode(Animation.REVERSE);

        bossImageView.startAnimation(breathe);
    }

    /**
     * Play hit animation - shake and flash red
     */
    public void playHitAnimation() {
        currentState = BossState.HIT;
        bossImageView.clearAnimation();

        // Shake animation
        TranslateAnimation shake = new TranslateAnimation(
                0, 20, 0, 0 // Move 20px right
        );
        shake.setDuration(100);
        shake.setRepeatCount(3);
        shake.setRepeatMode(Animation.REVERSE);

        bossImageView.startAnimation(shake);

        // Flash red effect using color filter
        bossImageView.setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.MULTIPLY);

        // Remove red tint after animation
        animationHandler.postDelayed(() -> {
            bossImageView.clearColorFilter();
            if (currentState == BossState.HIT) {
                playIdleAnimation();
            }
        }, 400);
    }

    /**
     * Play attack animation - lean forward and scale up
     */
    public void playAttackAnimation() {
        currentState = BossState.ATTACK;
        bossImageView.clearAnimation();

        // Attack sequence: scale up -> lean forward -> back to normal
        ScaleAnimation scaleUp = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleUp.setDuration(200);
        scaleUp.setFillAfter(true);

        bossImageView.startAnimation(scaleUp);

        // Lean forward after scale up
        animationHandler.postDelayed(() -> {
            TranslateAnimation leanForward = new TranslateAnimation(
                    0, 30, // Move 30px right (forward)
                    0, 0
            );
            leanForward.setDuration(300);
            leanForward.setFillAfter(true);

            bossImageView.startAnimation(leanForward);

            // Return to normal after attack
            animationHandler.postDelayed(() -> {
                if (currentState == BossState.ATTACK) {
                    playIdleAnimation();
                }
            }, 300);

        }, 200);
    }

    /**
     * Play death animation - fade out and fall
     */
    public void playDeathAnimation() {
        currentState = BossState.DEATH;
        bossImageView.clearAnimation();

        // Death sequence: shake -> fade -> fall

        // 1. Violent shake
        TranslateAnimation deathShake = new TranslateAnimation(
                -30, 30, 0, 0
        );
        deathShake.setDuration(150);
        deathShake.setRepeatCount(4);
        deathShake.setRepeatMode(Animation.REVERSE);

        bossImageView.startAnimation(deathShake);

        // 2. Fade out and fall after shake
        animationHandler.postDelayed(() -> {
            // Fade and fall animation
            bossImageView.animate()
                    .alpha(0.0f)        // Fade to transparent
                    .scaleX(0.5f)       // Shrink horizontally
                    .scaleY(0.5f)       // Shrink vertically
                    .translationY(100)  // Fall down
                    .rotation(90)       // Rotate as falling
                    .setDuration(1000)
                    .start();
        }, 600);
    }

    /**
     * Play shake effect for strong hits
     */
    public void playShakeEffect() {
        // Quick intense shake without changing state
        TranslateAnimation shake = new TranslateAnimation(
                -15, 15, -10, 10
        );
        shake.setDuration(80);
        shake.setRepeatCount(5);
        shake.setRepeatMode(Animation.REVERSE);

        bossImageView.startAnimation(shake);
    }

    /**
     * Play flash effect for damage indication
     */
    public void playFlashEffect() {
        // White flash effect
        bossImageView.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.ADD);

        animationHandler.postDelayed(() -> {
            bossImageView.clearColorFilter();
        }, 150);
    }

    /**
     * Play glow effect (for special attacks or power-ups)
     */
    public void playGlowEffect() {
        // Pulsing glow effect
        bossImageView.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction(() ->
                        bossImageView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                );

        // Add glow color
        bossImageView.setColorFilter(0x44FF0000, android.graphics.PorterDuff.Mode.ADD);

        animationHandler.postDelayed(() -> {
            bossImageView.clearColorFilter();
        }, 600);
    }

    /**
     * Reset boss to normal state
     */
    public void resetToIdle() {
        bossImageView.clearAnimation();
        bossImageView.clearColorFilter();
        bossImageView.setScaleX(1.0f);
        bossImageView.setScaleY(1.0f);
        bossImageView.setAlpha(1.0f);
        bossImageView.setRotation(0);
        bossImageView.setTranslationX(0);
        bossImageView.setTranslationY(0);

        playIdleAnimation();
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        animationHandler.removeCallbacksAndMessages(null);
        bossImageView.clearAnimation();
        bossImageView.clearColorFilter();
    }

    /**
     * Get current animation state
     */
    public BossState getCurrentState() {
        return currentState;
    }

    /**
     * Check if boss is currently animating (not just idle)
     */
    public boolean isAnimating() {
        return currentState != BossState.IDLE;
    }
}