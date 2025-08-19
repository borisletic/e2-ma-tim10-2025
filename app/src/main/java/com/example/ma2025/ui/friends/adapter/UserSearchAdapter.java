package com.example.ma2025.ui.friends.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.User;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAddFriend(User user);
        void onViewProfile(User user);
    }

    public UserSearchAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvUsername;
        private TextView tvLevel;
        private TextView tvTitle;
        private Button btnAddFriend;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLevel = itemView.findViewById(R.id.tv_level);
            tvTitle = itemView.findViewById(R.id.tv_title);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);

            btnAddFriend.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onAddFriend(users.get(getAdapterPosition()));
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onViewProfile(users.get(getAdapterPosition()));
                }
            });
        }

        public void bind(User user) {
            tvUsername.setText(user.getUsername());
            tvLevel.setText("Nivo " + user.getLevel());
            tvTitle.setText(user.getTitle());

            // Set avatar based on user's avatar
            setAvatarImage(user.getAvatar());
        }

        private void setAvatarImage(String avatar) {
            // Implementation for setting avatar image
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