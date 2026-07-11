package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductivityViewModel(application: Application, private val repository: ProductivityRepository) : AndroidViewModel(application) {

    // Tab Navigation State
    var currentTab by mutableStateOf(0) // 0: Home, 1: Planner, 2: Stats, 3: More (which includes settings & guide)

    // Tasks and Habits States
    val tasks: StateFlow<List<Task>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Habit>> = repository.habits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSession>> = repository.focusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pomodoro Timer State
    var selectedTaskForFocus by mutableStateOf<Task?>(null)
    var timerSecondsRemaining by mutableStateOf(25 * 60)
    var isTimerRunning by mutableStateOf(false)
    private var timerJob: Job? = null

    // Daily Reflection State
    var currentReflection by mutableStateOf<DailyReflection?>(null)

    // AI Suggestions
    var aiSuggestion by mutableStateOf("Focus on completing your highest priority task now.")
    var isAiLoading by mutableStateOf(false)

    init {
        // Initialize default habits if db is empty
        viewModelScope.launch {
            repository.habits.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertHabit(Habit(name = "Hydration", emoji = "💧", streak = 8, lastCompletedDate = getYesterdayDateString()))
                    repository.insertHabit(Habit(name = "Meditate", emoji = "🧘", streak = 12, lastCompletedDate = getYesterdayDateString()))
                    repository.insertHabit(Habit(name = "Read", emoji = "📖", streak = 3, lastCompletedDate = getYesterdayDateString()))
                }
            }
            // Initialize default tasks if db is empty
            repository.tasks.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertTask(Task(
                        title = "Draft PRD for Mobile MVP",
                        category = "Work",
                        priority = "Urgent & Important",
                        isCompleted = false,
                        notes = "Incorporate habits and focus modes."
                    ))
                    repository.insertTask(Task(
                        title = "Morning Review Ritual",
                        category = "Daily Ritual",
                        priority = "Important but Not Urgent",
                        isCompleted = true
                    ))
                }
            }
            loadTodayReflection()
            refreshAiSuggestion()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday)
    }

    fun loadTodayReflection() {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val reflection = repository.getReflectionForDate(todayStr)
            if (reflection != null) {
                currentReflection = reflection
            } else {
                val newReflection = DailyReflection(date = todayStr)
                repository.insertDailyReflection(newReflection)
                currentReflection = newReflection
            }
        }
    }

    // Task Management
    fun addTask(title: String, category: String, priority: String, notes: String, subtasksList: List<String> = emptyList()) {
        viewModelScope.launch {
            val subtasks = subtasksList.map { Subtask(it, false) }.toRawString()
            val newTask = Task(
                title = title,
                category = category,
                priority = priority,
                notes = notes,
                subtasksRaw = subtasks
            )
            repository.insertTask(newTask)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
        }
    }

    fun toggleSubtaskCompletion(task: Task, subtaskIndex: Int) {
        viewModelScope.launch {
            val subtasks = task.getSubtasks().toMutableList()
            if (subtaskIndex in subtasks.indices) {
                val sub = subtasks[subtaskIndex]
                subtasks[subtaskIndex] = sub.copy(isCompleted = !sub.isCompleted)
                val updated = task.copy(subtasksRaw = subtasks.toRawString())
                repository.updateTask(updated)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTaskById(task.id)
            if (selectedTaskForFocus?.id == task.id) {
                selectedTaskForFocus = null
                stopTimer()
            }
        }
    }

    // Habit Management
    fun addHabit(name: String, emoji: String) {
        viewModelScope.launch {
            repository.insertHabit(Habit(name = name, emoji = emoji))
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val yesterdayStr = getYesterdayDateString()
            
            val updated = if (habit.lastCompletedDate == todayStr) {
                // Already completed today, unchecking it (decrement streak, clear date)
                val newStreak = (habit.streak - 1).coerceAtLeast(0)
                habit.copy(streak = newStreak, lastCompletedDate = "")
            } else {
                // Complete for today
                val newStreak = if (habit.lastCompletedDate == yesterdayStr || habit.lastCompletedDate.isEmpty()) {
                    habit.streak + 1
                } else {
                    1 // Streak reset if missed days
                }
                habit.copy(streak = newStreak, lastCompletedDate = todayStr)
            }
            repository.updateHabit(updated)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabitById(habit.id)
        }
    }

    // Daily Reflection Ritual Updates
    fun updateDailyReflection(energyLevel: String, morningIntentions: String, eveningReflection: String, isMorningCompleted: Boolean, isEveningCompleted: Boolean) {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val updated = DailyReflection(
                date = todayStr,
                energyLevel = energyLevel,
                morningIntentions = morningIntentions,
                eveningReflection = eveningReflection,
                isMorningCompleted = isMorningCompleted,
                isEveningCompleted = isEveningCompleted
            )
            repository.insertDailyReflection(updated)
            currentReflection = updated
            refreshAiSuggestion()
        }
    }

    // Pomodoro Timer Control
    fun startTimer(task: Task?) {
        if (isTimerRunning) return
        selectedTaskForFocus = task
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (timerSecondsRemaining > 0 && isTimerRunning) {
                delay(1000)
                timerSecondsRemaining--
            }
            if (timerSecondsRemaining <= 0) {
                // Focus session completed!
                task?.let {
                    repository.insertFocusSession(
                        FocusSession(taskTitle = it.title, durationMinutes = 25)
                    )
                }
                timerSecondsRemaining = 25 * 60
                isTimerRunning = false
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
    }

    fun stopTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        timerSecondsRemaining = 25 * 60
    }

    // AI suggestion integration
    fun refreshAiSuggestion() {
        viewModelScope.launch {
            isAiLoading = true
            val currentTasks = tasks.value
            val currentHabits = habits.value
            val energy = currentReflection?.energyLevel ?: "Medium"
            aiSuggestion = GeminiClient.getAiSuggestion(currentTasks, currentHabits, energy)
            isAiLoading = false
        }
    }
}
