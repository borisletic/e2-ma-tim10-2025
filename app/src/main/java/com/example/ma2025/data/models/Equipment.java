package com.example.ma2025.data.models;

import com.example.ma2025.utils.Constants;

public class Equipment {

    private String id;
    private String userId;
    private String name;
    private String description;
    private int type; // 1=Potion, 2=Clothing, 3=Weapon
    private int subType; // Specific type within category
    private double effectValue; // Percentage or amount
    private String effectType; // PP_BOOST, XP_BOOST, ATTACK_BOOST, etc.
    private int price; // In coins
    private boolean isActive;
    private boolean isPermanent; // true for permanent effects
    private int usesRemaining; // For temporary items (clothing = 2 battles)
    private long purchaseTime;
    private long activationTime;
    private String iconName;

    // Constructors
    public Equipment() {
        this.purchaseTime = System.currentTimeMillis();
        this.isActive = false;
        this.usesRemaining = 0;
        this.isPermanent = false;
    }

    // U Equipment.java dodajte ovaj konstruktor:
    public Equipment(Equipment other) {
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.subType = other.subType;
        this.effectValue = other.effectValue;
        this.effectType = other.effectType;
        this.price = other.price;
        this.isPermanent = other.isPermanent;
        this.iconName = other.iconName;
        this.usesRemaining = other.usesRemaining;
        this.isActive = false; // Nova oprema nije aktivna
        this.purchaseTime = System.currentTimeMillis();
    }

    public Equipment(String name, String description, int type, int subType,
                     double effectValue, String effectType, int price, boolean isPermanent) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.subType = subType;
        this.effectValue = effectValue;
        this.effectType = effectType;
        this.price = price;
        this.isPermanent = isPermanent;

        // Set uses based on type
        if (type == Constants.EQUIPMENT_TYPE_CLOTHING) {
            this.usesRemaining = 2; // Clothing lasts 2 battles
        } else if (type == Constants.EQUIPMENT_TYPE_POTION && !isPermanent) {
            this.usesRemaining = 1; // Single-use potions
        } else {
            this.usesRemaining = -1; // Permanent items
        }
    }

    // Static factory methods for creating equipment
    public static Equipment createPotion(String name, double ppBoost, int pricePercent, boolean isPermanent) {
        Equipment potion = new Equipment();
        potion.name = name;
        potion.description = isPermanent ?
                "Trajno povećava PP za " + (int)(ppBoost * 100) + "%" :
                "Jednokratno povećava PP za " + (int)(ppBoost * 100) + "%";
        potion.type = Constants.EQUIPMENT_TYPE_POTION;
        potion.effectValue = ppBoost;
        potion.effectType = Constants.EFFECT_PP_BOOST;
        potion.isPermanent = isPermanent;
        potion.iconName = "potion_" + (isPermanent ? "permanent" : "temporary");
        potion.usesRemaining = isPermanent ? -1 : 1; // -1 for permanent, 1 for single use
        return potion;
    }

    public static Equipment createClothing(String name, String effectType, double effectValue, int pricePercent) {
        Equipment clothing = new Equipment();
        clothing.name = name;
        clothing.type = Constants.EQUIPMENT_TYPE_CLOTHING;
        clothing.effectType = effectType;
        clothing.effectValue = effectValue;
        clothing.isPermanent = false;
        clothing.usesRemaining = 2;

        switch (effectType) {
            case Constants.EFFECT_PP_BOOST:
                clothing.description = "Povećava snagu za " + (int)(effectValue * 100) + "% (2 borbe)";
                clothing.iconName = "gloves";
                break;
            case Constants.EFFECT_ATTACK_BOOST:
                clothing.description = "Povećava šansu napada za " + (int)(effectValue * 100) + "% (2 borbe)";
                clothing.iconName = "shield";
                break;
            case "extra_attack":
                clothing.description = (int)(effectValue * 100) + "% šanse za dodatni napad (2 borbe)";
                clothing.iconName = "boots";
                break;
        }
        return clothing;
    }

    public static Equipment createWeapon(String name, String effectType, double effectValue) {
        Equipment weapon = new Equipment();
        weapon.name = name;
        weapon.type = Constants.EQUIPMENT_TYPE_WEAPON;
        weapon.effectType = effectType;
        weapon.effectValue = effectValue;
        weapon.isPermanent = true;
        weapon.price = 0; // Weapons are only obtained from boss battles
        weapon.usesRemaining = -1; // Permanent

        switch (effectType) {
            case Constants.EFFECT_PP_BOOST:
                weapon.description = "Trajno povećava snagu za " + (int)(effectValue * 100) + "%";
                weapon.iconName = "sword";
                break;
            case Constants.EFFECT_COIN_BOOST:
                weapon.description = "Trajno povećava novčiće za " + (int)(effectValue * 100) + "%";
                weapon.iconName = "bow";
                break;
        }
        return weapon;
    }

    // Business logic methods
    public boolean canActivate() {
        return !isActive && (usesRemaining > 0 || usesRemaining == -1);
    }

    public void activate() {
        if (canActivate()) {
            isActive = true;
            activationTime = System.currentTimeMillis();
        }
    }

    public void deactivate() {
        isActive = false;
    }

    public void useOnce() {
        if (isActive && usesRemaining > 0) {
            usesRemaining--;
            if (usesRemaining <= 0 && !isPermanent) {
                deactivate();
            }
        }
    }

    public boolean isExpired() {
        return !isPermanent && usesRemaining <= 0;
    }

    public String getStatusText() {
        if (!isActive) return "Neaktivno";
        if (isPermanent) return "Aktivno (trajno)";
        if (usesRemaining == -1) return "Aktivno (trajno)";
        return "Aktivno (" + usesRemaining + " preostalo)";
    }

    public String getTypeText() {
        switch (type) {
            case Constants.EQUIPMENT_TYPE_POTION: return "Napitak";
            case Constants.EQUIPMENT_TYPE_CLOTHING: return "Odeća";
            case Constants.EQUIPMENT_TYPE_WEAPON: return "Oružje";
            default: return "Nepoznato";
        }
    }

    // Calculate price based on boss reward percentage
    public static int calculatePrice(int bossReward, int percentage) {
        return (int) ((double) bossReward * percentage / 100.0);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public int getSubType() { return subType; }
    public void setSubType(int subType) { this.subType = subType; }

    public double getEffectValue() { return effectValue; }
    public void setEffectValue(double effectValue) { this.effectValue = effectValue; }

    public String getEffectType() { return effectType; }
    public void setEffectType(String effectType) { this.effectType = effectType; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isPermanent() { return isPermanent; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }

    public int getUsesRemaining() { return usesRemaining; }
    public void setUsesRemaining(int usesRemaining) { this.usesRemaining = usesRemaining; }

    public long getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(long purchaseTime) { this.purchaseTime = purchaseTime; }

    public long getActivationTime() { return activationTime; }
    public void setActivationTime(long activationTime) { this.activationTime = activationTime; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

}