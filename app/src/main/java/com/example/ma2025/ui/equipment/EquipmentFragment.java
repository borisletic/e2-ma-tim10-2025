package com.example.ma2025.ui.equipment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.data.models.User;
import com.example.ma2025.data.preferences.PreferencesManager;
import com.example.ma2025.databinding.FragmentEquipmentBinding;
import com.example.ma2025.ui.equipment.adapter.EquipmentAdapter;
import com.example.ma2025.ui.equipment.adapter.ShopAdapter;
import com.example.ma2025.utils.Constants;
import com.example.ma2025.utils.GameLogicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import com.example.ma2025.data.repositories.AllianceRepository;
import com.example.ma2025.data.repositories.SpecialMissionRepository;
import com.example.ma2025.data.models.Alliance;


public class EquipmentFragment extends Fragment implements
        EquipmentAdapter.OnEquipmentActionListener,
        ShopAdapter.OnPurchaseListener {

    private static final String TAG = "EquipmentFragment";
    private FragmentEquipmentBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferencesManager preferencesManager;
    private User currentUser;
    private EquipmentAdapter equipmentAdapter;
    private ShopAdapter shopAdapter;
    private List<Equipment> userEquipment;
    private List<Equipment> shopItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentEquipmentBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            Log.e(TAG, "Error creating view", e);
            return inflater.inflate(R.layout.fragment_equipment, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initializeFirebase();
            setupUI();
            loadUserData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated", e);
            Toast.makeText(getContext(), "Greška pri inicijalizaciji", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferencesManager = new PreferencesManager(requireContext());

            // Initialize with default user if needed
            currentUser = new User();
            currentUser.setCoins(0);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            throw e;
        }
    }

    private void setupUI() {
        try {
            if (binding == null) {
                Log.e(TAG, "Binding is null in setupUI");
                return;
            }

            // Initialize lists first
            userEquipment = new ArrayList<>();
            shopItems = new ArrayList<>();

            // Setup click listeners with null checks
            if (binding.btnMyEquipment != null) {
                binding.btnMyEquipment.setOnClickListener(v -> showMyEquipment());
            }
            if (binding.btnShop != null) {
                binding.btnShop.setOnClickListener(v -> showShop());
            }

            // Setup RecyclerViews with null checks
            if (binding.recyclerViewEquipment != null) {
                equipmentAdapter = new EquipmentAdapter(userEquipment, this);
                binding.recyclerViewEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
                binding.recyclerViewEquipment.setAdapter(equipmentAdapter);
            }

            if (binding.recyclerViewShop != null) {
                shopAdapter = new ShopAdapter(shopItems, this);
                binding.recyclerViewShop.setLayoutManager(new GridLayoutManager(getContext(), 2));
                binding.recyclerViewShop.setAdapter(shopAdapter);
            }

            // Default to My Equipment
            showMyEquipment();

            Log.d(TAG, "UI setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
            Toast.makeText(getContext(), "Greška pri postavljanju interfejsa", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMyEquipment() {
        try {
            if (binding == null) return;

            if (binding.btnMyEquipment != null) {
                binding.btnMyEquipment.setSelected(true);
            }
            if (binding.btnShop != null) {
                binding.btnShop.setSelected(false);
            }
            if (binding.recyclerViewEquipment != null) {
                binding.recyclerViewEquipment.setVisibility(View.VISIBLE);
            }
            if (binding.recyclerViewShop != null) {
                binding.recyclerViewShop.setVisibility(View.GONE);
            }
            if (binding.tvNoEquipment != null) {
                binding.tvNoEquipment.setVisibility(
                        (userEquipment == null || userEquipment.isEmpty()) ? View.VISIBLE : View.GONE
                );
            }

            loadUserEquipment();

        } catch (Exception e) {
            Log.e(TAG, "Error showing my equipment", e);
        }
    }

    private void showShop() {
        try {
            if (binding == null) return;

            if (binding.btnMyEquipment != null) {
                binding.btnMyEquipment.setSelected(false);
            }
            if (binding.btnShop != null) {
                binding.btnShop.setSelected(true);
            }
            if (binding.recyclerViewEquipment != null) {
                binding.recyclerViewEquipment.setVisibility(View.GONE);
            }
            if (binding.recyclerViewShop != null) {
                binding.recyclerViewShop.setVisibility(View.VISIBLE);
            }
            if (binding.tvNoEquipment != null) {
                binding.tvNoEquipment.setVisibility(View.GONE);
            }

            generateShopItems();

        } catch (Exception e) {
            Log.e(TAG, "Error showing shop", e);
        }
    }

    private void loadUserData() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Log.w(TAG, "User not authenticated");
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            db.collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        try {
                            if (document.exists()) {
                                currentUser = document.toObject(User.class);
                                if (currentUser == null) {
                                    currentUser = new User();
                                    currentUser.setCoins(500);
                                }
                            } else {
                                createDefaultUser(userId);
                            }
                            updateCoinsDisplay();
                            loadUserEquipment();
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user data", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user data", e);
                        createDefaultUser(userId);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loadUserData", e);
        }
    }

    private void createDefaultUser(String userId) {
        currentUser = new User();
        currentUser.setUid(userId);
        currentUser.setCoins(5000); // Povećajte sa 500 na 5000 za testiranje
        updateCoinsDisplay();
    }

    private void updateCoinsDisplay() {
        try {
            if (currentUser != null && binding != null && binding.tvCoins != null) {
                binding.tvCoins.setText(currentUser.getCoins() + " novčića");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating coins display", e);
        }
    }

    private void loadUserEquipment() {
        try {
            if (mAuth.getCurrentUser() == null) {
                Log.w(TAG, "User not authenticated for equipment loading");
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            if (userEquipment == null) {
                                userEquipment = new ArrayList<>();
                            }
                            userEquipment.clear();

                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Equipment equipment = document.toObject(Equipment.class);
                                equipment.setId(document.getId());
                                userEquipment.add(equipment);
                            }

                            if (equipmentAdapter != null) {
                                equipmentAdapter.notifyDataSetChanged();
                            }

                            if (binding != null && binding.tvNoEquipment != null) {
                                binding.tvNoEquipment.setVisibility(
                                        userEquipment.isEmpty() ? View.VISIBLE : View.GONE
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing equipment data", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user equipment", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loadUserEquipment", e);
        }
    }

    private void generateShopItems() {
        try {
            if (shopItems == null) {
                shopItems = new ArrayList<>();
            }
            shopItems.clear();

            if (currentUser == null) {
                currentUser = new User();
                currentUser.setLevel(1);
            }

            int bossReward = GameLogicUtils.calculateBossReward(currentUser.getLevel());

            // POTIONS - Dodajte više napitaka
            Equipment potion1 = Equipment.createPotion("Napitak Snage I", 0.20, 50, false);
            potion1.setPrice(Equipment.calculatePrice(bossReward, 50));
            shopItems.add(potion1);

            Equipment potion2 = Equipment.createPotion("Napitak Snage II", 0.40, 70, false);
            potion2.setPrice(Equipment.calculatePrice(bossReward, 70));
            shopItems.add(potion2);

            Equipment potion3 = Equipment.createPotion("Eliksir Snage I", 0.05, 200, true);
            potion3.setPrice(Equipment.calculatePrice(bossReward, 200));
            shopItems.add(potion3);

            Equipment potion4 = Equipment.createPotion("Eliksir Snage II", 0.10, 1000, true);
            potion4.setPrice(Equipment.calculatePrice(bossReward, 1000));
            shopItems.add(potion4);

            // CLOTHING - Dodajte više odeće
            Equipment gloves = Equipment.createClothing("Rukavice Snage", Constants.EFFECT_PP_BOOST, 0.10, 60);
            gloves.setPrice(Equipment.calculatePrice(bossReward, 60));
            shopItems.add(gloves);

            Equipment shield = Equipment.createClothing("Štit Preciznosti", Constants.EFFECT_ATTACK_BOOST, 0.10, 60);
            shield.setPrice(Equipment.calculatePrice(bossReward, 60));
            shopItems.add(shield);

            Equipment boots = Equipment.createClothing("Čizme Brzine", "extra_attack", 0.40, 80);
            boots.setPrice(Equipment.calculatePrice(bossReward, 80));
            shopItems.add(boots);

            // Dodatna odeća
            Equipment armor = Equipment.createClothing("Oklop Zaštite", Constants.EFFECT_PP_BOOST, 0.15, 100);
            armor.setPrice(Equipment.calculatePrice(bossReward, 100));
            shopItems.add(armor);

            if (shopAdapter != null) {
                shopAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating shop items", e);
        }
    }

    private void updateSpecialMissionForPurchase(String userId) {
        AllianceRepository allianceRepo = new AllianceRepository();
        allianceRepo.getUserAlliance(userId, new AllianceRepository.OnAllianceLoadedListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                SpecialMissionRepository.getInstance().getActiveMission(alliance.getId())
                        .observe(getViewLifecycleOwner(), mission -> { // ← Zameniti observeForever sa observe
                            if (mission != null) {
                                SpecialMissionRepository.getInstance().updateMissionProgress(
                                        mission.getId(), userId, "store_visit",
                                        new SpecialMissionRepository.OnProgressUpdatedCallback() {
                                            @Override
                                            public void onSuccess(int damageDealt, int remainingBossHp) {
                                                if (damageDealt > 0) {
                                                    Log.d(TAG, "Special mission updated: " + damageDealt + " damage dealt");
                                                    Toast.makeText(getContext(),
                                                            "Kupovina je nanela " + damageDealt + " štete bosu specijalne misije!",
                                                            Toast.LENGTH_SHORT).show();
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
                Log.d(TAG, "No alliance found for purchase update: " + error);
            }

            @Override
            public void onNotInAlliance() {
                Log.d(TAG, "User not in alliance, skipping special mission update for purchase");
            }
        });
    }

    // Interface implementations
    @Override
    public void onActivateEquipment(Equipment equipment) {
        try {
            if (equipment != null && equipment.canActivate()) {
                equipment.activate();
                updateEquipmentInFirebase(equipment);
                if (equipmentAdapter != null) {
                    equipmentAdapter.notifyDataSetChanged();
                }
                Toast.makeText(getContext(), equipment.getName() + " aktivirano!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Oprema je već aktivna ili potrošena", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error activating equipment", e);
        }
    }

    @Override
    public void onDeactivateEquipment(Equipment equipment) {
        try {
            if (equipment != null) {
                equipment.deactivate();
                updateEquipmentInFirebase(equipment);
                if (equipmentAdapter != null) {
                    equipmentAdapter.notifyDataSetChanged();
                }
                Toast.makeText(getContext(), equipment.getName() + " deaktivirano!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deactivating equipment", e);
        }
    }

    @Override
    public void onPurchaseItem(Equipment equipment) {
        try {
            if (currentUser == null || equipment == null) {
                Toast.makeText(getContext(), "Greška pri kupovini", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser.getCoins() >= equipment.getPrice()) {
                // Oduzmi novčiće
                currentUser.setCoins(currentUser.getCoins() - equipment.getPrice());

                // Kreiraj kopiju opreme za korisnika
                Equipment purchasedEquipment = new Equipment(equipment);
                purchasedEquipment.setId(null);

                // Sačuvaj u Firebase
                String userId = mAuth.getCurrentUser().getUid();
                purchasedEquipment.setUserId(userId);

                db.collection(Constants.COLLECTION_EQUIPMENT)
                        .add(purchasedEquipment)
                        .addOnSuccessListener(documentReference -> {
                            purchasedEquipment.setId(documentReference.getId());

                            // Ažuriraj novčiće u Firebase
                            updateUserCoins();

                            // NOVO: Ažuriraj specijalnu misiju
                            updateSpecialMissionForPurchase(userId);

                            Toast.makeText(getContext(),
                                    equipment.getName() + " uspešno kupljeno!",
                                    Toast.LENGTH_SHORT).show();

                            // Refresh ako smo na "Moja Oprema" tabu
                            if (binding.recyclerViewEquipment.getVisibility() == View.VISIBLE) {
                                loadUserEquipment();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error purchasing equipment", e);
                            // Vrati novčiće u slučaju greške
                            currentUser.setCoins(currentUser.getCoins() + equipment.getPrice());
                            updateCoinsDisplay();
                            Toast.makeText(getContext(), "Greška pri kupovini", Toast.LENGTH_SHORT).show();
                        });

                updateCoinsDisplay();

            } else {
                Toast.makeText(getContext(),
                        String.format("Nemate dovoljno novčića! Potrebno: %d, imate: %d",
                                equipment.getPrice(), currentUser.getCoins()),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error purchasing item", e);
        }
    }

    private void updateUserCoins() {
        if (mAuth.getCurrentUser() != null && currentUser != null) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(mAuth.getCurrentUser().getUid())
                    .update("coins", currentUser.getCoins())
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating user coins", e);
                    });
        }
    }

    private void updateEquipmentInFirebase(Equipment equipment) {
        try {
            if (equipment != null && equipment.getId() != null) {
                db.collection(Constants.COLLECTION_EQUIPMENT)
                        .document(equipment.getId())
                        .set(equipment)
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating equipment", e);
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateEquipmentInFirebase", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onUpgradeEquipment(Equipment equipment) {
        try {
            if (equipment == null || !equipment.canUpgrade()) {
                Toast.makeText(getContext(), "Ovu opremu nije moguće unaprediti", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser == null) {
                Toast.makeText(getContext(), "Greška pri učitavanju korisničkih podataka", Toast.LENGTH_SHORT).show();
                return;
            }

            int bossReward = GameLogicUtils.calculateBossReward(currentUser.getLevel());
            int upgradeCost = equipment.getUpgradeCost(bossReward);

            // Show confirmation dialog
            showUpgradeConfirmationDialog(equipment, upgradeCost);

        } catch (Exception e) {
            Log.e(TAG, "Error upgrading equipment", e);
            Toast.makeText(getContext(), "Greška pri unapređivanju opreme", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpgradeConfirmationDialog(Equipment equipment, int upgradeCost) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Unapređenje oružja");
            builder.setMessage(String.format(
                    "Da li želite da unapredite %s?\n\n" +
                            "Cena: %d novčića\n" +
                            "Efekat se povećava za 0.01%%\n" +
                            "Trenutni nivo: %d",
                    equipment.getName(),
                    upgradeCost,
                    equipment.getUpgradeLevel()
            ));

            builder.setPositiveButton("Unapredi", (dialog, which) -> {
                performUpgrade(equipment, upgradeCost);
            });

            builder.setNegativeButton("Otkaži", (dialog, which) -> {
                dialog.dismiss();
            });

            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing upgrade dialog", e);
        }
    }

    private void performUpgrade(Equipment equipment, int upgradeCost) {
        try {
            if (currentUser.getCoins() >= upgradeCost) {
                // Deduct coins
                currentUser.setCoins(currentUser.getCoins() - upgradeCost);

                // Upgrade equipment
                equipment.upgrade();

                // Update in Firebase
                updateEquipmentInFirebase(equipment);
                updateUserCoins();

                // Update UI
                if (equipmentAdapter != null) {
                    equipmentAdapter.notifyDataSetChanged();
                }
                updateCoinsDisplay();

                Toast.makeText(getContext(),
                        String.format("%s uspešno unapređeno na nivo %d!",
                                equipment.getName(), equipment.getUpgradeLevel()),
                        Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getContext(),
                        String.format("Nemate dovoljno novčića! Potrebno: %d, Imate: %d",
                                upgradeCost, currentUser.getCoins()),
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error performing upgrade", e);
            Toast.makeText(getContext(), "Greška pri unapređivanju", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveRewardEquipment(Equipment equipment) {
        try {
            if (mAuth.getCurrentUser() == null || equipment == null) {
                Log.e(TAG, "Cannot save reward equipment - user not authenticated or equipment is null");
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();
            equipment.setUserId(userId);
            equipment.setId(null); // Ensure new document

            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .add(equipment)
                    .addOnSuccessListener(documentReference -> {
                        equipment.setId(documentReference.getId());
                        Log.d(TAG, "Reward equipment saved successfully: " + equipment.getName());

                        // Refresh equipment list if currently viewing
                        if (binding != null && binding.recyclerViewEquipment.getVisibility() == View.VISIBLE) {
                            loadUserEquipment();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving reward equipment", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Exception in saveRewardEquipment", e);
        }
    }
}