package com.example.ma2025.ui.levels.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.ui.levels.model.LevelInfo;
import java.util.ArrayList;
import java.util.List;

public class LevelProgressAdapter extends RecyclerView.Adapter<LevelProgressAdapter.LevelViewHolder> {

    private List<LevelInfo> levels = new ArrayList<>();

    public void updateLevels(List<LevelInfo> newLevels) {
        this.levels.clear();
        this.levels.addAll(newLevels);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level_progress, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        LevelInfo levelInfo = levels.get(position);
        holder.bind(levelInfo);
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLevel;
        private TextView tvTitle;
        private TextView tvXpRequired;
        private TextView tvPpReward;
        private ImageView ivLevelIcon;
        private View levelIndicator;

        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvXpRequired = itemView.findViewById(R.id.tv_xp_required);
            tvPpReward = itemView.findViewById(R.id.tv_pp_reward);
            ivLevelIcon = itemView.findViewById(R.id.iv_level_icon);
            levelIndicator = itemView.findViewById(R.id.level_indicator);
        }

        public void bind(LevelInfo levelInfo) {
            tvLevel.setText("Nivo " + levelInfo.getLevel());
            tvTitle.setText(levelInfo.getTitle());

            if (levelInfo.getLevel() == 0) {
                tvXpRequired.setText("Početni nivo");
            } else {
                tvXpRequired.setText(levelInfo.getXpRequired() + " XP potrebno");
            }

            if (levelInfo.getPpReward() > 0) {
                tvPpReward.setText("+" + levelInfo.getPpReward() + " PP");
                tvPpReward.setVisibility(View.VISIBLE);
            } else {
                tvPpReward.setVisibility(View.GONE);
            }

            // Visual state based on level status
            if (levelInfo.isCurrent()) {
                // Current level - highlighted
                levelIndicator.setBackgroundColor(itemView.getContext()
                        .getResources().getColor(R.color.primary_color, null));
                ivLevelIcon.setImageResource(R.drawable.ic_level_current);
                tvLevel.setTextColor(itemView.getContext()
                        .getResources().getColor(R.color.primary_color, null));
            } else if (levelInfo.isUnlocked()) {
                // Unlocked level - completed
                levelIndicator.setBackgroundColor(itemView.getContext()
                        .getResources().getColor(R.color.success_color, null));
                ivLevelIcon.setImageResource(R.drawable.ic_level_completed);
                tvLevel.setTextColor(itemView.getContext()
                        .getResources().getColor(R.color.success_color, null));
            } else {
                // Locked level - not reached yet
                levelIndicator.setBackgroundColor(itemView.getContext()
                        .getResources().getColor(R.color.divider_color, null));
                ivLevelIcon.setImageResource(R.drawable.ic_level_locked);
                tvLevel.setTextColor(itemView.getContext()
                        .getResources().getColor(R.color.text_secondary, null));
            }
        }
    }
}