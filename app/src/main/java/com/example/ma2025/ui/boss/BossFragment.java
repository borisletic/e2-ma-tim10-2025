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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;
import com.example.ma2025.utils.BossAnimationManager;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.GameLogicUtils;
import com.example.ma2025.viewmodels.BossViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BossFragment extends Fragment {

    private static final String TAG = "BossFragment";

    // Boss battle UI components
    private ImageView ivBossSprite;
    private TextView tvBossTitle, tvBossLevel, tvBossHp;
    private ProgressBar pbBossHp;

    // Player stats UI components
    private TextView tvPlayerTitle, tvPlayerPp, tvBasePp, tvEquipmentBonus;
    private ProgressBar pbPlayerPp;

    // Equipment display
    private LinearLayout llActiveEquipment;
    private TextView tvEquipmentTitle, tvNoEquipment;

    // Battle controls
    private Button btnPrepareForBattle, btnAttack;
    private TextView tvAttacksRemaining, tvBattleMessage, tvAttackSuccessRate;

    // Animation and state
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
        setupInitialBattleState();
    }

    private void initViews(View view) {
        // Boss section
        ivBossSprite = view.findViewById(R.id.iv_boss_sprite);
        tvBossTitle = view.findViewById(R.id.tv_boss_title);
        tvBossLevel = view.findViewById(R.id.tv_boss_level);
        tvBossHp = view.findViewById(R.id.tv_boss_hp);
        pbBossHp = view.findViewById(R.id.pb_boss_hp);

        // Player section
        tvPlayerTitle = view.findViewById(R.id.tv_player_title);
        tvPlayerPp = view.findViewById(R.id.tv_player_pp);
        tvBasePp = view.findViewById(R.id.tv_base_pp);
        tvEquipmentBonus = view.findViewById(R.id.tv_equipment_bonus);
        pbPlayerPp = view.findViewById(R.id.pb_player_pp);

        // Equipment section
        tvEquipmentTitle = view.findViewById(R.id.tv_equipment_title);
        llActiveEquipment = view.findViewById(R.id.ll_active_equipment);
        tvNoEquipment = view.findViewById(R.id.tv_no_equipment);

        // Battle controls
        btnPrepareForBattle = view.findViewById(R.id.btn_prepare_battle);
        btnAttack = view.findViewById(R.id.btn_attack);
        tvAttacksRemaining = view.findViewById(R.id.tv_attacks_remaining);
        tvBattleMessage = view.findViewById(R.id.tv_battle_message);
        tvAttackSuccessRate = view.findViewById(R.id.tv_attack_success_rate);
    }

    private void setupViewModel() {
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);
        setupAnimation();
    }

    private void setupAnimation() {
        if (getContext() != null && ivBossSprite != null) {
            animationManager = new BossAnimationManager(getContext(), ivBossSprite);
        }
    }

    private void setupInitialBattleState() {
        tvBattleMessage.setText("Spreman za borbu!");
        tvAttacksRemaining.setText("Napadi: " + attacksRemaining + " od " + maxAttacks);

        tvBossTitle.setText("BOSS");
        tvPlayerTitle.setText("TVOJA SNAGA");
        tvEquipmentTitle.setText("AKTIVNA OPREMA");

        updateAttackSuccessRate();
    }

    private void setupObservers() {
        bossViewModel.getUserLevel().observe(getViewLifecycleOwner(), level -> {
            if (level != null) {
                updateBossDisplay(level);
            }
        });

        bossViewModel.getBossCurrentHp().observe(getViewLifecycleOwner(), currentHp -> {
            if (currentHp != null && bossViewModel.getBossMaxHp().getValue() != null) {
                updateBossHpDisplay(currentHp, bossViewModel.getBossMaxHp().getValue());
            }
        });

        bossViewModel.getUserBasePp().observe(getViewLifecycleOwner(), basePp -> {
            if (basePp != null) {
                tvBasePp.setText("Osnovna PP: " + basePp);
                updateTotalPpDisplay();
            }
        });

        bossViewModel.getEquipmentPpBonus().observe(getViewLifecycleOwner(), bonus -> {
            if (bonus != null) {
                updateEquipmentBonusDisplay(bonus);
                updateTotalPpDisplay();
            }
        });

        bossViewModel.getTotalPp().observe(getViewLifecycleOwner(), totalPp -> {
            if (totalPp != null) {
                updatePlayerPpDisplay(totalPp);
            }
        });

        bossViewModel.getActiveEquipment().observe(getViewLifecycleOwner(), this::updateEquipmentDisplay);
    }

    private void updateBossDisplay(int userLevel) {
        tvBossLevel.setText("Nivo " + userLevel + " Boss");

        String bossName = getBossName(userLevel);
        tvBossTitle.setText(bossName);
        updateRewardsDisplay(userLevel);


        Log.d(TAG, "Boss display updated for level: " + userLevel);
    }

    private String getBossName(int level) {
        switch (level) {
            case 0:
            case 1: return "POČETNI ČUVAR";
            case 2: return "KAMENI GOLEM";
            case 3: return "VATRENI DEMON";
            case 4: return "LEDENI ZMAJ";
            case 5: return "MRAČNI VLADAR";
            default: return "LEGENDERNI BOSS (" + level + ")";
        }
    }

    private void updateBossHpDisplay(int currentHp, int maxHp) {
        tvBossHp.setText(currentHp + " / " + maxHp + " HP");

        int progress = maxHp > 0 ? (int) ((currentHp * 100.0) / maxHp) : 0;
        pbBossHp.setProgress(progress);

    }

    private void updateRewardsDisplay(int userLevel) {
        TextView tvCoinsReward = requireView().findViewById(R.id.tv_coins_reward);
        TextView tvEquipmentChance = requireView().findViewById(R.id.tv_equipment_chance);

        int coinsReward = calculateCoinsReward(userLevel);
        tvCoinsReward.setText(String.valueOf(coinsReward));

        tvEquipmentChance.setText("20% šanse");
    }

    private int calculateCoinsReward(int level) {
        if (level <= 1) {
            return 200;
        }

        int previousReward = calculateCoinsReward(level - 1);
        return (int) (previousReward * 1.2);
    }

    private void updateEquipmentBonusDisplay(int bonus) {
        if (bonus > 0) {
            tvEquipmentBonus.setText("+ " + bonus + " PP (oprema)");
            tvEquipmentBonus.setVisibility(View.VISIBLE);
        } else {
            tvEquipmentBonus.setText("Nema bonus");
            tvEquipmentBonus.setVisibility(View.VISIBLE);
            tvEquipmentBonus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void updateTotalPpDisplay() {
        Integer basePp = bossViewModel.getUserBasePp().getValue();
        Integer bonusPp = bossViewModel.getEquipmentPpBonus().getValue();

        if (basePp != null && bonusPp != null) {
            int total = basePp + bonusPp;
            tvPlayerPp.setText("UKUPNO: " + total + " PP");
        }
    }

    private void updatePlayerPpDisplay(int totalPp) {
        int progress = Math.min(100, (totalPp * 100) / 500);
        pbPlayerPp.setProgress(progress);

        Log.d(TAG, "Player PP display updated: " + totalPp);
    }

    private void updateEquipmentDisplay(List<Equipment> equipmentList) {
        llActiveEquipment.removeAllViews();

        if (equipmentList != null && !equipmentList.isEmpty()) {
            tvNoEquipment.setVisibility(View.GONE);

            for (Equipment equipment : equipmentList) {
                View equipmentView = createEquipmentView(equipment);
                llActiveEquipment.addView(equipmentView);
            }

        } else {
            tvNoEquipment.setVisibility(View.VISIBLE);
        }
    }

    private View createEquipmentView(Equipment equipment) {
        View equipmentView = LayoutInflater.from(getContext()).inflate(R.layout.item_boss_equipment, llActiveEquipment, false);

        ImageView icon = equipmentView.findViewById(R.id.iv_equipment_icon);
        TextView name = equipmentView.findViewById(R.id.tv_equipment_name);
        TextView effect = equipmentView.findViewById(R.id.tv_equipment_effect);

        icon.setImageResource(getEquipmentIcon(equipment));
        name.setText(equipment.getName());
        effect.setText(getEquipmentEffectText(equipment));

        return equipmentView;
    }

    private int getEquipmentIcon(Equipment equipment) {
        int iconRes = R.drawable.ic_equipment;

        switch (equipment.getType()) {
            case Constants.EQUIPMENT_TYPE_POTION:
                iconRes = equipment.isPermanent() ?
                        R.drawable.ic_potion_permanent : R.drawable.ic_potion_temporary;
                break;
            case Constants.EQUIPMENT_TYPE_CLOTHING:
                switch (equipment.getEffectType()) {
                    case Constants.EFFECT_PP_BOOST:
                        iconRes = R.drawable.ic_gloves;
                        break;
                    case Constants.EFFECT_ATTACK_BOOST:
                        iconRes = R.drawable.ic_shield;
                        break;
                    case "extra_attack":
                        iconRes = R.drawable.ic_boots;
                        break;
                }
                break;
            case Constants.EQUIPMENT_TYPE_WEAPON:
                iconRes = equipment.getEffectType().equals(Constants.EFFECT_PP_BOOST) ?
                        R.drawable.ic_sword : R.drawable.ic_bow;
                break;
        }

        return iconRes;
    }

    private String getEquipmentEffectText(Equipment equipment) {
        if ("pp_boost".equals(equipment.getEffectType())) {
            int bonus = (int)(equipment.getEffectValue() * 100);
            return "+" + bonus + " PP";
        }
        return equipment.getEffectType();
    }

    private void setupClickListeners() {
        btnPrepareForBattle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.navigateToEquipment();
            }
        });

        btnAttack.setOnClickListener(v -> performAttack());
    }

    private void performAttack() {
        if (attacksRemaining <= 0) {
            tvBattleMessage.setText("Nemaš više napada za danas!");
            btnAttack.setEnabled(false);
            return;
        }

        Integer totalPp = bossViewModel.getTotalPp().getValue();
        Integer currentHp = bossViewModel.getBossCurrentHp().getValue();

        if (totalPp == null || currentHp == null || currentHp <= 0) {
            tvBattleMessage.setText("Boss je već poražen!");
            return;
        }

        int damage = totalPp;

        if (animationManager != null) {
            animationManager.playHitAnimation();
            animationManager.playShakeEffect();
        }

        int newHp = Math.max(0, currentHp - damage);
        bossViewModel.setBossCurrentHp(newHp);

        attacksRemaining--;
        tvAttacksRemaining.setText("Napadi: " + attacksRemaining + " od " + maxAttacks);

        if (newHp <= 0) {
            tvBattleMessage.setText("POBEDA! Porazio si " + getBossName(bossViewModel.getUserLevel().getValue()) + "!");
            btnAttack.setEnabled(false);

            if (animationManager != null) {
                animationManager.playDeathAnimation();
            }
            handleBossDefeat();
        } else {
            tvBattleMessage.setText("Pogodak! Naneo si " + damage + " štete!");
        }

        if (attacksRemaining <= 0) {
            btnAttack.setEnabled(false);
            btnAttack.setAlpha(0.5f);
        }
    }

    private void updateAttackSuccessRate() {
        // Placeholder - treba implementirati logiku na osnovu uspešnosti zadataka
        // Za sada hardkodovano na 68%
        int successRate = 68; // TODO: Izračunati na osnovu rešenih zadataka
        tvAttackSuccessRate.setText("Šansa napada: " + successRate + "%");
    }

    private void handleBossDefeat() {
        Integer userLevel = bossViewModel.getUserLevel().getValue();
        if (userLevel == null) return;

        int coinsReward = calculateCoinsReward(userLevel);

        Equipment rewardEquipment = GameLogicUtils.generateRandomEquipmentReward(userLevel);

        String rewardMessage = "Dobio si " + coinsReward + " coins!";
        if (rewardEquipment != null) {
            String equipmentName = rewardEquipment.getName();
            rewardMessage += " + " + equipmentName + "!";
        }

        tvBattleMessage.setText(rewardMessage);

        updateUserCoins(coinsReward);

        if (rewardEquipment != null) {
            saveEquipmentReward(rewardEquipment);
        }
    }

    private void updateUserCoins(int coinsToAdd) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            int currentCoins = user.getCoins();
                            int newCoins = currentCoins + coinsToAdd;

                            db.collection(Constants.COLLECTION_USERS)
                                    .document(userId)
                                    .update("coins", newCoins)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User coins updated: " + currentCoins + " -> " + newCoins);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating coins", e);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user for coins update", e);
                });
    }

    private void saveEquipmentReward(Equipment equipment) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null || equipment == null) return;

        equipment.setUserId(userId);
        equipment.setId(null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.COLLECTION_EQUIPMENT)
                .add(equipment)
                .addOnSuccessListener(documentReference -> {
                    equipment.setId(documentReference.getId());
                    Log.d(TAG, "Reward equipment saved: " + equipment.getName());

                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Bonus nagrada! Dobili ste: " + equipment.getName(),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving reward equipment", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (animationManager != null) {
            animationManager.cleanup();
        }
    }
}