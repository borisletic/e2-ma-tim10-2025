package com.example.ma2025.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private Context context;

    public EquipmentAdapter(Context context, List<Equipment> equipmentList) {
        this.context = context;
        this.equipmentList = equipmentList;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
    }

    @Override
    public int getItemCount() {
        return equipmentList != null ? equipmentList.size() : 0;
    }

    public void updateEquipment(List<Equipment> newEquipmentList) {
        this.equipmentList = newEquipmentList;
        notifyDataSetChanged();
    }

    public static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivEquipmentIcon;
        private TextView tvEquipmentName;
        private TextView tvEquipmentEffect;
        private TextView tvEquipmentDurability;
        private View activeIndicator;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipmentIcon = itemView.findViewById(R.id.iv_equipment_icon);
            tvEquipmentName = itemView.findViewById(R.id.tv_equipment_name);
            tvEquipmentEffect = itemView.findViewById(R.id.tv_equipment_effect);
            tvEquipmentDurability = itemView.findViewById(R.id.tv_equipment_durability);
            activeIndicator = itemView.findViewById(R.id.active_indicator);
        }

        public void bind(Equipment equipment) {
            if (equipment == null) return;

            // Koristi postojeće metode iz novog Equipment modela
            tvEquipmentName.setText(equipment.getName());

            // Prikaži efekat na osnovu effectValue
            tvEquipmentEffect.setText("+" + (int)(equipment.getEffectValue() * 100) + "%");

            // Postavi ikonu na osnovu tipa opreme
            int iconRes = getEquipmentIcon(equipment);
            ivEquipmentIcon.setImageResource(iconRes);

            // Prikaži indikator aktivnosti
            activeIndicator.setVisibility(equipment.isActive() ? View.VISIBLE : View.GONE);

            // Prikaži status na osnovu tipa opreme
            if (equipment.getType() == Constants.EQUIPMENT_TYPE_CLOTHING) {
                tvEquipmentDurability.setVisibility(View.VISIBLE);
                tvEquipmentDurability.setText("Trajanje: " + equipment.getUsesRemaining());
            } else if (equipment.getType() == Constants.EQUIPMENT_TYPE_WEAPON) {
                tvEquipmentDurability.setVisibility(View.VISIBLE);
                tvEquipmentDurability.setText("Trajno");
            } else if (equipment.getType() == Constants.EQUIPMENT_TYPE_POTION) {
                tvEquipmentDurability.setVisibility(View.VISIBLE);
                if (equipment.isPermanent()) {
                    tvEquipmentDurability.setText("Trajno");
                } else {
                    tvEquipmentDurability.setText("Jednokratno");
                }
            } else {
                tvEquipmentDurability.setVisibility(View.GONE);
            }
        }

        private int getEquipmentIcon(Equipment equipment) {
            // Vrati ikonu na osnovu iconName ili tipa opreme
            if (equipment.getIconName() != null) {
                switch (equipment.getIconName()) {
                    case "potion_permanent":
                    case "potion_temporary":
                        return R.drawable.ic_equipment; // Možeš zameniti sa specifičnom ikonom za napitke
                    case "gloves":
                        return R.drawable.ic_equipment; // Možeš zameniti sa ikonom rukavica
                    case "shield":
                        return R.drawable.ic_equipment; // Možeš zameniti sa ikonom štita
                    case "boots":
                        return R.drawable.ic_equipment; // Možeš zameniti sa ikonom čizama
                    case "sword":
                        return R.drawable.ic_equipment; // Možeš zameniti sa ikonom mača
                    case "bow":
                        return R.drawable.ic_equipment; // Možeš zameniti sa ikonom luka
                    default:
                        return R.drawable.ic_equipment;
                }
            }

            // Fallback na osnovu tipa opreme
            switch (equipment.getType()) {
                case Constants.EQUIPMENT_TYPE_POTION:
                    return R.drawable.ic_equipment; // Ikona za napitke
                case Constants.EQUIPMENT_TYPE_CLOTHING:
                    return R.drawable.ic_equipment; // Ikona za odeću
                case Constants.EQUIPMENT_TYPE_WEAPON:
                    return R.drawable.ic_equipment; // Ikona za oružje
                default:
                    return R.drawable.ic_equipment;
            }
        }
    }
}