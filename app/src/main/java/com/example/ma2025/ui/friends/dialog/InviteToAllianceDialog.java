package com.example.ma2025.ui.friends.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Friend;
import com.example.ma2025.ui.friends.adapter.FriendSelectionAdapter;
import java.util.List;

public class InviteToAllianceDialog extends DialogFragment {

    private List<Friend> friends;
    private FriendSelectionAdapter adapter;
    private OnFriendsInvitedListener listener;

    public interface OnFriendsInvitedListener {
        void onFriendsInvited(List<Friend> selectedFriends);
    }

    public void setFriends(List<Friend> friends) {
        this.friends = friends;
    }

    public void setOnFriendsInvitedListener(OnFriendsInvitedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_invite_friends, null);

        RecyclerView recyclerView = view.findViewById(R.id.rv_friends);
        adapter = new FriendSelectionAdapter(friends);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Pozovi prijatelje u savez")
                .setMessage("Izaberite prijatelje koje želite da pozovete:")
                .setView(view)
                .setPositiveButton("Pošalji pozive", (dialog, which) -> {
                    if (listener != null) {
                        List<Friend> selectedFriends = adapter.getSelectedFriends();
                        listener.onFriendsInvited(selectedFriends);
                    }
                })
                .setNegativeButton("Otkaži", null)
                .create();
    }
}