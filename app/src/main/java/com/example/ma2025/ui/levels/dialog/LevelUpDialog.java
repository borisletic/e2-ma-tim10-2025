package com.example.ma2025.ui.levels.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.ma2025.R;
import com.example.ma2025.databinding.DialogLevelUpBinding;
import com.example.ma2025.utils.Constants;

public class LevelUpDialog extends DialogFragment {

    private static final String ARG_OLD_LEVEL = "old_level";
    private static final String ARG_NEW_LEVEL = "new_level";
    private static final String ARG_NEW_TITLE = "new_title";
    private static final String ARG_PP_GAINED = "pp_gained";
    private static final String ARG_TOTAL_PP = "total_pp";

    private DialogLevelUpBinding binding;
    private int oldLevel;
    private int newLevel;
    private String newTitle;
    private int ppGained;
    private int totalPp;

    public static LevelUpDialog newInstance(int oldLevel, int newLevel, String newTitle, int ppGained, int totalPp) {
        LevelUpDialog dialog = new LevelUpDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_OLD_LEVEL, oldLevel);
        args.putInt(ARG_NEW_LEVEL, newLevel);
        args.putString(ARG_NEW_TITLE, newTitle);
        args.putInt(ARG_PP_GAINED, ppGained);
        args.putInt(ARG_TOTAL_PP, totalPp);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);

        if (getArguments() != null) {
            oldLevel = getArguments().getInt(ARG_OLD_LEVEL);
            newLevel = getArguments().getInt(ARG_NEW_LEVEL);
            newTitle = getArguments().getString(ARG_NEW_TITLE, "");
            ppGained = getArguments().getInt(ARG_PP_GAINED);
            totalPp = getArguments().getInt(ARG_TOTAL_PP);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogLevelUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDialog();
        populateData();
        startAnimations();
        setupClickListeners();
    }

    private void setupDialog() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }
    }

    private void populateData() {
        binding.tvOldLevel.setText("Nivo " + oldLevel);
        binding.tvNewLevel.setText("Nivo " + newLevel);
        binding.tvNewTitle.setText(newTitle);

        if (ppGained > 0) {
            binding.tvPpGained.setText("+" + ppGained + " PP");
            binding.tvTotalPp.setText("Ukupno: " + totalPp + " PP");
            binding.layoutPpRewards.setVisibility(View.VISIBLE);
        } else {
            binding.layoutPpRewards.setVisibility(View.GONE);
        }

        // Initially hide elements for animation
        binding.layoutContent.setAlpha(0f);
        binding.ivLevelUpIcon.setScaleX(0f);
        binding.ivLevelUpIcon.setScaleY(0f);
    }

    private void startAnimations() {
        // Icon scale animation
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(binding.ivLevelUpIcon, "scaleX", 0f, 1.2f, 1f);
        iconScaleX.setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        iconScaleX.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(binding.ivLevelUpIcon, "scaleY", 0f, 1.2f, 1f);
        iconScaleY.setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        iconScaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Content fade in
        ObjectAnimator contentFade = ObjectAnimator.ofFloat(binding.layoutContent, "alpha", 0f, 1f);
        contentFade.setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        contentFade.setStartDelay(300);

        // Start animations
        iconScaleX.start();
        iconScaleY.start();
        contentFade.start();

        // Number count animation for PP using ValueAnimator
        if (ppGained > 0) {
            animatePpCounter();
        }
    }

    private void animatePpCounter() {
        // FIXED: Using ValueAnimator instead of ObjectAnimator
        ValueAnimator ppAnimator = ValueAnimator.ofInt(0, ppGained);
        ppAnimator.setDuration(Constants.ANIMATION_DURATION_LONG);
        ppAnimator.setStartDelay(600);
        ppAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            binding.tvPpGained.setText("+" + value + " PP");
        });
        ppAnimator.start();
    }

    private void setupClickListeners() {
        binding.btnContinue.setOnClickListener(v -> {
            startExitAnimation();
        });

        binding.btnShare.setOnClickListener(v -> {
            shareAchievement();
        });
    }

    private void shareAchievement() {
        // TODO: Implement sharing functionality
        // For now, just close the dialog
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(),
                    "Sharing funkcionalnost će biti dodana uskoro!",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void startExitAnimation() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(binding.getRoot(), "alpha", 1f, 0f);
        fadeOut.setDuration(Constants.ANIMATION_DURATION_SHORT);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss();
            }
        });
        fadeOut.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}