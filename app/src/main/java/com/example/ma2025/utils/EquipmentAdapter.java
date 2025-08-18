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

            tvEquipmentName.setText(equipment.getName());
            tvEquipmentEffect.setText("+" + equipment.getEffect() + "%");

            // Set icon based on equipment type
            int iconRes = getEquipmentIcon(equipment);
            ivEquipmentIcon.setImageResource(iconRes);

            // Show active indicator
            activeIndicator.setVisibility(equipment.isActive() ? View.VISIBLE : View.GONE);

            // Show durability for clothing or level for weapons
            if (equipment.getType() == Equipment.EquipmentType.CLOTHING) {
                tvEquipmentDurability.setVisibility(View.VISIBLE);
                tvEquipmentDurability.setText("Trajanje: " + equipment.getDurability());
            } else if (equipment.getType() == Equipment.EquipmentType.WEAPON) {
                tvEquipmentDurability.setVisibility(View.VISIBLE);
                tvEquipmentDurability.setText("Nivo: " + equipment.getLevel());
            } else {
                tvEquipmentDurability.setVisibility(View.GONE);
            }
        }

        private int getEquipmentIcon(Equipment equipment) {
            // Use generic equipment icon for now - you can add specific icons later
            return R.drawable.ic_equipment;
        }
    }
}