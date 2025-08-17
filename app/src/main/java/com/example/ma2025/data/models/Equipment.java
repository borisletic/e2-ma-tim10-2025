package com.example.ma2025.data.models;

public class Equipment {
    public enum EquipmentType {
        POTION, CLOTHING, WEAPON
    }

    public enum PotionType {
        TEMP_POWER_20, TEMP_POWER_40, PERM_POWER_5, PERM_POWER_10
    }

    public enum ClothingType {
        GLOVES, SHIELD, BOOTS
    }

    public enum WeaponType {
        SWORD, BOW
    }

    private String id;
    private String name;
    private EquipmentType type;
    private int effect; // procenat bonus
    private int price;
    private int durability; // za odeću (broj borbi)
    private boolean isActive;
    private boolean isPermanent; // za napitke i oružje
    private int level; // za unapređenje oružja
    private Object subType; // PotionType, ClothingType ili WeaponType

    public Equipment() {}

    public Equipment(String id, String name, EquipmentType type, Object subType, int effect, int price) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.subType = subType;
        this.effect = effect;
        this.price = price;
        this.isActive = false;
        this.level = 1;

        if (type == EquipmentType.CLOTHING) {
            this.durability = 2; // Odeća traje 2 borbe
            this.isPermanent = false;
        } else if (type == EquipmentType.POTION) {
            this.isPermanent = (subType == PotionType.PERM_POWER_5 || subType == PotionType.PERM_POWER_10);
        } else {
            this.isPermanent = true; // Oružje je trajno
        }
    }

    // Getteri
    public String getId() { return id; }
    public String getName() { return name; }
    public EquipmentType getType() { return type; }
    public int getEffect() { return effect; }
    public int getPrice() { return price; }
    public int getDurability() { return durability; }
    public boolean isActive() { return isActive; }
    public boolean isPermanent() { return isPermanent; }
    public int getLevel() { return level; }
    public Object getSubType() { return subType; }

    // Setteri
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(EquipmentType type) { this.type = type; }
    public void setEffect(int effect) { this.effect = effect; }
    public void setPrice(int price) { this.price = price; }
    public void setDurability(int durability) { this.durability = durability; }
    public void setActive(boolean active) { isActive = active; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }
    public void setLevel(int level) { this.level = level; }
    public void setSubType(Object subType) { this.subType = subType; }

    // Utility metode
    public void use() {
        if (type == EquipmentType.CLOTHING && durability > 0) {
            durability--;
            if (durability <= 0) {
                isActive = false;
            }
        } else if (type == EquipmentType.POTION && !isPermanent) {
            isActive = false; // Jednokratni napici se troše
        }
    }

    public void upgrade() {
        if (type == EquipmentType.WEAPON) {
            level++;
            // Povećaj efekat za 0.01%
            effect += 1; // Predstavlja 0.01% u setinkama
        }
    }

    public int getUpgradePrice(int baseReward) {
        return (int) (baseReward * 0.6);
    }

    public boolean canStack() {
        return type == EquipmentType.CLOTHING || (type == EquipmentType.WEAPON && isPermanent);
    }
}