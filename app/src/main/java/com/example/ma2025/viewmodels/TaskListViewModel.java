package com.example.ma2025.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.ma2025.data.database.entities.TaskEntity;
import com.example.ma2025.data.database.entities.CategoryEntity;
import com.example.ma2025.data.repositories.TaskRepository;
import com.example.ma2025.data.repositories.CategoryRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskListViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private MutableLiveData<TaskCompletionResult> taskCompletionResult = new MutableLiveData<>();

    // Search and filtering
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private MutableLiveData<Integer> sortType = new MutableLiveData<>(SORT_BY_DUE_DATE);
    private MediatorLiveData<List<TaskEntity>> filteredTasks = new MediatorLiveData<>();

    // Sort constants
    public static final int SORT_BY_DUE_DATE = 0;
    public static final int SORT_BY_PRIORITY = 1;
    public static final int SORT_BY_CATEGORY = 2;
    public static final int SORT_BY_CREATED_DATE = 3;
    public static final int SORT_BY_STATUS = 4;

    public TaskListViewModel(@NonNull Application application) {
        super(application);
        taskRepository = TaskRepository.getInstance(application);
        categoryRepository = CategoryRepository.getInstance(application);

        setupFilteredTasks();
    }

    // ========== EXISTING METHODS ==========

    // Get all tasks for user
    public LiveData<List<TaskEntity>> getAllTasks(String userId) {
        return taskRepository.getAllTasks(userId);
    }

    // Get all tasks (bez userId parametra - koristi trenutnog korisnika)
    public LiveData<List<TaskEntity>> getAllTasks() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return getAllTasks(userId);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }

    // Get task by ID
    public LiveData<TaskEntity> getTaskById(long taskId) {
        return taskRepository.getTaskById(taskId);
    }

    // Get tasks by status
    public LiveData<List<TaskEntity>> getTasksByStatus(String userId, int status) {
        return taskRepository.getTasksByStatus(userId, status);
    }

    // Complete a task
    public void completeTask(long taskId) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.completeTask(taskId, userId, new TaskRepository.OnTaskCompletedCallback() {
                @Override
                public void onSuccess(int xpEarned, int newLevel) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            "Zadatak završen! +" + xpEarned + " XP", xpEarned, newLevel));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    public void failTask(long taskId) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.failTask(taskId, userId, new TaskRepository.OnTaskCompletedCallback() {
                @Override
                public void onSuccess(int xpEarned, int newLevel) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            "Zadatak označen kao neurađen", 0, 0));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    public void deleteTask(TaskEntity task) {
        taskRepository.deleteTask(task);
    }

    public void deleteRecurringTask(long taskId) {
        taskRepository.deleteRecurringTask(taskId);
    }

    // Get current user ID
    public String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    // Get task completion result
    public LiveData<TaskCompletionResult> getTaskCompletionResult() {
        return taskCompletionResult;
    }

    // Clear result after handling
    public void clearResult() {
        taskCompletionResult.setValue(null);
    }

    // ========== NEW METHODS FOR PAUSE/RESUME ==========

    public void pauseTask(long taskId) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.pauseTask(taskId, userId, new TaskRepository.OnTaskStatusChangeCallback() {
                @Override
                public void onSuccess(String message) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            "Zadatak pauziran", 0, 0));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    public void resumeTask(long taskId) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.resumeTask(taskId, userId, new TaskRepository.OnTaskStatusChangeCallback() {
                @Override
                public void onSuccess(String message) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            "Zadatak ponovo aktiviran", 0, 0));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    public void cancelTask(long taskId) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.cancelTask(taskId, userId, new TaskRepository.OnTaskStatusChangeCallback() {
                @Override
                public void onSuccess(String message) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            "Zadatak otkazan", 0, 0));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    // ========== SEARCH AND FILTERING ==========

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSortType(int sortType) {
        this.sortType.setValue(sortType);
    }

    public LiveData<Integer> getSortType() {
        return sortType;
    }

    public LiveData<List<TaskEntity>> getFilteredTasks() {
        return filteredTasks;
    }

    private void setupFilteredTasks() {
        String userId = getCurrentUserId();
        if (userId != null) {
            // Add sources to mediator
            filteredTasks.addSource(getAllTasks(userId), tasks -> updateFilteredTasks());
            filteredTasks.addSource(searchQuery, query -> updateFilteredTasks());
            filteredTasks.addSource(sortType, type -> updateFilteredTasks());
        }
    }

    private void updateFilteredTasks() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        List<TaskEntity> allTasks = getAllTasks(userId).getValue();
        if (allTasks == null) {
            filteredTasks.setValue(new ArrayList<>());
            return;
        }

        String query = searchQuery.getValue();
        Integer sort = sortType.getValue();

        List<TaskEntity> filtered = new ArrayList<>();

        // Filter by search query
        for (TaskEntity task : allTasks) {
            if (query == null || query.isEmpty() ||
                    task.title.toLowerCase().contains(query.toLowerCase()) ||
                    (task.description != null && task.description.toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(task);
            }
        }

        // Sort the filtered list
        if (sort != null) {
            sortTasks(filtered, sort);
        }

        filteredTasks.setValue(filtered);
    }

    private void sortTasks(List<TaskEntity> tasks, int sortType) {
        switch (sortType) {
            case SORT_BY_DUE_DATE:
                Collections.sort(tasks, (t1, t2) -> {
                    if (t1.dueTime == null && t2.dueTime == null) return 0;
                    if (t1.dueTime == null) return 1;
                    if (t2.dueTime == null) return -1;
                    return Long.compare(t1.dueTime, t2.dueTime);
                });
                break;

            case SORT_BY_PRIORITY:
                Collections.sort(tasks, (t1, t2) -> {
                    int priority1 = calculatePriority(t1);
                    int priority2 = calculatePriority(t2);
                    return Integer.compare(priority2, priority1); // Descending
                });
                break;

            case SORT_BY_CATEGORY:
                Collections.sort(tasks, (t1, t2) -> {
                    if (t1.categoryId == null && t2.categoryId == null) return 0;
                    if (t1.categoryId == null) return 1;
                    if (t2.categoryId == null) return -1;
                    return Long.compare(t1.categoryId, t2.categoryId);
                });
                break;

            case SORT_BY_CREATED_DATE:
                Collections.sort(tasks, (t1, t2) -> Long.compare(t2.createdAt, t1.createdAt)); // Newest first
                break;

            case SORT_BY_STATUS:
                Collections.sort(tasks, (t1, t2) -> Integer.compare(t1.status, t2.status));
                break;
        }
    }

    private int calculatePriority(TaskEntity task) {
        int baseScore = task.difficulty * 10 + task.importance * 15;

        // Add urgency bonus if task has due time
        if (task.dueTime != null) {
            long timeUntilDue = task.dueTime - System.currentTimeMillis();
            long hoursUntilDue = timeUntilDue / (1000 * 60 * 60);

            if (hoursUntilDue < 1) baseScore += 100;      // Due in less than 1 hour
            else if (hoursUntilDue < 6) baseScore += 50;   // Due in less than 6 hours
            else if (hoursUntilDue < 24) baseScore += 25;  // Due today
            else if (hoursUntilDue < 72) baseScore += 10;  // Due in 3 days
        }

        return baseScore;
    }

    // ========== BATCH OPERATIONS ==========

    public void completeBatchTasks(List<Long> taskIds) {
        String userId = getCurrentUserId();
        if (userId != null) {
            taskRepository.completeBatchTasks(taskIds, userId, new TaskRepository.OnBatchOperationCallback() {
                @Override
                public void onSuccess(int completedCount, int totalXpEarned) {
                    taskCompletionResult.postValue(new TaskCompletionResult(true,
                            completedCount + " zadataka završeno! +" + totalXpEarned + " XP",
                            totalXpEarned, 0));
                }

                @Override
                public void onError(String error) {
                    taskCompletionResult.postValue(new TaskCompletionResult(false, error, 0, 0));
                }
            });
        }
    }

    public void deleteBatchTasks(List<TaskEntity> tasks) {
        taskRepository.deleteBatchTasks(tasks);
        taskCompletionResult.postValue(new TaskCompletionResult(true,
                tasks.size() + " zadataka obrisano", 0, 0));
    }

    // ========== CATEGORIES ==========

    public LiveData<List<CategoryEntity>> getAllCategories() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return categoryRepository.getAllCategories(userId);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }

    // ========== OVERDUE TASKS ==========

    public LiveData<List<TaskEntity>> getOverdueTasks() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return taskRepository.getOverdueTasksLiveData(userId);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<Integer> getOverdueTasksCount() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return taskRepository.getOverdueTasksCount(userId);
        }
        MutableLiveData<Integer> count = new MutableLiveData<>();
        count.setValue(0);
        return count;
    }

    // ========== STATISTICS ==========

    public LiveData<TaskStatistics> getTaskStatistics() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return taskRepository.getTaskStatistics(userId);
        }
        return new MutableLiveData<>();
    }

    // Result class for task completion
    public static class TaskCompletionResult {
        private boolean success;
        private String message;
        private int xpEarned;
        private int newLevel;

        public TaskCompletionResult(boolean success, String message, int xpEarned, int newLevel) {
            this.success = success;
            this.message = message;
            this.xpEarned = xpEarned;
            this.newLevel = newLevel;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return message; }
        public int getXpEarned() { return xpEarned; }
        public int getNewLevel() { return newLevel; }
    }

    // Statistics class
    public static class TaskStatistics {
        public int totalTasks;
        public int completedTasks;
        public int activeTasks;
        public int failedTasks;
        public int pausedTasks;
        public int overdueTasksCount;
        public double completionRate;

        public TaskStatistics(int total, int completed, int active, int failed, int paused, int overdue) {
            this.totalTasks = total;
            this.completedTasks = completed;
            this.activeTasks = active;
            this.failedTasks = failed;
            this.pausedTasks = paused;
            this.overdueTasksCount = overdue;
            this.completionRate = total > 0 ? ((double) completed / total) * 100 : 0;
        }
    }
}