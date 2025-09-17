package com.example.ma2025.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import com.example.ma2025.R;

public class TreasureChestAnimator {

    private static final String TAG = "TreasureChestAnimator";
    private Context context;
    private ImageView chestImageView;
    private Handler animationHandler;

    public interface OnChestOpenedListener {
        void onChestOpened();
    }

    public TreasureChestAnimator(Context context, ImageView chestImageView) {
        this.context = context;
        this.chestImageView = chestImageView;
        this.animationHandler = new Handler(Looper.getMainLooper());

        // Set closed chest image
        chestImageView.setImageResource(R.drawable.treasure_chest_closed);
    }

    /**
     * Plays shake animation on the chest
     */
    public void playShakeAnimation() {
        TranslateAnimation shake = new TranslateAnimation(
                -15, 15, // X movement
                -10, 10  // Y movement
        );
        shake.setDuration(100);
        shake.setRepeatCount(5);
        shake.setRepeatMode(Animation.REVERSE);

        chestImageView.startAnimation(shake);

        // Add scale effect during shake
        animationHandler.postDelayed(() -> {
            ScaleAnimation pulse = new ScaleAnimation(
                    1.0f, 1.1f,
                    1.0f, 1.1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulse.setDuration(200);
            pulse.setRepeatCount(1);
            pulse.setRepeatMode(Animation.REVERSE);

            chestImageView.startAnimation(pulse);
        }, 150);
    }

    /**
     * Opens the chest with animation and calls listener when done
     */
    public void openChest(OnChestOpenedListener listener) {
        // 1. Final shake before opening
        TranslateAnimation finalShake = new TranslateAnimation(-20, 20, -15, 15);
        finalShake.setDuration(150);
        finalShake.setRepeatCount(3);
        finalShake.setRepeatMode(Animation.REVERSE);

        chestImageView.startAnimation(finalShake);

        // 2. Change to open chest image and scale up
        animationHandler.postDelayed(() -> {
            chestImageView.setImageResource(R.drawable.treasure_chest_open);

            ScaleAnimation openAnimation = new ScaleAnimation(
                    1.0f, 1.3f,
                    1.0f, 1.3f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            openAnimation.setDuration(400);
            openAnimation.setInterpolator(new BounceInterpolator());
            openAnimation.setFillAfter(true);

            chestImageView.startAnimation(openAnimation);

            // 3. Call listener after opening animation
            if (listener != null) {
                animationHandler.postDelayed(listener::onChestOpened, 450);
            }

        }, 500);
    }

    /**
     * Shows bouncing animation to indicate chest is ready to be opened
     */
    public void showReadyToOpenAnimation() {
        AnimationSet bounceSet = new AnimationSet(false);

        // Bounce up and down
        TranslateAnimation bounce = new TranslateAnimation(
                0, 0,
                0, -30
        );
        bounce.setDuration(600);
        bounce.setRepeatCount(Animation.INFINITE);
        bounce.setRepeatMode(Animation.REVERSE);
        bounce.setInterpolator(new BounceInterpolator());

        // Gentle scale pulsing
        ScaleAnimation pulse = new ScaleAnimation(
                1.0f, 1.05f,
                1.0f, 1.05f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(800);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);

        bounceSet.addAnimation(bounce);
        bounceSet.addAnimation(pulse);

        chestImageView.startAnimation(bounceSet);
    }

    /**
     * Resets chest to closed state
     */
    public void resetChest() {
        chestImageView.clearAnimation();
        chestImageView.setImageResource(R.drawable.treasure_chest_closed);
        chestImageView.setScaleX(1.0f);
        chestImageView.setScaleY(1.0f);
        chestImageView.setTranslationX(0);
        chestImageView.setTranslationY(0);
    }

    /**
     * Cleanup animations
     */
    public void cleanup() {
        animationHandler.removeCallbacksAndMessages(null);
        chestImageView.clearAnimation();
    }
}