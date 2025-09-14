package com.example.ma2025.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.example.ma2025.data.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class CreateTaskViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private MutableLiveData<TaskCreationResult> taskCreationResult = new MutableLiveData<>();

    public CreateTaskViewModel(@NonNull Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        categoryRepository = CategoryRepository.getInstance(application);

        // Initialize user progress if needed
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.initializeUserProgress(userId);
        }
    }

    // Get all categories for the current user
    public LiveData<List<CategoryEntity>> getAllCategories() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return categoryRepository.getAllCategories(userId);
        }
        return new MutableLiveData<>();
    }

    // Get task by ID for editing
    public LiveData<TaskEntity> getTaskById(long taskId) {
        return taskRepository.getTaskById(taskId);
    }

    // Create a new task
    public void createTask(TaskEntity task) {
        // Validacija pre kreiranja
        TaskValidationResult validation = validateTask(task);
        if (!validation.isValid()) {
            taskCreationResult.postValue(new TaskCreationResult(false, validation.getErrorMessage(), -1));
            return;
        }

        taskRepository.insertTask(task, new TaskRepository.OnTaskInsertedCallback() {
            @Override
            public void onSuccess(long taskId) {
                // Task instances are generated in TaskRepository for repeating tasks
                taskCreationResult.postValue(new TaskCreationResult(true, "Zadatak je uspešno kreiran", taskId));
            }

            @Override
            public void onError(String error) {
                taskCreationResult.postValue(new TaskCreationResult(false, error, -1));
            }
        });
    }

    // Update existing task
    public void updateTask(TaskEntity task) {
        // Proveri da li se zadatak može uređivati
        if (!canTaskBeEdited(task)) {
            taskCreationResult.postValue(new TaskCreationResult(false,
                    "Završeni ili neurađeni zadaci se ne mogu menjati", -1));
            return;
        }

        // Validacija pre ažuriranja
        TaskValidationResult validation = validateTaskForUpdate(task);
        if (!validation.isValid()) {
            taskCreationResult.postValue(new TaskCreationResult(false, validation.getErrorMessage(), -1));
            return;
        }

        try {
            task.updatedAt = System.currentTimeMillis();
            task.syncedToFirebase = false;

            // Ako je ponavljajući zadatak, ažuriraj samo buduće instance
            if (task.isRepeating) {
                updateRecurringTaskFutureInstances(task);
            } else {
                taskRepository.updateTask(task);
            }

            taskCreationResult.postValue(new TaskCreationResult(true, "Zadatak je uspešno ažuriran", task.id));
        } catch (Exception e) {
            taskCreationResult.postValue(new TaskCreationResult(false, "Greška pri ažuriranju: " + e.getMessage(), -1));
        }
    }

    // Proveri da li se zadatak može uređivati
    private boolean canTaskBeEdited(TaskEntity task) {
        // Ne mogu se menjati završeni, neurađeni ili otkazani zadaci
        return task.status == TaskEntity.STATUS_ACTIVE || task.status == TaskEntity.STATUS_PAUSED;
    }

    // Ažuriraj samo buduće instance ponavljajućeg zadatka
    private void updateRecurringTaskFutureInstances(TaskEntity masterTask) {
        // Ova metoda će ažurirati master zadatak i sve buduće (nepokretnute) instance
        // Implementacija će biti u TaskRepository
        taskRepository.updateRecurringTaskFutureInstances(masterTask);
    }

    // Get current user ID from Firebase Auth
    public String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    // Get task creation result
    public LiveData<TaskCreationResult> getTaskCreationResult() {
        return taskCreationResult;
    }

    // Clear the result after handling
    public void clearResult() {
        taskCreationResult.setValue(null);
    }

    // Validate task creation constraints
    public TaskValidationResult validateTask(TaskEntity task) {
        return validateTaskCommon(task, false);
    }

    // Validate task for update (less strict on some fields)
    public TaskValidationResult validateTaskForUpdate(TaskEntity task) {
        return validateTaskCommon(task, true);
    }

    private TaskValidationResult validateTaskCommon(TaskEntity task, boolean isUpdate) {
        // Validate task name
        if (task.title == null || task.title.trim().isEmpty()) {
            return new TaskValidationResult(false, "Naziv zadatka je obavezan");
        }

        // Validate user ID
        if (task.userId == null || task.userId.trim().isEmpty()) {
            return new TaskValidationResult(false, "Korisnik nije identifikovan");
        }

        // Category validation je opciona - može biti null

        // Validate difficulty and importance ranges
        if (task.difficulty < 1 || task.difficulty > 4) {
            return new TaskValidationResult(false, "Težina mora biti između 1 i 4");
        }

        if (task.importance < 1 || task.importance > 4) {
            return new TaskValidationResult(false, "Bitnost mora biti između 1 i 4");
        }

        // Validate dates for repeating tasks
        if (task.isRepeating) {
            if (task.startDate == null || task.startDate <= 0) {
                return new TaskValidationResult(false, "Datum početka je obavezan za ponavljajuće zadatke");
            }
            if (task.endDate == null || task.endDate <= 0) {
                return new TaskValidationResult(false, "Datum završetka je obavezan za ponavljajuće zadatke");
            }
            if (task.startDate >= task.endDate) {
                return new TaskValidationResult(false, "Datum završetka mora biti posle datuma početka");
            }
            if (task.repeatInterval == null || task.repeatInterval <= 0) {
                return new TaskValidationResult(false, "Interval ponavljanja mora biti pozitivan broj");
            }
            if (task.repeatUnit == null || task.repeatUnit.trim().isEmpty()) {
                return new TaskValidationResult(false, "Jedinica ponavljanja je obavezna");
            }
        } else {
            // Validate due time for single tasks
            // For updates, allow past due times if task is not active
            if (task.dueTime == null) {
                return new TaskValidationResult(false, "Vreme izvršenja je obavezno");
            }

            if (!isUpdate && task.dueTime <= System.currentTimeMillis()) {
                return new TaskValidationResult(false, "Vreme izvršenja mora biti u budućnosti");
            }

            // For updates, only check future time if task is still active
            if (isUpdate && task.status == TaskEntity.STATUS_ACTIVE && task.dueTime <= System.currentTimeMillis()) {
                return new TaskValidationResult(false, "Vreme izvršenja mora biti u budućnosti za aktivne zadatke");
            }
        }

        return new TaskValidationResult(true, "Validacija uspešna");
    }

    // Calculate XP preview for task
    public int calculateXpPreview(int difficulty, int importance, int userLevel) {
        TaskEntity tempTask = new TaskEntity();
        tempTask.difficulty = difficulty;
        tempTask.importance = importance;

        return tempTask.calculateXpValue(userLevel);
    }

    // Get current user level for XP calculation
    public void getCurrentUserLevel(OnUserLevelCallback callback) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.getUserProgress(userId).observeForever(userProgress -> {
                if (userProgress != null) {
                    callback.onUserLevel(userProgress.currentLevel);
                } else {
                    callback.onUserLevel(0); // Default level
                }
            });
        } else {
            callback.onUserLevel(0);
        }
    }

    // Get current time for helpers
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    // Get category by ID
    public LiveData<CategoryEntity> getCategoryById(Long categoryId) {
        if (categoryId != null) {
            return categoryRepository.getCategoryById(categoryId.longValue());
        }
        return new MutableLiveData<>();
    }

    // Result classes
    public static class TaskCreationResult {
        private boolean success;
        private String message;
        private long taskId;

        public TaskCreationResult(boolean success, String message, long taskId) {
            this.success = success;
            this.message = message;
            this.taskId = taskId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return message; }
        public long getTaskId() { return taskId; }
    }

    public static class TaskValidationResult {
        private boolean valid;
        private String errorMessage;

        public TaskValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    // Callback interfaces
    public interface OnUserLevelCallback {
        void onUserLevel(int level);
    }
}