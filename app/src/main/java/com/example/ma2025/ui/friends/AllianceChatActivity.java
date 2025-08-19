package com.example.ma2025.ui.friends;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ma2025.R;
import com.example.ma2025.data.models.AllianceMessage;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.data.repositories.AllianceRepository;
import com.example.ma2025.databinding.ActivityAllianceChatBinding;
import com.example.ma2025.ui.friends.adapter.AllianceChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class AllianceChatActivity extends AppCompatActivity {

    private static final String TAG = "AllianceChatActivity";
    private ActivityAllianceChatBinding binding;

    private FirebaseAuth mAuth;
    private PreferencesManager preferencesManager;
    private AllianceRepository allianceRepository;

    private AllianceChatAdapter chatAdapter;
    private List<AllianceMessage> messagesList = new ArrayList<>();

    private String allianceId;
    private String allianceName;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllianceChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        getIntentData();
        setupUI();
        loadMessages();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        preferencesManager = new PreferencesManager(this);
        allianceRepository = new AllianceRepository();

        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        currentUsername = preferencesManager.getCachedUsername();
    }

    private void getIntentData() {
        allianceId = getIntent().getStringExtra("alliance_id");
        allianceName = getIntent().getStringExtra("alliance_name");

        if (allianceId == null || allianceName == null) {
            Toast.makeText(this, "Greška pri učitavanju chat-a", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void setupUI() {
        setupToolbar();
        setupRecyclerView();
        setupSendButton();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(allianceName);
            getSupportActionBar().setSubtitle("Chat saveza");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new AllianceChatAdapter(messagesList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(chatAdapter);
    }

    private void setupSendButton() {
        binding.btnSend.setOnClickListener(v -> sendMessage());

        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadMessages() {
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        allianceRepository.getMessages(allianceId, new AllianceRepository.OnMessagesListener() {
            @Override
            public void onSuccess(List<AllianceMessage> messages) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                messagesList.clear();
                messagesList.addAll(messages);
                chatAdapter.notifyDataSetChanged();

                // Scroll to bottom
                if (!messagesList.isEmpty()) {
                    binding.rvMessages.scrollToPosition(messagesList.size() - 1);
                }

                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(AllianceChatActivity.this, error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Unesite poruku", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null || currentUsername == null) {
            Toast.makeText(this, "Greška: korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable send button temporarily
        binding.btnSend.setEnabled(false);

        allianceRepository.sendMessage(allianceId, currentUserId, currentUsername, messageText,
                new AllianceRepository.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        binding.btnSend.setEnabled(true);
                        binding.etMessage.setText("");
                        // Messages will be updated automatically via listener
                    }

                    @Override
                    public void onError(String error) {
                        binding.btnSend.setEnabled(true);
                        Toast.makeText(AllianceChatActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (messagesList.isEmpty()) {
            binding.tvEmptyState.setVisibility(android.view.View.VISIBLE);
            binding.tvEmptyState.setText("Budite prvi koji će poslati poruku u ovom savezu!");
        } else {
            binding.tvEmptyState.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}