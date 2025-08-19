package com.example.ma2025.data.repositories;

import android.util.Log;
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.data.models.AllianceInvitation;
import com.example.ma2025.data.models.AllianceMember;
import com.example.ma2025.data.models.AllianceMessage;
import com.example.ma2025.data.models.User;
import com.example.ma2025.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AllianceRepository {
    private static final String TAG = "AllianceRepository";
    private final FirebaseFirestore db;

    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnAllianceLoadedListener {
        void onSuccess(Alliance alliance);
        void onError(String error);
        void onNotInAlliance();
    }

    public interface OnOperationCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface OnInvitationsListener {
        void onSuccess(List<AllianceInvitation> invitations);
        void onError(String error);
    }

    public interface OnMessagesListener {
        void onSuccess(List<AllianceMessage> messages);
        void onError(String error);
    }

    public void getUserAlliance(String userId, OnAllianceLoadedListener listener) {
        db.collection(Constants.COLLECTION_ALLIANCES)
                .whereArrayContains("memberIds", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onNotInAlliance();
                    } else {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Alliance alliance = doc.toObject(Alliance.class);
                        alliance.setId(doc.getId());

                        // Load alliance members details
                        loadAllianceMembers(alliance, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user alliance", e);
                    listener.onError("Greška pri učitavanju saveza");
                });
    }

    private void loadAllianceMembers(Alliance alliance, OnAllianceLoadedListener listener) {
        List<AllianceMember> members = new ArrayList<>();
        List<String> memberIds = alliance.getMemberIds();

        if (memberIds.isEmpty()) {
            alliance.setMembers(members);
            listener.onSuccess(alliance);
            return;
        }

        for (String memberId : memberIds) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                String role = alliance.isLeader(user.getUid()) ? "leader" : "member";
                                AllianceMember member = new AllianceMember(
                                        user.getUid(),
                                        user.getUsername(),
                                        user.getAvatar(),
                                        user.getLevel(),
                                        user.getTitle(),
                                        role
                                );
                                members.add(member);
                            }
                        }

                        // Check if all members loaded
                        if (members.size() == memberIds.size()) {
                            alliance.setMembers(members);
                            listener.onSuccess(alliance);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading member: " + memberId, e);
                        // Continue even if one member fails to load
                        if (members.size() == memberIds.size() - 1) {
                            alliance.setMembers(members);
                            listener.onSuccess(alliance);
                        }
                    });
        }
    }

    public void createAlliance(String userId, String allianceName, OnOperationCompleteListener listener) {
        // First check if user is already in an alliance
        getUserAlliance(userId, new OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                listener.onError("Već ste član saveza: " + alliance.getName());
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }

            @Override
            public void onNotInAlliance() {
                // User is not in alliance, proceed with creation
                db.collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    Alliance alliance = new Alliance(allianceName, userId, user.getUsername());

                                    db.collection(Constants.COLLECTION_ALLIANCES)
                                            .add(alliance)
                                            .addOnSuccessListener(documentReference -> {
                                                listener.onSuccess("Savez '" + allianceName + "' je uspešno kreiran!");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error creating alliance", e);
                                                listener.onError("Greška pri kreiranju saveza");
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting user info", e);
                            listener.onError("Greška pri dobijanju korisničkih podataka");
                        });
            }
        });
    }

    public void inviteToAlliance(String allianceId, String allianceName, String fromUserId,
                                 String fromUsername, String toUserId, OnOperationCompleteListener listener) {

        // Check if user is already in alliance
        getUserAlliance(toUserId, new OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                listener.onError("Korisnik je već član saveza");
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }

            @Override
            public void onNotInAlliance() {
                // Check if invitation already exists
                db.collection(Constants.COLLECTION_ALLIANCE_INVITATIONS)
                        .whereEqualTo("toUserId", toUserId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                listener.onError("Korisnik već ima poziv za savez");
                            } else {
                                // Create invitation
                                AllianceInvitation invitation = new AllianceInvitation(
                                        allianceId, allianceName, fromUserId, fromUsername, toUserId
                                );

                                db.collection(Constants.COLLECTION_ALLIANCE_INVITATIONS)
                                        .add(invitation)
                                        .addOnSuccessListener(documentReference -> {
                                            // Send notification to invited user
                                            sendInvitationNotification(toUserId, allianceName, fromUsername);
                                            listener.onSuccess("Poziv je poslat!");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error creating invitation", e);
                                            listener.onError("Greška pri slanju poziva");
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking existing invitations", e);
                            listener.onError("Greška pri proveri postojećih poziva");
                        });
            }
        });
    }

    public void getPendingInvitations(String userId, OnInvitationsListener listener) {
        db.collection(Constants.COLLECTION_ALLIANCE_INVITATIONS)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting invitations", error);
                        listener.onError("Greška pri učitavanju poziva");
                        return;
                    }

                    List<AllianceInvitation> invitations = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                            invitation.setId(doc.getId());
                            invitations.add(invitation);
                        }
                    }
                    listener.onSuccess(invitations);
                });
    }

    public void respondToInvitation(String invitationId, boolean accept, OnOperationCompleteListener listener) {
        db.collection(Constants.COLLECTION_ALLIANCE_INVITATIONS)
                .document(invitationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AllianceInvitation invitation = documentSnapshot.toObject(AllianceInvitation.class);
                        if (invitation != null) {
                            if (accept) {
                                acceptInvitation(invitation, listener);
                            } else {
                                declineInvitation(invitationId, listener);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting invitation", e);
                    listener.onError("Greška pri obradi poziva");
                });
    }

    private void acceptInvitation(AllianceInvitation invitation, OnOperationCompleteListener listener) {
        // First check if user is already in another alliance
        getUserAlliance(invitation.getToUserId(), new OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance existingAlliance) {
                // User is in alliance, need to leave first
                if (existingAlliance.isMissionActive()) {
                    listener.onError("Ne možete napustiti savez tokom aktivne misije");
                } else {
                    leaveAlliance(existingAlliance.getId(), invitation.getToUserId(), () -> {
                        joinAlliance(invitation, listener);
                    });
                }
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }

            @Override
            public void onNotInAlliance() {
                joinAlliance(invitation, listener);
            }
        });
    }

    private void joinAlliance(AllianceInvitation invitation, OnOperationCompleteListener listener) {
        // Add user to alliance
        db.collection(Constants.COLLECTION_ALLIANCES)
                .document(invitation.getAllianceId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Alliance alliance = documentSnapshot.toObject(Alliance.class);
                        if (alliance != null) {
                            alliance.addMember(invitation.getToUserId());
                            alliance.setUpdatedAt(new Date());

                            db.collection(Constants.COLLECTION_ALLIANCES)
                                    .document(invitation.getAllianceId())
                                    .set(alliance)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update invitation status
                                        updateInvitationStatus(invitation.getId(), "accepted", () -> {
                                            // Notify alliance leader
                                            notifyLeaderOfAcceptance(alliance.getLeaderId(), invitation.getToUserId());
                                            listener.onSuccess("Uspešno ste se pridružili savezu!");
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error joining alliance", e);
                                        listener.onError("Greška pri pridruživanju savezu");
                                    });
                        }
                    } else {
                        listener.onError("Savez ne postoji");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting alliance", e);
                    listener.onError("Greška pri dobijanju podataka o savezu");
                });
    }

    private void declineInvitation(String invitationId, OnOperationCompleteListener listener) {
        updateInvitationStatus(invitationId, "declined", () -> {
            listener.onSuccess("Poziv je odbačen");
        });
    }

    private void updateInvitationStatus(String invitationId, String status, Runnable onComplete) {
        db.collection(Constants.COLLECTION_ALLIANCE_INVITATIONS)
                .document(invitationId)
                .update("status", status, "respondedAt", new Date())
                .addOnSuccessListener(aVoid -> {
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                    if (onComplete != null) onComplete.run();
                });
    }

    private void leaveAlliance(String allianceId, String userId, Runnable onComplete) {
        db.collection(Constants.COLLECTION_ALLIANCES)
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Alliance alliance = documentSnapshot.toObject(Alliance.class);
                        if (alliance != null) {
                            alliance.removeMember(userId);
                            alliance.setUpdatedAt(new Date());

                            db.collection(Constants.COLLECTION_ALLIANCES)
                                    .document(allianceId)
                                    .set(alliance)
                                    .addOnSuccessListener(aVoid -> {
                                        if (onComplete != null) onComplete.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error leaving alliance", e);
                                        if (onComplete != null) onComplete.run();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting alliance for leaving", e);
                    if (onComplete != null) onComplete.run();
                });
    }

    public void deleteAlliance(String allianceId, String userId, OnOperationCompleteListener listener) {
        db.collection(Constants.COLLECTION_ALLIANCES)
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Alliance alliance = documentSnapshot.toObject(Alliance.class);
                        if (alliance != null && alliance.isLeader(userId)) {
                            if (alliance.isMissionActive()) {
                                listener.onError("Ne možete obrisati savez tokom aktivne misije");
                            } else {
                                db.collection(Constants.COLLECTION_ALLIANCES)
                                        .document(allianceId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            listener.onSuccess("Savez je uspešno obrisan");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error deleting alliance", e);
                                            listener.onError("Greška pri brisanju saveza");
                                        });
                            }
                        } else {
                            listener.onError("Nemate dozvolu za brisanje saveza");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting alliance for deletion", e);
                    listener.onError("Greška pri dobijanju podataka o savezu");
                });
    }

    public void sendMessage(String allianceId, String senderId, String senderUsername,
                            String message, OnOperationCompleteListener listener) {
        AllianceMessage allianceMessage = new AllianceMessage(allianceId, senderId, senderUsername, message);

        db.collection(Constants.COLLECTION_ALLIANCE_MESSAGES)
                .add(allianceMessage)
                .addOnSuccessListener(documentReference -> {
                    // Notify other alliance members
                    notifyAllianceMembersOfMessage(allianceId, senderId, senderUsername);
                    listener.onSuccess("Poruka je poslata");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    listener.onError("Greška pri slanju poruke");
                });
    }

    public void getMessages(String allianceId, OnMessagesListener listener) {
        db.collection(Constants.COLLECTION_ALLIANCE_MESSAGES)
                .whereEqualTo("allianceId", allianceId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting messages", error);
                        listener.onError("Greška pri učitavanju poruka");
                        return;
                    }

                    List<AllianceMessage> messages = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            AllianceMessage message = doc.toObject(AllianceMessage.class);
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                    }
                    listener.onSuccess(messages);
                });
    }

    private void sendInvitationNotification(String userId, String allianceName, String fromUsername) {
        // Implementation for sending notification
        // This would use Android's notification system
    }

    private void notifyLeaderOfAcceptance(String leaderId, String acceptedUserId) {
        // Implementation for notifying leader
        // This would use Android's notification system
    }

    private void notifyAllianceMembersOfMessage(String allianceId, String senderId, String senderUsername) {
        // Implementation for notifying alliance members of new message
        // This would use Android's notification system
    }
}