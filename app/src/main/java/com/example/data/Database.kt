package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // e.g. "Work", "Personal", "Health", "Urgent"
    val priority: String, // e.g. "Urgent & Important", "Important but Not Urgent", "Urgent but Not Important", "Not Urgent & Not Important"
    val isCompleted: Boolean = false,
    val dueDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val subtasksRaw: String = "" // "Subtask 1:::false;;;Subtask 2:::true"
) {
    fun getSubtasks(): List<Subtask> {
        if (subtasksRaw.isBlank()) return emptyList()
        return subtasksRaw.split(";;;").mapNotNull { raw ->
            val parts = raw.split(":::")
            if (parts.size == 2) {
                Subtask(parts[0], parts[1].toBoolean())
            } else null
        }
    }
}

data class Subtask(
    val title: String,
    val isCompleted: Boolean
)

fun List<Subtask>.toRawString(): String {
    return joinToString(";;;") { "${it.title}:::${it.isCompleted}" }
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val emoji: String,
    val streak: Int = 0,
    val lastCompletedDate: String = "" // "YYYY-MM-DD"
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskTitle: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_reflections")
data class DailyReflection(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val energyLevel: String = "Medium", // "High", "Medium", "Low"
    val morningIntentions: String = "",
    val eveningReflection: String = "",
    val isMorningCompleted: Boolean = false,
    val isEveningCompleted: Boolean = false
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY streak DESC, id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)
}

@Dao
interface DailyReflectionDao {
    @Query("SELECT * FROM daily_reflections WHERE date = :date")
    suspend fun getReflectionByDate(date: String): DailyReflection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: DailyReflection)
}

@Database(
    entities = [Task::class, Habit::class, FocusSession::class, DailyReflection::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailyReflectionDao(): DailyReflectionDao
}
