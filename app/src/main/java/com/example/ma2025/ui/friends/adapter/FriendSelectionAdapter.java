package com.example.ma2025.ui.friends.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Friend;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendSelectionAdapter extends RecyclerView.Adapter<FriendSelectionAdapter.FriendSelectionViewHolder> {

    private List<Friend> friends;
    private Set<String> selectedFriendIds = new HashSet<>();

    public FriendSelectionAdapter(List<Friend> friends) {
        this.friends = friends != null ? friends : new ArrayList<>();
    }

    @NonNull
    @Override
    public FriendSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_selection, parent, false);
        return new FriendSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendSelectionViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public List<Friend> getSelectedFriends() {
        List<Friend> selectedFriends = new ArrayList<>();
        for (Friend friend : friends) {
            if (selectedFriendIds.contains(friend.getFriendId())) {
                selectedFriends.add(friend);
            }
        }
        return selectedFriends;
    }

    class FriendSelectionViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbSelect;
        private ImageView ivAvatar;
        private TextView tvUsername;
        private TextView tvLevel;

        public FriendSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvLevel = itemView.findViewById(R.id.tv_level);

            itemView.setOnClickListener(v -> {
                cbSelect.toggle();
            });

            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Friend friend = friends.get(position);
                    if (isChecked) {
                        selectedFriendIds.add(friend.getFriendId());
                    } else {
                        selectedFriendIds.remove(friend.getFriendId());
                    }
                }
            });
        }

        public void bind(Friend friend) {
            tvUsername.setText(friend.getFriendUsername());
            tvLevel.setText("Nivo " + friend.getFriendLevel());
            cbSelect.setChecked(selectedFriendIds.contains(friend.getFriendId()));
            setAvatarImage(friend.getFriendAvatar());
        }

        private void setAvatarImage(String avatar) {
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