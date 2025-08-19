package com.example.ma2025.data.repositories;

import android.util.Log;
import com.example.ma2025.data.models.Friend;
import com.example.ma2025.data.models.User;
import com.example.ma2025.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendsRepository {
    private static final String TAG = "FriendsRepository";
    private final FirebaseFirestore db;

    public FriendsRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnFriendsLoadedListener {
        void onSuccess(List<Friend> friends);
        void onError(String error);
    }

    public interface OnUserSearchListener {
        void onSuccess(List<User> users);
        void onError(String error);
    }

    public interface OnOperationCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public void getFriends(String userId, OnFriendsLoadedListener listener) {
        db.collection(Constants.COLLECTION_FRIENDS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "accepted")
                .orderBy("friendUsername")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting friends", error);
                        listener.onError("Greška pri učitavanju prijatelja");
                        return;
                    }

                    List<Friend> friends = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Friend friend = doc.toObject(Friend.class);
                            friend.setId(doc.getId());
                            friends.add(friend);
                        }
                    }
                    listener.onSuccess(friends);
                });
    }

    public void searchUsers(String query, String currentUserId, OnUserSearchListener listener) {
        if (query.trim().isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        // Don't include current user in search results
                        if (!user.getUid().equals(currentUserId)) {
                            users.add(user);
                        }
                    }
                    listener.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching users", e);
                    listener.onError("Greška pri pretrazi korisnika");
                });
    }

    public void addFriend(String userId, String friendId, OnOperationCompleteListener listener) {
        // First get friend's info
        db.collection(Constants.COLLECTION_USERS)
                .document(friendId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User friendUser = documentSnapshot.toObject(User.class);
                        if (friendUser != null) {
                            // Create friendship record
                            Friend friend = new Friend(
                                    friendUser.getUid(),
                                    friendUser.getUsername(),
                                    friendUser.getAvatar(),
                                    friendUser.getLevel(),
                                    friendUser.getTitle()
                            );

                            // Add to current user's friends
                            db.collection(Constants.COLLECTION_FRIENDS)
                                    .add(friend.toMap(userId))
                                    .addOnSuccessListener(documentReference -> {
                                        // Add reciprocal friendship
                                        getUserInfo(userId, (currentUser) -> {
                                            Friend reciprocalFriend = new Friend(
                                                    currentUser.getUid(),
                                                    currentUser.getUsername(),
                                                    currentUser.getAvatar(),
                                                    currentUser.getLevel(),
                                                    currentUser.getTitle()
                                            );

                                            db.collection(Constants.COLLECTION_FRIENDS)
                                                    .add(reciprocalFriend.toMap(friendId))
                                                    .addOnSuccessListener(docRef -> {
                                                        listener.onSuccess("Prijatelj je uspešno dodat!");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error adding reciprocal friend", e);
                                                        listener.onError("Greška pri dodavanju prijatelja");
                                                    });
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error adding friend", e);
                                        listener.onError("Greška pri dodavanju prijatelja");
                                    });
                        }
                    } else {
                        listener.onError("Korisnik nije pronađen");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting friend info", e);
                    listener.onError("Greška pri dobijanju podataka o korisniku");
                });
    }

    public void addFriendByQR(String userId, String scannedUserId, OnOperationCompleteListener listener) {
        if (userId.equals(scannedUserId)) {
            listener.onError("Ne možete dodati sebe kao prijatelja");
            return;
        }

        // Check if already friends
        checkIfAlreadyFriends(userId, scannedUserId, (isAlreadyFriend) -> {
            if (isAlreadyFriend) {
                listener.onError("Ovaj korisnik je već vaš prijatelj");
            } else {
                addFriend(userId, scannedUserId, listener);
            }
        });
    }

    private void checkIfAlreadyFriends(String userId, String friendId,
                                       OnCheckFriendshipListener listener) {
        db.collection(Constants.COLLECTION_FRIENDS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("friendId", friendId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onResult(!queryDocumentSnapshots.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking friendship", e);
                    listener.onResult(false);
                });
    }

    private void getUserInfo(String userId, OnUserInfoListener listener) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            listener.onUserLoaded(user);
                        }
                    }
                });
    }

    private interface OnCheckFriendshipListener {
        void onResult(boolean isAlreadyFriend);
    }

    private interface OnUserInfoListener {
        void onUserLoaded(User user);
    }
}