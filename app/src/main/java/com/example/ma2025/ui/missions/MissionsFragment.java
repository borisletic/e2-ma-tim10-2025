package com.example.ma2025.ui.missions;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.repositories.AllianceRepository;
import com.example.ma2025.data.repositories.SpecialMissionRepository;
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.data.models.SpecialMission;
import com.example.ma2025.data.models.MissionProgress;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import com.example.ma2025.ui.missions.adapter.MemberProgressAdapter;

public class MissionsFragment extends Fragment {

    private static final String TAG = "MissionsFragment";

    // UI komponente
    private Button btnStartMission;
    private TextView tvMissionStatus;
    private ProgressBar progressBarBoss;
    private TextView tvBossHp;
    private TextView tvTimeRemaining;
    private RecyclerView recyclerViewProgress;
    private View layoutMissionActive;
    private View layoutNoMission;

    // Komponente za individualni napredak
    private TextView tvStoreVisits;
    private TextView tvSuccessfulAttacks;
    private TextView tvEasyTasks;
    private TextView tvHardTasks;
    private TextView tvMessageDays;
    private TextView tvNoFailedTasks;
    private TextView tvUserDamage;

    // Data
    private Alliance currentAlliance;
    private SpecialMission activeMission;
    private String currentUserId;
    private AllianceRepository allianceRepository;
    private MemberProgressAdapter memberProgressAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_missions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        setupListeners();
        loadUserAlliance();
    }

    private void initializeComponents(View view) {
        // Main controls
        btnStartMission = view.findViewById(R.id.btn_start_mission);
        tvMissionStatus = view.findViewById(R.id.tv_mission_status);

        // Mission progress
        progressBarBoss = view.findViewById(R.id.progress_bar_boss);
        tvBossHp = view.findViewById(R.id.tv_boss_hp);
        tvTimeRemaining = view.findViewById(R.id.tv_time_remaining);

        // Layouts
        layoutMissionActive = view.findViewById(R.id.layout_mission_active);
        layoutNoMission = view.findViewById(R.id.layout_no_mission);

        // User progress
        tvStoreVisits = view.findViewById(R.id.tv_store_visits);
        tvSuccessfulAttacks = view.findViewById(R.id.tv_successful_attacks);
        tvEasyTasks = view.findViewById(R.id.tv_easy_tasks);
        tvHardTasks = view.findViewById(R.id.tv_hard_tasks);
        tvMessageDays = view.findViewById(R.id.tv_message_days);
        tvNoFailedTasks = view.findViewById(R.id.tv_no_failed_tasks);
        tvUserDamage = view.findViewById(R.id.tv_user_damage);

        // RecyclerView for member progress
        recyclerViewProgress = view.findViewById(R.id.recycler_view_member_progress);
        recyclerViewProgress.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter
        memberProgressAdapter = new MemberProgressAdapter(null, null);
        recyclerViewProgress.setAdapter(memberProgressAdapter);

        // Initialize repositories
        allianceRepository = new AllianceRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupListeners() {
        btnStartMission.setOnClickListener(v -> startSpecialMission());
    }

    private void loadUserAlliance() {
        allianceRepository.getUserAlliance(currentUserId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                currentAlliance = alliance;
                loadActiveMission();
                updateStartButtonVisibility();
            }

            @Override
            public void onError(String error) {
                showError("Greška pri učitavanju saveza: " + error);
                showNoAllianceState();
            }

            @Override
            public void onNotInAlliance() {
                showNoAllianceState();
            }
        });
    }

    private void loadActiveMission() {
        if (currentAlliance == null) return;

        SpecialMissionRepository.getInstance().getActiveMission(currentAlliance.getId())
                .observe(this, mission -> {
                    activeMission = mission;
                    updateUI();
                });
    }

    private void updateUI() {
        if (activeMission != null) {
            showActiveMissionState();
            updateMissionProgress();
            updateUserProgress();
            updateTimeRemaining();
        } else {
            showNoMissionState();
        }
    }

    private void showActiveMissionState() {
        layoutMissionActive.setVisibility(View.VISIBLE);
        layoutNoMission.setVisibility(View.GONE);
        btnStartMission.setVisibility(View.GONE);
        tvMissionStatus.setText("Aktivna specijalna misija");
    }

    private void showNoMissionState() {
        layoutMissionActive.setVisibility(View.GONE);
        layoutNoMission.setVisibility(View.VISIBLE);
        tvMissionStatus.setText("Nema aktivne specijalne misije");
    }

    private void showNoAllianceState() {
        layoutMissionActive.setVisibility(View.GONE);
        layoutNoMission.setVisibility(View.VISIBLE);
        btnStartMission.setVisibility(View.GONE);
        tvMissionStatus.setText("Morate biti član saveza da biste učestvovali u specijalnim misijama");
    }

    private void updateStartButtonVisibility() {
        if (currentAlliance != null && currentAlliance.isLeader(currentUserId)) {
            btnStartMission.setVisibility(View.VISIBLE);
        } else {
            btnStartMission.setVisibility(View.GONE);
        }
    }

    private void updateMissionProgress() {
        if (activeMission == null) return;

        progressBarBoss.setMax(activeMission.getMaxBossHp());
        progressBarBoss.setProgress(activeMission.getMaxBossHp() - activeMission.getBossHp());

        tvBossHp.setText(String.format("Boss HP: %d/%d",
                activeMission.getBossHp(), activeMission.getMaxBossHp()));
    }

    private void updateUserProgress() {
        if (activeMission == null) return;

        MissionProgress userProgress = activeMission.getMemberProgress().get(currentUserId);
        if (userProgress == null) {
            // Inicijalizuj prazan progress
            userProgress = new MissionProgress(currentUserId);
        }

        tvStoreVisits.setText(String.format("Kupovine: %d/5", userProgress.getStoreVisits()));
        tvSuccessfulAttacks.setText(String.format("Uspešni napadi: %d/10", userProgress.getSuccessfulAttacks()));
        tvEasyTasks.setText(String.format("Laki zadaci: %d/10", userProgress.getEasyTasksCompleted()));
        tvHardTasks.setText(String.format("Teški zadaci: %d/6", userProgress.getHardTasksCompleted()));
        tvMessageDays.setText(String.format("Dani sa porukama: %d", userProgress.getMessageDaysCount()));
        tvUserDamage.setText(String.format("Ukupna šteta: %d HP", userProgress.getTotalDamageDealt()));

        // Update no failed tasks indicator
        tvNoFailedTasks.setText(userProgress.isNoFailedTasks() ?
                "Bez neuspešnih zadataka" : "Ima neuspešnih zadataka");

        // Update member progress adapter
        if (currentAlliance != null && memberProgressAdapter != null) {
            memberProgressAdapter.updateData(currentAlliance.getMembers(), activeMission.getMemberProgress());
        }
    }

    private void updateTimeRemaining() {
        if (activeMission == null) return;

        long remainingTime = activeMission.getRemainingTime();

        if (remainingTime > 0) {
            long days = TimeUnit.MILLISECONDS.toDays(remainingTime);
            long hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;

            tvTimeRemaining.setText(String.format("Preostalo vreme: %dd %dh %dm", days, hours, minutes));
        } else {
            tvTimeRemaining.setText("Misija je istekla");
        }
    }

    private void startSpecialMission() {
        if (currentAlliance == null) {
            showError("Nema aktivnog saveza");
            return;
        }

        if (!currentAlliance.isLeader(currentUserId)) {
            showError("Samo vođa saveza može pokrenuti specijalnu misiju");
            return;
        }

        // Show confirmation dialog
        showStartMissionDialog();
    }

    private void showStartMissionDialog() {
        int memberCount = currentAlliance.getMembers().size();
        int totalBossHp = memberCount * 100;

        String message = "DETALJI MISIJE:\n\n" +
                "• Boss HP: " + totalBossHp + " (" + memberCount + " članova × 100)\n" +
                "• Trajanje: 14 dana\n" +
                "• Može se pokrenuti samo jednom\n" +
                "• Automatski kreće za sve članove\n\n" +
                "⚠️ PAŽNJA: Specijalna misija ne može biti prekinuta nakon pokretanja!";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pokretanje Specijalne Misije")
                .setMessage(message)
                .setPositiveButton("Pokreni Misiju", (dialog, which) -> {
                    executeStartMission();
                })
                .setNegativeButton("Odustani", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void executeStartMission() {
        btnStartMission.setEnabled(false);

        SpecialMissionRepository.getInstance().createSpecialMission(
                currentAlliance.getId(),
                currentUserId,
                currentAlliance.getMemberIds(),
                new SpecialMissionRepository.OnMissionCreatedCallback() {
                    @Override
                    public void onSuccess(SpecialMission mission) {
                        btnStartMission.setEnabled(true);
                        showSuccess("Specijalna misija je pokrenuta!");
                        loadActiveMission(); // Reload to show the new mission
                    }

                    @Override
                    public void onError(String error) {
                        btnStartMission.setEnabled(true);
                        showError("Greška pri pokretanju misije: " + error);
                    }
                }
        );
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, message);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }
}