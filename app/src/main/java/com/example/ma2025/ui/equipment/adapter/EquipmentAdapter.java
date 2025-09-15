package com.example.ma2025.ui.equipment.adapter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ma2025.R;
import com.example.ma2025.data.models.Equipment;
import com.example.ma2025.utils.Constants;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;
    private OnEquipmentActionListener listener;

    public interface OnEquipmentActionListener {
        void onActivateEquipment(Equipment equipment);
        void onDeactivateEquipment(Equipment equipment);
        void onUpgradeEquipment(Equipment equipment);
    }

    public EquipmentAdapter(List<Equipment> equipmentList, OnEquipmentActionListener listener) {
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment, listener);
    }

    @Override
    public int getItemCount() {
        return equipmentList != null ? equipmentList.size() : 0;
    }

    static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivEquipmentIcon;
        private TextView tvEquipmentName;
        private TextView tvEquipmentDescription;
        private TextView tvEquipmentType;
        private TextView tvEquipmentStatus;
        private Button btnAction;
        private Button btnUpgrade;
        private View statusIndicator;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipmentIcon = itemView.findViewById(R.id.iv_equipment_icon);
            tvEquipmentName = itemView.findViewById(R.id.tv_equipment_name);
            tvEquipmentDescription = itemView.findViewById(R.id.tv_equipment_description);
            tvEquipmentType = itemView.findViewById(R.id.tv_equipment_type);
            tvEquipmentStatus = itemView.findViewById(R.id.tv_equipment_status);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnUpgrade = itemView.findViewById(R.id.btn_upgrade);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        public void bind(Equipment equipment, OnEquipmentActionListener listener) {
            tvEquipmentName.setText(equipment.getName());
            tvEquipmentDescription.setText(equipment.getDescription());
            tvEquipmentType.setText(equipment.getTypeText());
            tvEquipmentStatus.setText(equipment.getStatusText());

            // Set icon based on equipment type
            setEquipmentIcon(equipment);

            // Set status indicator color
            setStatusIndicator(equipment);

            // Setup action button
            setupActionButton(equipment, listener);

            // Setup upgrade button
            setupUpgradeButton(equipment, listener);
        }

        private void setEquipmentIcon(Equipment equipment) {
            int iconRes = R.drawable.ic_equipment; // Default icon

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

            ivEquipmentIcon.setImageResource(iconRes);
        }

        private void setStatusIndicator(Equipment equipment) {
            if (statusIndicator == null) return;

            int colorRes;
            if (equipment.isActive()) {
                colorRes = R.color.success_color;
            } else if (equipment.isExpired()) {
                colorRes = R.color.error_color;
            } else {
                colorRes = R.color.text_secondary;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                statusIndicator.setBackgroundColor(
                        itemView.getContext().getColor(colorRes)
                );
            }
        }

        private void setupActionButton(Equipment equipment, OnEquipmentActionListener listener) {
            if (equipment.isExpired()) {
                btnAction.setText("Potrošeno");
                btnAction.setEnabled(false);
                btnAction.setOnClickListener(null);
            } else if (equipment.isActive()) {
                btnAction.setText("Deaktiviraj");
                btnAction.setEnabled(true);
                btnAction.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeactivateEquipment(equipment);
                    }
                });
            } else {
                btnAction.setText("Aktiviraj");
                btnAction.setEnabled(equipment.canActivate());
                btnAction.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActivateEquipment(equipment);
                    }
                });
            }
        }

        private void setupUpgradeButton(Equipment equipment, OnEquipmentActionListener listener) {
            if (equipment.canUpgrade()) {
                // Show upgrade button for weapons
                btnUpgrade.setVisibility(View.VISIBLE);
                btnUpgrade.setText("Unapredi");
                btnUpgrade.setEnabled(true);
                btnUpgrade.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUpgradeEquipment(equipment);
                    }
                });
            } else {
                // Hide upgrade button for non-weapons
                btnUpgrade.setVisibility(View.GONE);
            }
        }
    }
}