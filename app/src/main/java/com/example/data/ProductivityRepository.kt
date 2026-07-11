package com.example.data

import kotlinx.coroutines.flow.Flow

class ProductivityRepository(private val db: AppDatabase) {

    val tasks: Flow<List<Task>> = db.taskDao().getAllTasks()
    val habits: Flow<List<Habit>> = db.habitDao().getAllHabits()
    val focusSessions: Flow<List<FocusSession>> = db.focusSessionDao().getAllFocusSessions()

    suspend fun insertTask(task: Task) {
        db.taskDao().insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        db.taskDao().updateTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        db.taskDao().deleteTaskById(id)
    }

    suspend fun insertHabit(habit: Habit) {
        db.habitDao().insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        db.habitDao().updateHabit(habit)
    }

    suspend fun deleteHabitById(id: Int) {
        db.habitDao().deleteHabitById(id)
    }

    suspend fun insertFocusSession(session: FocusSession) {
        db.focusSessionDao().insertSession(session)
    }

    suspend fun getReflectionForDate(date: String): DailyReflection? {
        return db.dailyReflectionDao().getReflectionByDate(date)
    }

    suspend fun insertDailyReflection(reflection: DailyReflection) {
        db.dailyReflectionDao().insertReflection(reflection)
    }
}
