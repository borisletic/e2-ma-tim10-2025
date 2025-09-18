package com.example.ma2025.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "bosses")
public class BossEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "level")
    public int level; // Nivo bosa (1, 2, 3...)

    @ColumnInfo(name = "max_hp")
    public int maxHp; // Maksimalni HP bosa

    @ColumnInfo(name = "current_hp")
    public int currentHp; // Trenutni HP bosa

    @ColumnInfo(name = "is_defeated")
    public boolean isDefeated; // Da li je bos poražen

    @ColumnInfo(name = "coins_reward")
    public int coinsReward; // Nagrada u novčićima

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public BossEntity() {
        this.userId = "";
        this.level = 1;
        this.maxHp = 200; // Prvi bos ima 200 HP
        this.currentHp = 200;
        this.isDefeated = false;
        this.coinsReward = 200; // Prvi bos daje 200 novčića
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public BossEntity(@NonNull String userId, int level) {
        this();
        this.userId = userId;
        this.level = level;
        this.maxHp = calculateBossHp(level);
        this.currentHp = this.maxHp;
        this.coinsReward = calculateCoinsReward(level);
    }

    /**
     * Računa HP bosa prema formuli iz specifikacije:
     * Prvi bos: 200 HP
     * Sledeći: HP prethodnog bosa * 2 + HP prethodnog bosa / 2
     */
    private int calculateBossHp(int level) {
        if (level == 1) {
            return 200;
        }

        int previousBossHp = calculateBossHp(level - 1);
        return previousBossHp * 2 + previousBossHp / 2;
    }

    /**
     * Računa nagradu u novčićima prema specifikaciji:
     * Prvi bos: 200 novčića
     * Sledeći: 20% više nego prethodnji
     */
    private int calculateCoinsReward(int level) {
        if (level == 1) {
            return 200;
        }

        int previousReward = calculateCoinsReward(level - 1);
        return (int) (previousReward * 1.2); // 20% više
    }

    /**
     * Nanosi štetu bosu
     * @param damage količina štete
     * @return true ako je bos poražen
     */
    public boolean takeDamage(int damage) {
        this.currentHp -= damage;
        if (this.currentHp <= 0) {
            this.currentHp = 0;
            this.isDefeated = true;
        }
        this.updatedAt = System.currentTimeMillis();
        return this.isDefeated;
    }

    /**
     * Resetuje bosa na punu snagu
     */
    public void reset() {
        this.currentHp = this.maxHp;
        this.isDefeated = false;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Računa procenat HP-a koji je preostao
     */
    public float getHpPercentage() {
        if (maxHp == 0) return 0f;
        return (float) currentHp / maxHp;
    }

    /**
     * Proverava da li je bos živ
     */
    public boolean isAlive() {
        return currentHp > 0 && !isDefeated;
    }
}