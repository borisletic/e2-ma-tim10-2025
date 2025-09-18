package com.example.ma2025.ui.missions.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.MissionProgress;
import com.example.ma2025.data.models.AllianceMember;
import java.util.List;
import java.util.Map;

public class MemberProgressAdapter extends RecyclerView.Adapter<MemberProgressAdapter.MemberProgressViewHolder> {

    private List<AllianceMember> members;
    private Map<String, MissionProgress> memberProgress;

    public MemberProgressAdapter(List<AllianceMember> members, Map<String, MissionProgress> memberProgress) {
        this.members = members;
        this.memberProgress = memberProgress;
    }

    @NonNull
    @Override
    public MemberProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_progress, parent, false);
        return new MemberProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberProgressViewHolder holder, int position) {
        AllianceMember member = members.get(position);
        MissionProgress progress = memberProgress.get(member.getUserId());

        if (progress == null) {
            progress = new MissionProgress(member.getUserId());
        }

        holder.bind(member, progress);
    }

    @Override
    public int getItemCount() {
        return members != null ? members.size() : 0;
    }

    public void updateData(List<AllianceMember> newMembers, Map<String, MissionProgress> newProgress) {
        this.members = newMembers;
        this.memberProgress = newProgress;
        notifyDataSetChanged();
    }

    static class MemberProgressViewHolder extends RecyclerView.ViewHolder {

        private TextView tvMemberName;
        private TextView tvMemberRole;
        private TextView tvTotalDamage;
        private ProgressBar progressBar;
        private ImageView ivNoFailedTasks;
        private TextView tvProgressDetails;

        public MemberProgressViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMemberName = itemView.findViewById(R.id.tv_member_name);
            tvMemberRole = itemView.findViewById(R.id.tv_member_role);
            tvTotalDamage = itemView.findViewById(R.id.tv_total_damage);
            progressBar = itemView.findViewById(R.id.progress_member);
            ivNoFailedTasks = itemView.findViewById(R.id.iv_no_failed_tasks);
            tvProgressDetails = itemView.findViewById(R.id.tv_progress_details);
        }

        public void bind(AllianceMember member, MissionProgress progress) {
            tvMemberName.setText(member.getUsername());
            tvMemberRole.setText(member.getRole().equals("leader") ? "Vođa" : "Član");

            int totalDamage = progress.getTotalDamageDealt();
            tvTotalDamage.setText(totalDamage + " HP");

            // Set progress bar (max damage someone can theoretically deal is around 100-120 HP)
            progressBar.setMax(120);
            progressBar.setProgress(Math.min(totalDamage, 120));

            // No failed tasks indicator
            ivNoFailedTasks.setImageResource(progress.isNoFailedTasks() ?
                    R.drawable.ic_check : R.drawable.ic_close);

            // Progress details
            String details = String.format("Prodavnica: %d | Napadi: %d | Laki: %d | Teški: %d | Poruke: %d",
                    progress.getStoreVisits(),
                    progress.getSuccessfulAttacks(),
                    progress.getEasyTasksCompleted(),
                    progress.getHardTasksCompleted(),
                    progress.getMessageDaysCount()
            );
            tvProgressDetails.setText(details);

            // Highlight current user or leader
            if (member.getRole().equals("leader")) {
                itemView.setBackgroundResource(R.drawable.bg_leader_highlight);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_member_normal);
            }
        }
    }
}