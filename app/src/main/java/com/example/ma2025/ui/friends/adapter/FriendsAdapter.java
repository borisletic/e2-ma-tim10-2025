package com.example.ma2025.ui.friends.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Friend;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<Friend> friends;
    private OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onViewProfile(Friend friend);
    }

    public FriendsAdapter(List<Friend> friends, OnFriendActionListener listener) {
        this.friends = friends;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvUsername;
        private TextView tvLevel;
        private TextView tvTitle;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvTitle = itemView.findViewById(R.id.tv_title);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onViewProfile(friends.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Friend friend) {
            tvUsername.setText(friend.getFriendUsername());
            tvLevel.setText("Nivo " + friend.getFriendLevel());
            tvTitle.setText(friend.getFriendTitle());

            // Set avatar based on friend's avatar
            setAvatarImage(friend.getFriendAvatar());
        }

        private void setAvatarImage(String avatar) {
            // Implementation for setting avatar image
            // You would typically use Glide or similar library here
            switch (avatar) {
                case "avatar_1":
                    ivAvatar.setImageResource(R.drawable.avatar_1);
                    break;
                case "avatar_2":
                    ivAvatar.setImageResource(R.drawable.avatar_2);
                    break;
                case "avatar_3":
                    ivAvatar.setImageResource(R.drawable.avatar_3);
                    break;
                case "avatar_4":
                    ivAvatar.setImageResource(R.drawable.avatar_4);
                    break;
                case "avatar_5":
                    ivAvatar.setImageResource(R.drawable.avatar_5);
                    break;
                default:
                    ivAvatar.setImageResource(R.drawable.ic_person);
                    break;
            }
        }
    }
}