package com.example.ma2025.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
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
                UserProgressEntity.class
        },
        version = 1,
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

    // Migration example (for future database updates)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Example migration - add new column
            // database.execSQL("ALTER TABLE tasks ADD COLUMN new_column TEXT");
        }
    };

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