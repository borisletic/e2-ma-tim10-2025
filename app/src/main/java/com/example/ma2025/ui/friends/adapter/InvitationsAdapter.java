package com.example.ma2025.ui.friends.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.AllianceInvitation;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.InvitationViewHolder> {

    private List<AllianceInvitation> invitations;
    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAcceptInvitation(AllianceInvitation invitation);
        void onDeclineInvitation(AllianceInvitation invitation);
    }

    public InvitationsAdapter(List<AllianceInvitation> invitations, OnInvitationActionListener listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        AllianceInvitation invitation = invitations.get(position);
        holder.bind(invitation);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAllianceName;
        private TextView tvFromUser;
        private TextView tvDate;
        private Button btnAccept;
        private Button btnDecline;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAllianceName = itemView.findViewById(R.id.tv_alliance_name);
            tvFromUser = itemView.findViewById(R.id.tv_from_user);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);

            btnAccept.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onAcceptInvitation(invitations.get(getAdapterPosition()));
                }
            });

            btnDecline.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeclineInvitation(invitations.get(getAdapterPosition()));
                }
            });
        }

        public void bind(AllianceInvitation invitation) {
            tvAllianceName.setText(invitation.getAllianceName());
            tvFromUser.setText("Poziv od: " + invitation.getFromUsername());

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(invitation.getCreatedAt()));
        }
    }
}