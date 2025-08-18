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
        binding = FragmentEquipmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        setupUI();
        loadUserData();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferencesManager = new PreferencesManager(requireContext());
    }

    private void setupUI() {
        // Setup tabs
        binding.btnMyEquipment.setOnClickListener(v -> showMyEquipment());
        binding.btnShop.setOnClickListener(v -> showShop());

        // Setup RecyclerViews
        userEquipment = new ArrayList<>();
        shopItems = new ArrayList<>();

        equipmentAdapter = new EquipmentAdapter(userEquipment, this);
        binding.recyclerViewEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEquipment.setAdapter(equipmentAdapter);

        shopAdapter = new ShopAdapter(shopItems, this);
        binding.recyclerViewShop.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerViewShop.setAdapter(shopAdapter);

        // Default to My Equipment
        showMyEquipment();
    }

    private void showMyEquipment() {
        binding.btnMyEquipment.setSelected(true);
        binding.btnShop.setSelected(false);
        binding.recyclerViewEquipment.setVisibility(View.VISIBLE);
        binding.recyclerViewShop.setVisibility(View.GONE);
        binding.tvNoEquipment.setVisibility(userEquipment.isEmpty() ? View.VISIBLE : View.GONE);

        loadUserEquipment();
    }

    private void showShop() {
        binding.btnMyEquipment.setSelected(false);
        binding.btnShop.setSelected(true);
        binding.recyclerViewEquipment.setVisibility(View.GONE);
        binding.recyclerViewShop.setVisibility(View.VISIBLE);
        binding.tvNoEquipment.setVisibility(View.GONE);

        generateShopItems();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentUser = document.toObject(User.class);
                        updateCoinsDisplay();
                        loadUserEquipment();
                    } else {
                        createDefaultUser(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(getContext(), "Greška pri učitavanju korisnika", Toast.LENGTH_SHORT).show();
                });
    }

    private void createDefaultUser(String userId) {
        currentUser = new User();
        currentUser.setUid(userId);
        currentUser.setCoins(500); // Start with some coins for testing
        updateCoinsDisplay();
    }

    private void updateCoinsDisplay() {
        if (currentUser != null && binding != null) {
            binding.tvCoins.setText(currentUser.getCoins() + " novčića");
        }
    }

    private void loadUserEquipment() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection(Constants.COLLECTION_EQUIPMENT)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userEquipment.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Equipment equipment = document.toObject(Equipment.class);
                        equipment.setId(document.getId());
                        userEquipment.add(equipment);
                    }

                    equipmentAdapter.notifyDataSetChanged();
                    binding.tvNoEquipment.setVisibility(userEquipment.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user equipment", e);
                });
    }

    private void generateShopItems() {
        shopItems.clear();

        if (currentUser == null) return;

        // Calculate boss reward for current level for pricing
        int bossReward = GameLogicUtils.calculateBossReward(currentUser.getLevel());

        // POTIONS
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

        // CLOTHING
        Equipment gloves = Equipment.createClothing("Rukavice Snage", Constants.EFFECT_PP_BOOST, 0.10, 60);
        gloves.setPrice(Equipment.calculatePrice(bossReward, 60));
        shopItems.add(gloves);

        Equipment shield = Equipment.createClothing("Štit Preciznosti", Constants.EFFECT_ATTACK_BOOST, 0.10, 60);
        shield.setPrice(Equipment.calculatePrice(bossReward, 60));
        shopItems.add(shield);

        Equipment boots = Equipment.createClothing("Čizme Brzine", "extra_attack", 0.40, 80);
        boots.setPrice(Equipment.calculatePrice(bossReward, 80));
        shopItems.add(boots);

        shopAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivateEquipment(Equipment equipment) {
        if (equipment.canActivate()) {
            equipment.activate();
            updateEquipmentInFirebase(equipment);
            equipmentAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), equipment.getName() + " aktivirano!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Oprema je već aktivna ili potrošena", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeactivateEquipment(Equipment equipment) {
        equipment.deactivate();
        updateEquipmentInFirebase(equipment);
        equipmentAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), equipment.getName() + " deaktivirano!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseItem(Equipment equipment) {
        if (currentUser == null) return;

        if (currentUser.getCoins() >= equipment.getPrice()) {
            // Deduct coins
            currentUser.setCoins(currentUser.getCoins() - equipment.getPrice());

            // Add equipment to user's collection
            Equipment purchasedEquipment = createEquipmentCopy(equipment);
            purchasedEquipment.setId(null); // Will be generated by Firebase

            // Save to Firebase
            String userId = mAuth.getCurrentUser().getUid();
            purchasedEquipment.setUserId(userId);

            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .add(purchasedEquipment)
                    .addOnSuccessListener(documentReference -> {
                        purchasedEquipment.setId(documentReference.getId());

                        // Update user coins in Firebase
                        updateUserCoins();

                        Toast.makeText(getContext(),
                                equipment.getName() + " uspešno kupljeno!",
                                Toast.LENGTH_SHORT).show();

                        // Refresh equipment list if on that tab
                        if (binding.recyclerViewEquipment.getVisibility() == View.VISIBLE) {
                            loadUserEquipment();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error purchasing equipment", e);
                        // Refund coins on error
                        currentUser.setCoins(currentUser.getCoins() + equipment.getPrice());
                        updateCoinsDisplay();
                        Toast.makeText(getContext(), "Greška pri kupovini", Toast.LENGTH_SHORT).show();
                    });

            updateCoinsDisplay();

        } else {
            Toast.makeText(getContext(), "Nemate dovoljno novčića!", Toast.LENGTH_SHORT).show();
        }
    }

    private Equipment createEquipmentCopy(Equipment original) {
        Equipment copy = new Equipment();
        copy.setName(original.getName());
        copy.setDescription(original.getDescription());
        copy.setType(original.getType());
        copy.setSubType(original.getSubType());
        copy.setEffectValue(original.getEffectValue());
        copy.setEffectType(original.getEffectType());
        copy.setPrice(original.getPrice());
        copy.setPermanent(original.isPermanent());
        copy.setIconName(original.getIconName());
        copy.setUsesRemaining(original.getUsesRemaining());
        return copy;
    }

    private void updateEquipmentInFirebase(Equipment equipment) {
        if (equipment.getId() != null) {
            db.collection(Constants.COLLECTION_EQUIPMENT)
                    .document(equipment.getId())
                    .set(equipment)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating equipment", e);
                    });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}