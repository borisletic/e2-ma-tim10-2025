package com.example.ma2025.ui.boss;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ma2025.MainActivity;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Alliance;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.repositories.AllianceRepository;
import com.example.ma2025.data.repositories.SpecialMissionRepository;
import com.example.ma2025.utils.BossAnimationManager;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.GameLogicUtils;
import com.example.ma2025.utils.ShakeDetector;
import com.example.ma2025.utils.TreasureChestAnimator;
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
    private TextView tvPlayerTitle, tvPlayerPp;
    private ProgressBar pbPlayerPp;

    // Equipment display
    private LinearLayout llActiveEquipment;
    private TextView tvEquipmentTitle, tvNoEquipment;

    // Battle controls
    private Button btnPrepareForBattle, btnAttack;
    private TextView tvAttacksRemaining, tvBattleMessage, tvAttackSuccessRate;

    // Treasure chest components
    private ImageView ivTreasureChest;
    private ShakeDetector shakeDetector;
    private TreasureChestAnimator chestAnimator;
    private boolean isChestReadyToOpen = false;
    private boolean isWaitingForShake = false;
    private int pendingCoinsReward = 0;
    private Equipment pendingEquipmentReward = null;

    // Animation and state
    private BossAnimationManager animationManager;
    private RelativeLayout rlTreasureOverlay;
    private ImageView ivTreasureChestFullscreen;

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
        Log.d(TAG, "=== onViewCreated finished, calling calculateAttackSuccessRate ===");
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

        // Treasure chest
        ivTreasureChest = view.findViewById(R.id.iv_treasure_chest);

        rlTreasureOverlay = view.findViewById(R.id.rl_treasure_overlay);
        ivTreasureChestFullscreen = view.findViewById(R.id.iv_treasure_chest_fullscreen);
    }

    private void setupViewModel() {
        bossViewModel = new ViewModelProvider(this).get(BossViewModel.class);
        setupAnimation();
        setupShakeDetector();
        setupChestAnimator();
    }

    private void setupAnimation() {
        if (getContext() != null && ivBossSprite != null) {
            animationManager = new BossAnimationManager(getContext(), ivBossSprite);
        }
    }

    private void setupShakeDetector() {
        if (getContext() != null) {
            shakeDetector = new ShakeDetector(getContext());
            shakeDetector.setOnShakeListener(shakeCount -> {
                if (isWaitingForShake && isChestReadyToOpen) {
                    openTreasureChest();
                }
            });
        }
    }

    private void setupChestAnimator() {
        if (getContext() != null && ivTreasureChestFullscreen != null) {
            chestAnimator = new TreasureChestAnimator(getContext(), ivTreasureChestFullscreen);
        }
    }

    private void setupInitialBattleState() {
        tvBattleMessage.setText("Spreman za borbu!");
        tvBossTitle.setText("BOSS");
        tvPlayerTitle.setText("TVOJA SNAGA");
        tvEquipmentTitle.setText("AKTIVNA OPREMA");
        bossViewModel.calculateAttackSuccessRate();
    }

    private void setupObservers() {
        bossViewModel.getUserLevel().observe(getViewLifecycleOwner(), level -> {
            if (level != null) {
                // Prikaži korisnikov nivo u UI
                tvPlayerTitle.setText("TVOJA SNAGA (Nivo " + level + ")");

                if (level <= 0) {
                    btnAttack.setVisibility(View.GONE);
                    ivBossSprite.setVisibility(View.GONE);
                    tvBossLevel.setVisibility(View.GONE);
                    tvBossTitle.setVisibility(View.GONE);
                    tvBossHp.setVisibility(View.GONE);
                    pbBossHp.setVisibility(View.GONE);
                    tvBattleMessage.setText("Rešavajte zadatke da dostignete nivo 1 i otključate borbu sa prvim bosom!");

                } else {
                    // Prikaži boss UI
                    btnAttack.setVisibility(View.VISIBLE);
                    ivBossSprite.setVisibility(View.VISIBLE);
                    tvBossLevel.setVisibility(View.VISIBLE);
                    tvBossTitle.setVisibility(View.VISIBLE);
                    tvBossHp.setVisibility(View.VISIBLE);
                    pbBossHp.setVisibility(View.VISIBLE);

                    updateBossDisplay(level);
                }
            }
        });

        bossViewModel.getBossCurrentHp().observe(getViewLifecycleOwner(), currentHp -> {
            Log.d(TAG, "Observer: Boss HP changed to: " + currentHp);
            if (currentHp != null && bossViewModel.getBossMaxHp().getValue() != null) {
                updateBossHpDisplay(currentHp, bossViewModel.getBossMaxHp().getValue());
            }
        });

        bossViewModel.getUserBasePp().observe(getViewLifecycleOwner(), basePp -> {
            if (basePp != null) {
                updateTotalPpDisplay();
            }
        });

        bossViewModel.getTotalPp().observe(getViewLifecycleOwner(), totalPp -> {
            if (totalPp != null) {
                updatePlayerPpDisplay(totalPp);
            }
        });

        bossViewModel.getAttacksRemaining().observe(getViewLifecycleOwner(), attacks -> {
            if (attacks != null) {
                tvAttacksRemaining.setText("Napadi: " + attacks + " od 5");

                if (attacks <= 0) {
                    btnAttack.setEnabled(false);
                    btnAttack.setAlpha(0.5f);

                    handleBattleEnd();
                } else {
                    btnAttack.setEnabled(true);
                    btnAttack.setAlpha(1.0f);
                }
            }
        });

        bossViewModel.getCurrentBossLevel().observe(getViewLifecycleOwner(), level -> {
            if (level != null) {
                updateBossDisplay(level);
            }
        });

        bossViewModel.getUndefeatedBossLevels().observe(getViewLifecycleOwner(), undefeatedList -> {
            Integer currentIndex = bossViewModel.getCurrentBossIndex().getValue();
            if (undefeatedList != null && !undefeatedList.isEmpty() && currentIndex != null) {
                tvBattleMessage.setText("Neporaženi bosevi: " + (currentIndex + 1) + "/" + undefeatedList.size());
            }
        });

        bossViewModel.getAttackSuccessRate().observe(getViewLifecycleOwner(), successRate -> {
            if (successRate != null) {
                tvAttackSuccessRate.setText("Šansa napada: " + successRate + "%");
            }
        });

        bossViewModel.getActiveEquipment().observe(getViewLifecycleOwner(), equipment -> {
            updateEquipmentDisplay(equipment);
        });
    }

    private void updateBossDisplay(int currentBossLevel) {
        Integer userLevel = bossViewModel.getUserLevel().getValue();

        if (userLevel == null || userLevel < currentBossLevel) {
            tvBossLevel.setText("Zakljucan");
            tvBossTitle.setText("???");
            ivBossSprite.setAlpha(0.3f);
        } else {
            tvBossLevel.setText("Nivo " + currentBossLevel + " Boss");
            String bossName = getBossName(currentBossLevel);
            tvBossTitle.setText(bossName);
            ivBossSprite.setAlpha(1.0f);
            updateRewardsDisplay(currentBossLevel);
        }
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

    private void updateTotalPpDisplay() {
        Integer basePp = bossViewModel.getUserBasePp().getValue();
        Integer bonusPp = bossViewModel.getEquipmentPpBonus().getValue();

        if (basePp != null && bonusPp != null) {
            int total = basePp + bonusPp;
        }
    }

    private void updatePlayerPpDisplay(int totalPp) {
        tvPlayerPp.setText("Ukupno:" + totalPp + " PP");
        int progress = Math.min(100, (totalPp * 100) / 500);
        pbPlayerPp.setProgress(progress);
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

    private void handleBattleEnd() {
        Integer userLevel = bossViewModel.getUserLevel().getValue();
        Integer currentHp = bossViewModel.getBossCurrentHp().getValue();
        Integer maxHp = bossViewModel.getBossMaxHp().getValue();

        if (userLevel == null || currentHp == null || maxHp == null) return;

        if (currentHp <= 0) {
            handleBossDefeat();
        } else {
            double hpPercentage = (double) currentHp / maxHp;
            if (hpPercentage <= 0.5) {
                handlePartialVictory(userLevel, hpPercentage);
            } else {
                tvBattleMessage.setText("Boss nije dovoljno oslabljen! Potrebno je umanjiti mu bar 50% HP-a za nagradu.");
            }
        }
    }

    private void handlePartialVictory(int userLevel, double remainingHpPercentage) {
        int fullCoinsReward = calculateCoinsReward(userLevel);
        int partialCoinsReward = fullCoinsReward / 2;
        double partialEquipmentChance = 0.10; // 10%

        Equipment rewardEquipment = GameLogicUtils.generateRandomEquipmentReward(userLevel, partialEquipmentChance);

        updateUserCoins(partialCoinsReward);
        if (rewardEquipment != null) {
            saveEquipmentReward(rewardEquipment);
        }

        showTreasureChest(partialCoinsReward, rewardEquipment);

        Log.d(TAG, String.format("Partial victory: %d coins (was %d), %.1f%% equipment chance",
                partialCoinsReward, fullCoinsReward, partialEquipmentChance * 100));
    }

    private void updateSpecialMissionForSuccessfulAttack() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) return;

        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.getUserAlliance(userId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                SpecialMissionRepository.getInstance().getActiveMission(alliance.getId())
                        .observe(getViewLifecycleOwner(), mission -> {
                            if (mission != null) {
                                SpecialMissionRepository.getInstance().updateMissionProgress(
                                        mission.getId(), userId, "successful_attack",
                                        new SpecialMissionRepository.OnProgressUpdatedCallback() {
                                            @Override
                                            public void onSuccess(int damageDealt, int remainingBossHp) {
                                                if (damageDealt > 0) {
                                                    Log.d(TAG, "Special mission updated: " + damageDealt + " damage dealt from successful attack");

                                                    // Dodaj vizuelni feedback
                                                    if (getContext() != null) {
                                                        Toast.makeText(getContext(),
                                                                "Uspešan napad je naneo " + damageDealt + " štete specijalnom bosu!",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.e(TAG, "Special mission update failed: " + error);
                                            }
                                        }
                                );
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "No alliance found for successful attack update: " + error);
            }

            @Override
            public void onNotInAlliance() {
                Log.d(TAG, "User not in alliance, skipping special mission update for successful attack");
            }
        });
    }

    private void performAttack() {
        Integer totalPp = bossViewModel.getTotalPp().getValue();
        Integer currentHp = bossViewModel.getBossCurrentHp().getValue();
        Integer successRate = bossViewModel.getAttackSuccessRate().getValue();

        if (totalPp == null || currentHp == null || currentHp <= 0) {
            tvBattleMessage.setText("Boss je već poražen!");
            return;
        }

        // Proveri da li napad uspeva na osnovu uspešnosti zadataka
        boolean attackSucceeds = GameLogicUtils.isAttackSuccessful(successRate != null ? successRate : 50);

        if (!attackSucceeds) {
            tvBattleMessage.setText("Promašaj! Pokušaj ponovo.");

            // Animacija promašaja
            if (animationManager != null) {
                animationManager.playShakeEffect();
            }

            bossViewModel.useAttack();
            return;
        }

        // Uspešan napad
        int damage = totalPp;

        if (animationManager != null) {
            animationManager.playHitAnimation();
            animationManager.playShakeEffect();
        }

        int newHp = Math.max(0, currentHp - damage);
        bossViewModel.updateBossHp(newHp);

        // Ažuriraj specijalnu misiju za uspešan napad
        updateSpecialMissionForSuccessfulAttack();

        if (newHp <= 0) {
            tvBattleMessage.setText("POBEDA! Porazio si " + getBossName(bossViewModel.getUserLevel().getValue()) + "!");
            btnAttack.setEnabled(false);

            if (animationManager != null) {
                animationManager.playDeathAnimation();
            }

            handleBattleEnd();
        } else {
            tvBattleMessage.setText("Pogodak! Naneo si " + damage + " HP štete!");
        }

        bossViewModel.useAttack();
    }

    private void handleBossDefeat() {
        // Sačuvaj nivo poraženog bosa PRE nego što se pomeriš na sledeći
        Integer defeatedLevel = bossViewModel.getCurrentBossLevel().getValue();

        // Označi trenutnog bosa kao poraženog
        bossViewModel.markCurrentBossDefeated();

        // Izračunaj nagrade za poraženog bosa
        int coinsReward = calculateCoinsReward(defeatedLevel != null ? defeatedLevel : 1);
        Equipment rewardEquipment = GameLogicUtils.generateRandomEquipmentReward(
                defeatedLevel != null ? defeatedLevel : 1, 0.20);

        // Dodaj nagrade u bazu
        updateUserCoins(coinsReward);
        if (rewardEquipment != null) {
            saveEquipmentReward(rewardEquipment);
        }

        // Prikaži kovčeg sa nagradama
        showTreasureChest(coinsReward, rewardEquipment);

        // Nakon 3 sekunde, pređi na sledećeg bosa
        tvBattleMessage.setText("Boss poražen! Priprema se sledeći boss...");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            bossViewModel.moveToNextUndefeatedBoss();
            bossViewModel.resetAttacks();
            btnAttack.setEnabled(true);
            btnAttack.setAlpha(1.0f);
            tvBattleMessage.setText("Spreman za borbu protiv novog bosa!");
        }, 3000);
    }

    // ========== TREASURE CHEST LOGIC ==========

    private void showTreasureChest(int coinsReward, Equipment rewardEquipment) {
        pendingCoinsReward = coinsReward;
        pendingEquipmentReward = rewardEquipment;

        rlTreasureOverlay.setVisibility(View.VISIBLE);
        tvBattleMessage.setText("Protresite telefon da otvorite kovčeg sa nagradama!");

        if (chestAnimator != null) {
            chestAnimator.showReadyToOpenAnimation();
        }

        isChestReadyToOpen = true;
        isWaitingForShake = true;

        if (shakeDetector != null) {
            shakeDetector.start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isWaitingForShake && isChestReadyToOpen) {
                openTreasureChest();
            }
        }, 10000);
    }

    private void openTreasureChest() {
        isWaitingForShake = false;

        if (shakeDetector != null) {
            shakeDetector.stop();
        }

        if (chestAnimator != null) {
            chestAnimator.openChest(() -> {
                showRewards();

                // Sakrij fullscreen overlay nakon 3 sekunde
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    rlTreasureOverlay.setVisibility(View.GONE);
                    if (chestAnimator != null) {
                        chestAnimator.resetChest();
                    }
                    isChestReadyToOpen = false;
                }, 3000);
            });
        }
    }

    private void showRewards() {
        String rewardMessage = "Dobili ste " + pendingCoinsReward + " novčića!";
        if (pendingEquipmentReward != null) {
            rewardMessage += "\n+ " + pendingEquipmentReward.getName() + "!";
        }

        tvBattleMessage.setText(rewardMessage);

        // Prikaži Toast sa bonus porukom
        if (getContext() != null) {
            Toast.makeText(getContext(),
                    "Kovčeg otvoren! " + rewardMessage,
                    Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "Chest opened! Rewards: " + pendingCoinsReward + " coins, equipment: " +
                (pendingEquipmentReward != null ? pendingEquipmentReward.getName() : "none"));
    }

    // ========== DATABASE OPERATIONS ==========

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
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
        if (chestAnimator != null) {
            chestAnimator.cleanup();
        }
    }
}