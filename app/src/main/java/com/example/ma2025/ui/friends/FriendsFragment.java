package com.example.ma2025.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.data.models.AllianceInvitation;
import com.example.ma2025.data.models.AllianceMember;
import com.example.ma2025.data.models.Friend;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.data.repositories.AllianceRepository;
import com.example.ma2025.data.repositories.FriendsRepository;
import com.example.ma2025.databinding.FragmentFriendsBinding;
import com.example.ma2025.ui.friends.adapter.FriendsAdapter;
import com.example.ma2025.ui.friends.adapter.InvitationsAdapter;
import com.example.ma2025.ui.friends.adapter.UserSearchAdapter;
import com.example.ma2025.ui.friends.dialog.CreateAllianceDialog;
import com.example.ma2025.ui.friends.dialog.InviteToAllianceDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment implements
        FriendsAdapter.OnFriendActionListener,
        UserSearchAdapter.OnUserActionListener,
        InvitationsAdapter.OnInvitationActionListener {

    private static final String TAG = "FriendsFragment";
    private FragmentFriendsBinding binding;

    private FirebaseAuth mAuth;
    private PreferencesManager preferencesManager;
    private FriendsRepository friendsRepository;
    private AllianceRepository allianceRepository;

    private FriendsAdapter friendsAdapter;
    private UserSearchAdapter searchAdapter;
    private InvitationsAdapter invitationsAdapter;

    private List<Friend> friendsList = new ArrayList<>();
    private List<User> searchResults = new ArrayList<>();
    private List<AllianceInvitation> invitationsList = new ArrayList<>();

    private Alliance currentAlliance;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupUI();
        loadData();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        preferencesManager = new PreferencesManager(requireContext());
        friendsRepository = new FriendsRepository();
        allianceRepository = new AllianceRepository();
    }

    private void setupUI() {
        setupRecyclerViews();
        setupSearchFunctionality();
        setupButtons();
        setupTabs();
    }

    private void setupRecyclerViews() {
        // Friends RecyclerView
        friendsAdapter = new FriendsAdapter(friendsList, this);
        binding.rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFriends.setAdapter(friendsAdapter);

        // Search Results RecyclerView
        searchAdapter = new UserSearchAdapter(searchResults, this);
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSearchResults.setAdapter(searchAdapter);

        // Invitations RecyclerView
        invitationsAdapter = new InvitationsAdapter(invitationsList, this);
        binding.rvInvitations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvInvitations.setAdapter(invitationsAdapter);
    }

    private void setupSearchFunctionality() {
        binding.etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        binding.btnScanQr.setOnClickListener(v -> startQRScanner());
        binding.btnCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        binding.btnInviteToAlliance.setOnClickListener(v -> showInviteToAllianceDialog());
        binding.btnLeaveAlliance.setOnClickListener(v -> leaveAlliance());
        binding.btnDeleteAlliance.setOnClickListener(v -> deleteAlliance());
        binding.btnAllianceChat.setOnClickListener(v -> openAllianceChat());
    }

    private void setupTabs() {
        binding.tabFriends.setOnClickListener(v -> showTab(0));
        binding.tabAlliance.setOnClickListener(v -> showTab(1));
        binding.tabInvitations.setOnClickListener(v -> showTab(2));

        // Default to friends tab
        showTab(0);
    }

    private void showTab(int tabIndex) {
        // Reset all tabs
        binding.tabFriends.setSelected(false);
        binding.tabAlliance.setSelected(false);
        binding.tabInvitations.setSelected(false);

        // Hide all content
        binding.layoutFriends.setVisibility(View.GONE);
        binding.layoutAlliance.setVisibility(View.GONE);
        binding.layoutInvitations.setVisibility(View.GONE);

        switch (tabIndex) {
            case 0: // Friends
                binding.tabFriends.setSelected(true);
                binding.layoutFriends.setVisibility(View.VISIBLE);
                break;
            case 1: // Alliance
                binding.tabAlliance.setSelected(true);
                binding.layoutAlliance.setVisibility(View.VISIBLE);
                loadAllianceData();
                break;
            case 2: // Invitations
                binding.tabInvitations.setSelected(true);
                binding.layoutInvitations.setVisibility(View.VISIBLE);
                loadInvitations();
                break;
        }
    }

    private void loadData() {
        loadFriends();
        loadAllianceData();
        loadInvitations();
    }

    private void loadFriends() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        binding.progressBarFriends.setVisibility(View.VISIBLE);

        friendsRepository.getFriends(userId, new FriendsRepository.OnFriendsLoadedListener() {
            @Override
            public void onSuccess(List<Friend> friends) {
                binding.progressBarFriends.setVisibility(View.GONE);
                friendsList.clear();
                friendsList.addAll(friends);
                friendsAdapter.notifyDataSetChanged();

                updateFriendsEmptyState();
            }

            @Override
            public void onError(String error) {
                binding.progressBarFriends.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                updateFriendsEmptyState();
            }
        });
    }

    private void loadAllianceData() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        // Proveri da li je binding još uvek validan
        if (binding == null) return;

        binding.progressBarAlliance.setVisibility(View.VISIBLE);

        allianceRepository.getUserAlliance(userId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                // Proveri da li je Fragment još aktivan
                if (binding == null || !isAdded()) return;

                binding.progressBarAlliance.setVisibility(View.GONE);
                currentAlliance = alliance;
                displayAllianceInfo(alliance);
                updateAllianceButtons(alliance, userId);
            }

            @Override
            public void onError(String error) {
                // Proveri da li je Fragment još aktivan
                if (binding == null || !isAdded()) return;

                binding.progressBarAlliance.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                showNoAllianceState();
            }

            @Override
            public void onNotInAlliance() {
                // Proveri da li je Fragment još aktivan
                if (binding == null || !isAdded()) return;

                binding.progressBarAlliance.setVisibility(View.GONE);
                currentAlliance = null;
                showNoAllianceState();
            }
        });
    }

    private void loadInvitations() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        binding.progressBarInvitations.setVisibility(View.VISIBLE);

        allianceRepository.getPendingInvitations(userId, new AllianceRepository.OnInvitationsListener() {
            @Override
            public void onSuccess(List<AllianceInvitation> invitations) {
                binding.progressBarInvitations.setVisibility(View.GONE);
                invitationsList.clear();
                invitationsList.addAll(invitations);
                invitationsAdapter.notifyDataSetChanged();

                updateInvitationsEmptyState();
                updateInvitationsBadge(invitations.size());
            }

            @Override
            public void onError(String error) {
                binding.progressBarInvitations.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                updateInvitationsEmptyState();
            }
        });
    }

    private void searchUsers(String query) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        if (query.trim().isEmpty()) {
            searchResults.clear();
            searchAdapter.notifyDataSetChanged();
            binding.layoutSearchResults.setVisibility(View.GONE);
            return;
        }

        binding.layoutSearchResults.setVisibility(View.VISIBLE);
        binding.progressBarSearch.setVisibility(View.VISIBLE);

        friendsRepository.searchUsers(query, userId, new FriendsRepository.OnUserSearchListener() {
            @Override
            public void onSuccess(List<User> users) {
                binding.progressBarSearch.setVisibility(View.GONE);
                searchResults.clear();
                searchResults.addAll(users);
                searchAdapter.notifyDataSetChanged();

                binding.tvNoSearchResults.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String error) {
                binding.progressBarSearch.setVisibility(View.GONE);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                binding.tvNoSearchResults.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startQRScanner() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("Skenirajte QR kod prijatelja");
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedUserId = result.getContents();
                addFriendByQR(scannedUserId);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addFriendByQR(String scannedUserId) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        friendsRepository.addFriendByQR(userId, scannedUserId, new FriendsRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadFriends(); // Refresh friends list
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateAllianceDialog() {
        CreateAllianceDialog dialog = new CreateAllianceDialog();
        dialog.setOnAllianceCreatedListener(allianceName -> {
            createAlliance(allianceName);
        });
        dialog.show(getParentFragmentManager(), "create_alliance");
    }

    private void createAlliance(String allianceName) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        allianceRepository.createAlliance(userId, allianceName, new AllianceRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadAllianceData(); // Refresh alliance data
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInviteToAllianceDialog() {
        if (currentAlliance == null) {
            Toast.makeText(getContext(), "Nemate aktivan savez", Toast.LENGTH_SHORT).show();
            return;
        }

        InviteToAllianceDialog dialog = new InviteToAllianceDialog();
        dialog.setFriends(friendsList);
        dialog.setOnFriendsInvitedListener(selectedFriends -> {
            inviteFriendsToAlliance(selectedFriends);
        });
        dialog.show(getParentFragmentManager(), "invite_to_alliance");
    }

    private void inviteFriendsToAlliance(List<Friend> selectedFriends) {
        String userId = getCurrentUserId();
        String username = preferencesManager.getCachedUsername();
        if (userId == null || currentAlliance == null) return;

        for (Friend friend : selectedFriends) {
            allianceRepository.inviteToAlliance(
                    currentAlliance.getId(),
                    currentAlliance.getName(),
                    userId,
                    username,
                    friend.getFriendId(),
                    new AllianceRepository.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            // Will be called for each successful invitation
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Greška pri pozivanju " + friend.getFriendUsername() + ": " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }

        Toast.makeText(getContext(), "Pozivi su poslati!", Toast.LENGTH_SHORT).show();
    }

    private void leaveAlliance() {
        if (currentAlliance == null) return;

        if (currentAlliance.isMissionActive()) {
            Toast.makeText(getContext(), "Ne možete napustiti savez tokom aktivne misije", Toast.LENGTH_SHORT).show();
            return;
        }

        // Implementation for leaving alliance
        // This would show a confirmation dialog and then call repository method
    }

    private void deleteAlliance() {
        if (currentAlliance == null) return;

        String userId = getCurrentUserId();
        if (userId == null) return;

        allianceRepository.deleteAlliance(currentAlliance.getId(), userId, new AllianceRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadAllianceData(); // Refresh alliance data
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAllianceChat() {
        if (currentAlliance == null) return;

        Intent intent = new Intent(getContext(), AllianceChatActivity.class);
        intent.putExtra("alliance_id", currentAlliance.getId());
        intent.putExtra("alliance_name", currentAlliance.getName());
        startActivity(intent);
    }

    private void displayAllianceInfo(Alliance alliance) {
        binding.layoutAllianceInfo.setVisibility(View.VISIBLE);
        binding.layoutNoAlliance.setVisibility(View.GONE);

        binding.tvAllianceName.setText(alliance.getName());
        binding.tvAllianceLeader.setText("Vođa: " + alliance.getLeaderUsername());
        binding.tvMemberCount.setText("Članovi: " + alliance.getMemberCount());

        // Display members list
        StringBuilder membersText = new StringBuilder();
        for (AllianceMember member : alliance.getMembers()) {
            if (membersText.length() > 0) membersText.append(", ");
            membersText.append(member.getUsername());
            if ("leader".equals(member.getRole())) {
                membersText.append(" (Vođa)");
            }
        }
        binding.tvAllianceMembers.setText(membersText.toString());
    }

    private void showNoAllianceState() {
        binding.layoutAllianceInfo.setVisibility(View.GONE);
        binding.layoutNoAlliance.setVisibility(View.VISIBLE);
    }

    private void updateAllianceButtons(Alliance alliance, String userId) {
        boolean isLeader = alliance.isLeader(userId);

        binding.btnInviteToAlliance.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        binding.btnDeleteAlliance.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        binding.btnLeaveAlliance.setVisibility(isLeader ? View.GONE : View.VISIBLE);
        binding.btnAllianceChat.setVisibility(View.VISIBLE);
    }

    private void updateFriendsEmptyState() {
        binding.tvNoFriends.setVisibility(friendsList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateInvitationsEmptyState() {
        binding.tvNoInvitations.setVisibility(invitationsList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateInvitationsBadge(int count) {
        if (count > 0) {
            binding.badgeInvitations.setVisibility(View.VISIBLE);
            binding.badgeInvitations.setText(String.valueOf(count));
        } else {
            binding.badgeInvitations.setVisibility(View.GONE);
        }
    }

    private String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    // Interface implementations
    @Override
    public void onAddFriend(User user) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        friendsRepository.addFriend(userId, user.getUid(), new FriendsRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadFriends(); // Refresh friends list
                // Clear search
                binding.etSearchUsers.setText("");
                searchResults.clear();
                searchAdapter.notifyDataSetChanged();
                binding.layoutSearchResults.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewProfile(Friend friend) {
        // Navigate to profile view
        Intent intent = new Intent(getContext(), UserProfileActivity.class);
        intent.putExtra("user_id", friend.getFriendId());
        startActivity(intent);
    }

    @Override
    public void onViewProfile(User user) {
        // Navigate to profile view
        Intent intent = new Intent(getContext(), UserProfileActivity.class);
        intent.putExtra("user_id", user.getUid());
        startActivity(intent);
    }

    @Override
    public void onAcceptInvitation(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation.getId(), true, new AllianceRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadInvitations(); // Refresh invitations
                loadAllianceData(); // Refresh alliance data
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeclineInvitation(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation.getId(), false, new AllianceRepository.OnOperationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadInvitations(); // Refresh invitations
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}