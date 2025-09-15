package com.example.ma2025.ui.boss;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.utils.BossAnimationManager;
import com.example.ma2025.viewmodels.BossViewModel;

import java.util.List;

public class BossFragment extends Fragment {

    private static final String TAG = "BossFragment";

    // UI Components
    private TextView tvBasePp;
    private TextView tvEquipmentBonus;
    private TextView tvTotalPp;
    private ProgressBar pbTotalPp, pbBossHp, pbBossHpBattle, pbPlayerPpBattle;
    private TextView tvBossLevel, tvBossHp, tvBossHpLabel, tvPlayerPpLabel, tvAttacksRemaining, tvSuccessChance, tvBattleMessage, tvNoEquipment;
    private Button btnRefresh, btnPrepareForBattle, btnAttack;
    private ImageView ivBossSprite;
    private LinearLayout llActiveEquipment;
    private BossAnimationManager animationManager;
    private int attacksRemaining = 5;
    private final int maxAttacks = 5;

    // ViewModel
    private BossViewModel bossViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boss, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupObservers();
        setupClickListeners();

        bossViewModel.refreshAllPpData();
    }

    private void initViews(View view) {
        tvBasePp = view.findViewById(R.id.tv_base_pp);
        tvEquipmentBonus = view.findViewById(R.id.tv_equipment_bonus);
        tvTotalPp = view.findViewById(R.id.tv_total_pp);
        pbTotalPp = view.findViewById(R.id.pb_total_pp);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvBossLevel = view.findViewById(R.id.tv_boss_level);
        tvBossHp = view.findViewById(R.id.tv_boss_hp);
        pbBossHp = view.findViewById(R.id.pb_boss_hp);
        btnPrepareForBattle = view.findViewById(R.id.btn_prepare_battle);
        ivBossSprite = view.findViewById(R.id.iv_boss_sprite);
        tvBossHpLabel = view.findViewById(R.id.tv_boss_hp_label);
        pbBossHpBattle = view.findViewById(R.id.pb_boss_hp_battle);
        tvPlayerPpLabel = view.findViewById(R.id.tv_player_pp_label);
        pbPlayerPpBattle = view.findViewById(R.id.pb_player_pp_battle);
        tvAttacksRemaining = view.findViewById(R.id.tv_attacks_remaining);
        tvSuccessChance = view.findViewById(R.id.tv_success_chance);
        btnAttack = view.findViewById(R.id.btn_attack);
        tvBattleMessage = view.findViewById(R.id.tv_battle_message);
        llActiveEquipment = view.findViewById(R.id.ll_active_equipment);
        tvNoEquipment = view.findViewById(R.id.tv_no_equipment);
    }

    private void setupViewModel() {
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);
        setupAnimation();
    }

    private void setupAnimation() {
        if (getContext() != null) {
            animationManager = new BossAnimationManager(getContext(), ivBossSprite);
        }
    }

    private void updateSuccessChance() {
        Integer totalPp = bossViewModel.getTotalPp().getValue();
        Integer bossLevel = bossViewModel.getUserLevel().getValue();

        if (totalPp != null && bossLevel != null) {
            double baseChance = (double) totalPp / (bossLevel * 50 + 100);
            int percentage = (int) (Math.min(0.95, Math.max(0.05, baseChance)) * 100);
            tvSuccessChance.setText("Šansa: " + percentage + "%");
        } else {
            tvSuccessChance.setText("Šansa: --%");
        }
    }

    private void setupObservers() {
        bossViewModel.getUserBasePp().observe(getViewLifecycleOwner(), basePp -> {
            if (basePp != null) {
                tvBasePp.setText("Osnovna PP: " + basePp);
                Log.d(TAG, "Base PP updated: " + basePp);
            }
        });

        bossViewModel.getEquipmentPpBonus().observe(getViewLifecycleOwner(), bonus -> {
            if (bonus != null) {
                if (bonus > 0) {
                    tvEquipmentBonus.setText("Bonus iz opreme: +" + bonus + " PP");
                    tvEquipmentBonus.setVisibility(View.VISIBLE);
                } else {
                    tvEquipmentBonus.setText("Nema bonus iz opreme");
                    tvEquipmentBonus.setVisibility(View.VISIBLE);
                }
                Log.d(TAG, "Equipment bonus updated: " + bonus);
            }
        });

        bossViewModel.getTotalPp().observe(getViewLifecycleOwner(), totalPp -> {
            if (totalPp != null) {
                tvTotalPp.setText("Ukupna PP: " + totalPp);
                int progress = Math.min(100, Math.max(0, (totalPp * 100) / 200));
                pbTotalPp.setProgress(progress);

                Log.d(TAG, "Total PP updated: " + totalPp);
                updateSuccessChance();
            }
        });

        bossViewModel.getUserLevel().observe(getViewLifecycleOwner(), level -> {
            if (level != null) {
                tvBossLevel.setText("Boss Nivo: " + level);
            }
        });

        bossViewModel.getBossCurrentHp().observe(getViewLifecycleOwner(), currentHp -> {
            if (currentHp != null && bossViewModel.getBossMaxHp().getValue() != null) {
                int maxHp = bossViewModel.getBossMaxHp().getValue();
                tvBossHp.setText("Boss HP: " + currentHp + "/" + maxHp);

                int progress = (int) ((currentHp * 100.0) / maxHp);
                pbBossHp.setProgress(progress);
            }
        });

        bossViewModel.getBossCurrentHp().observe(getViewLifecycleOwner(), currentHp -> {
            if (currentHp != null && bossViewModel.getBossMaxHp().getValue() != null) {
                int maxHp = bossViewModel.getBossMaxHp().getValue();
                tvBossHpLabel.setText("Boss HP: " + currentHp + "/" + maxHp);

                int progress = (int) ((currentHp * 100.0) / maxHp);
                pbBossHpBattle.setProgress(progress);
            }
        });

        bossViewModel.getTotalPp().observe(getViewLifecycleOwner(), totalPp -> {
            if (totalPp != null) {
                tvPlayerPpLabel.setText("Tvoja PP: " + totalPp);
                pbPlayerPpBattle.setProgress(Math.min(100, totalPp));
            }
        });

        bossViewModel.getActiveEquipment().observe(getViewLifecycleOwner(), equipmentList -> {
            Log.d(TAG, "Received equipment list with " + (equipmentList != null ? equipmentList.size() : "null") + " items");
            updateActiveEquipmentDisplay(equipmentList);
        });
    }

    private void setupClickListeners() {
        btnPrepareForBattle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.navigateToEquipment();
            }
        });

        btnAttack.setOnClickListener(v -> {
            if (attacksRemaining <= 0) {
                tvBattleMessage.setText("Nemaš više napada za danas!");
                btnAttack.setAlpha(0.5f);
                return;
            }

            if (animationManager != null) {
                animationManager.playHitAnimation();
                animationManager.playShakeEffect();
            }

            Integer currentHp = bossViewModel.getBossCurrentHp().getValue();
            if (currentHp != null && currentHp > 0) {
                int damage = 20; // Test damage
                int newHp = Math.max(0, currentHp - damage);
                bossViewModel.setBossCurrentHp(newHp);

                if (newHp <= 0) {
                    tvBattleMessage.setText("POBEDA! Porazio si bosa!");
                    if (animationManager != null) {
                        animationManager.playDeathAnimation();
                    }
                } else {
                    tvBattleMessage.setText("Pogodak! Naneo si " + damage + " štete!");
                }
            } else {
                tvBattleMessage.setText("Boss je već poražen!");
            }
            attacksRemaining--;
            tvAttacksRemaining.setText("Napadi: " + attacksRemaining + " od " + maxAttacks);

            if (attacksRemaining <= 0) {
                btnAttack.setEnabled(false);
            }
        });
    }

    private void updateActiveEquipmentDisplay(List<Equipment> activeEquipment) {
        if (activeEquipment != null && !activeEquipment.isEmpty()) {
            tvNoEquipment.setVisibility(View.GONE);
            llActiveEquipment.removeAllViews();

            // Jedan TextView sa svim imenima
            StringBuilder equipNames = new StringBuilder();
            for (int i = 0; i < activeEquipment.size(); i++) {
                equipNames.append(activeEquipment.get(i).getName());
                if (i < activeEquipment.size() - 1) {
                    equipNames.append(", ");
                }
            }

            TextView allEquipment = new TextView(getContext());
            allEquipment.setText(equipNames.toString());
            allEquipment.setTextSize(12);
            allEquipment.setTextColor(getResources().getColor(R.color.text_secondary));
            llActiveEquipment.addView(allEquipment);

        } else {
            tvNoEquipment.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (animationManager != null) {
            animationManager.cleanup();
        }
    }
}