package com.example.ma2025.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.ma2025.data.database.dao.BossDao;
import com.example.ma2025.data.database.entities.BossEntity;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.database.entities.TaskCompletionEntity;
import com.example.ma2025.data.database.entities.DailyStatsEntity;
import com.example.ma2025.data.database.entities.UserProgressEntity;
import com.example.ma2025.data.database.dao.TaskDao;
import com.example.ma2025.data.database.dao.CategoryDao;
import com.example.ma2025.data.database.dao.TaskCompletionDao;
import com.example.ma2025.data.database.dao.DailyStatsDao;
import com.example.ma2025.data.database.dao.UserProgressDao;

@Database(
        entities = {
                TaskEntity.class,
                CategoryEntity.class,
                TaskCompletionEntity.class,
                DailyStatsEntity.class,
                UserProgressEntity.class,
                BossEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "ma2025_database";
    private static volatile AppDatabase INSTANCE;

    // Abstract methods for DAOs
    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract TaskCompletionDao taskCompletionDao();
    public abstract DailyStatsDao dailyStatsDao();
    public abstract UserProgressDao userProgressDao();
    public abstract BossDao bossDao();

    // Migration for adding parent_task_id field
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Dodaj novo polje parent_task_id za ponavljajuće zadatke
            database.execSQL("ALTER TABLE tasks ADD COLUMN parent_task_id INTEGER");
            // Dodaj index za novo polje
            database.execSQL("CREATE INDEX index_tasks_parent_task_id ON tasks(parent_task_id)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Kreiranje bosses tabele
            database.execSQL("CREATE TABLE IF NOT EXISTS bosses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "level INTEGER NOT NULL, " +
                    "max_hp INTEGER NOT NULL, " +
                    "current_hp INTEGER NOT NULL, " +
                    "is_defeated INTEGER NOT NULL, " +
                    "coins_reward INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL)");
        }
    };

    // Singleton pattern
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addCallback(roomCallback)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigration() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Database callback for initial setup
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Insert default categories when database is created
            new Thread(() -> {
                if (INSTANCE != null) {
                    populateDefaultCategories();
                }
            }).start();
        }
    };

    // Populate default categories
    private static void populateDefaultCategories() {
        // This will be called when user first creates categories
        // Default categories will be added per user when they register
    }

    // Helper method to close database (for testing)
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    // Helper method to check if database exists
    public static boolean databaseExists(Context context) {
        return context.getDatabasePath(DATABASE_NAME).exists();
    }

    // Helper method to delete database (for testing/reset)
    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
        INSTANCE = null;
    }
}