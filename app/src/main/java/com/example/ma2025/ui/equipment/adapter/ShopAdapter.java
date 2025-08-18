package com.example.ma2025.ui.equipment.adapter;

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

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private List<Equipment> shopItems;
    private OnPurchaseListener listener;

    public interface OnPurchaseListener {
        void onPurchaseItem(Equipment equipment);
    }

    public ShopAdapter(List<Equipment> shopItems, OnPurchaseListener listener) {
        this.shopItems = shopItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_equipment, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Equipment equipment = shopItems.get(position);
        holder.bind(equipment, listener);
    }

    @Override
    public int getItemCount() {
        return shopItems.size();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivEquipmentIcon;
        private TextView tvEquipmentName;
        private TextView tvEquipmentDescription;
        private TextView tvEquipmentType;
        private TextView tvPrice;
        private Button btnPurchase;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipmentIcon = itemView.findViewById(R.id.iv_equipment_icon);
            tvEquipmentName = itemView.findViewById(R.id.tv_equipment_name);
            tvEquipmentDescription = itemView.findViewById(R.id.tv_equipment_description);
            tvEquipmentType = itemView.findViewById(R.id.tv_equipment_type);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnPurchase = itemView.findViewById(R.id.btn_purchase);
        }

        public void bind(Equipment equipment, OnPurchaseListener listener) {
            tvEquipmentName.setText(equipment.getName());
            tvEquipmentDescription.setText(equipment.getDescription());
            tvEquipmentType.setText(equipment.getTypeText());
            tvPrice.setText(equipment.getPrice() + " novčića");

            // Set icon based on equipment type
            setEquipmentIcon(equipment);

            // Setup purchase button
            btnPurchase.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPurchaseItem(equipment);
                }
            });
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
    }
}