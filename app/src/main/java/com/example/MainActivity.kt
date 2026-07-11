package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.ProductivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Factory for ProductivityViewModel
class ProductivityViewModelFactory(
    private val application: Application,
    private val repository: ProductivityRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductivityViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup local database
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "productivity_db"
        ).fallbackToDestructiveMigration().build()

        val repository = ProductivityRepository(db)
        val factory = ProductivityViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[ProductivityViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar(viewModel) },
                    containerColor = NaturalBg
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        when (viewModel.currentTab) {
                            0 -> HomeScreen(viewModel)
                            1 -> PlannerScreen(viewModel)
                            2 -> StatsScreen(viewModel)
                            3 -> MoreScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// --- Navigation ---
@Composable
fun BottomNavBar(viewModel: ProductivityViewModel) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.border(width = 1.dp, color = NaturalBorder)
    ) {
        val items = listOf(
            Triple(0, Icons.Default.Home, "Home"),
            Triple(1, Icons.AutoMirrored.Filled.List, "Planner"),
            Triple(2, Icons.Default.Analytics, "Stats"),
            Triple(3, Icons.Default.Settings, "More")
        )

        items.forEach { (index, icon, label) ->
            val isSelected = viewModel.currentTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { viewModel.currentTab = index },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NaturalPrimary else NaturalTextSecondary.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isSelected) NaturalPrimary else NaturalTextSecondary.copy(alpha = 0.6f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NaturalPrimaryContainer
                ),
                modifier = Modifier.testTag("nav_tab_$index")
            )
        }
    }
}

// --- Screens ---

@Composable
fun HomeScreen(viewModel: ProductivityViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }

    val activeTasks = tasks.filter { !it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header (Good morning, Julian / Profile)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Good morning,",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = NaturalText
                    )
                    Text(
                        text = "Julian",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = NaturalPrimary
                    )
                }
                // Custom Avatar matching the design HTML
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NaturalPrimaryContainer)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = NaturalPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 2. AI Suggestions Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NaturalContainer)
                    .clickable { viewModel.refreshAiSuggestion() }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NaturalPrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "AI ADVISOR",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (viewModel.isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NaturalPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = viewModel.aiSuggestion,
                            color = NaturalTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.0f)
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = NaturalPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Current Focus Pomodoro session
        item {
            FocusSessionCard(viewModel)
        }

        // 4. Today's Roadmap
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TODAY'S ROADMAP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NaturalTextSecondary
                )
                Text(
                    text = "${activeTasks.size} tasks left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NaturalPrimary
                )
            }
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your roadmap is empty. Add a task to start your day!",
                        color = NaturalTextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(tasks.take(4)) { task ->
                TaskCardItem(
                    task = task,
                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                    onClick = { selectedTaskForDetail = task }
                )
            }
        }

        // 5. Habit Streaks
        item {
            Text(
                text = "HABIT STREAKS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NaturalTextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                habits.forEach { habit ->
                    HabitStreakItem(habit = habit, onClick = { viewModel.toggleHabit(habit) })
                }
            }
        }

        // Floating Action Button spacer
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Floating add action button layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = NaturalPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("add_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category, priority, notes, subtasks ->
                viewModel.addTask(title, category, priority, notes, subtasks)
                showAddTaskDialog = false
            }
        )
    }

    if (selectedTaskForDetail != null) {
        TaskDetailDialog(
            task = selectedTaskForDetail!!,
            viewModel = viewModel,
            onDismiss = { selectedTaskForDetail = null }
        )
    }
}

@Composable
fun PlannerScreen(viewModel: ProductivityViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    
    var selectedPlannerTab by remember { mutableStateOf(0) } // 0: Tasks, 1: Habits, 2: Eisenhower Matrix
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Planner & Structure",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NaturalPrimary
        )
        Text(
            text = "Plan doing tasks the right way without overwhelming yourself.",
            style = MaterialTheme.typography.bodyMedium,
            color = NaturalTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Planner Sub-tabs
        TabRow(
            selectedTabIndex = selectedPlannerTab,
            containerColor = Color.Transparent,
            contentColor = NaturalPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPlannerTab]),
                    color = NaturalPrimary
                )
            }
        ) {
            Tab(
                selected = selectedPlannerTab == 0,
                onClick = { selectedPlannerTab = 0 },
                text = { Text("All Tasks", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedPlannerTab == 1,
                onClick = { selectedPlannerTab = 1 },
                text = { Text("Habits", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedPlannerTab == 2,
                onClick = { selectedPlannerTab = 2 },
                text = { Text("Eisenhower", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedPlannerTab) {
                0 -> {
                    if (tasks.isEmpty()) {
                        EmptyStatePlaceholder(text = "No tasks yet. Create one to organize your productivity!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(tasks) { task ->
                                TaskCardItem(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                    onClick = { selectedTaskForDetail = task }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HABIT LIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NaturalTextSecondary
                            )
                            Button(
                                onClick = { showAddHabitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimaryContainer, contentColor = NaturalPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Habit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (habits.isEmpty()) {
                            EmptyStatePlaceholder(text = "No habits tracked yet. Start small, build consistency!")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(habits) { habit ->
                                    HabitListItem(
                                        habit = habit,
                                        onToggle = { viewModel.toggleHabit(habit) },
                                        onDelete = { viewModel.deleteHabit(habit) }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Eisenhower Matrix Quadrants layout
                    EisenhowerMatrixView(tasks = tasks, onTaskClick = { selectedTaskForDetail = it })
                }
            }
        }
    }

    // Add dialog overlays
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, category, priority, notes, subtasks ->
                viewModel.addTask(title, category, priority, notes, subtasks)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { name, emoji ->
                viewModel.addHabit(name, emoji)
                showAddHabitDialog = false
            }
        )
    }

    if (selectedTaskForDetail != null) {
        TaskDetailDialog(
            task = selectedTaskForDetail!!,
            viewModel = viewModel,
            onDismiss = { selectedTaskForDetail = null }
        )
    }
}

@Composable
fun StatsScreen(viewModel: ProductivityViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()

    val completedTasks = tasks.filter { it.isCompleted }.size
    val totalTasks = tasks.size
    val completionRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0.0f

    val completedHabitsToday = habits.filter { habit ->
        habit.lastCompletedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }.size
    val totalHabits = habits.size

    val totalFocusSessions = focusSessions.size
    val totalFocusMinutes = focusSessions.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Productivity Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NaturalPrimary
            )
            Text(
                text = "Measure consistency and track progress metrics daily.",
                style = MaterialTheme.typography.bodyMedium,
                color = NaturalTextSecondary
            )
        }

        // Big Circular Progress Metric
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NaturalBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TASK COMPLETION RATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            // Background track
                            drawCircle(
                                color = NaturalBorder,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 16.dp.toPx())
                            )
                            // Progress sweep
                            drawArc(
                                color = NaturalPrimary,
                                startAngle = -90f,
                                sweepAngle = completionRatio * 360f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(completionRatio * 100).toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = NaturalPrimary
                            )
                            Text(
                                text = "$completedTasks of $totalTasks",
                                fontSize = 12.sp,
                                color = NaturalTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Grid stats counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus statistics card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, NaturalBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NaturalUrgentBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = NaturalUrgent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Focus Sessions", fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.Bold)
                        Text(text = "$totalFocusSessions", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NaturalText)
                        Text(text = "$totalFocusMinutes mins total", fontSize = 11.sp, color = NaturalTextSecondary)
                    }
                }

                // Habits completion status card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, NaturalBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NaturalPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = NaturalPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Habits Today", fontSize = 11.sp, color = NaturalTextSecondary, fontWeight = FontWeight.Bold)
                        Text(text = "$completedHabitsToday / $totalHabits", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NaturalText)
                        Text(
                            text = if (totalHabits > 0 && completedHabitsToday == totalHabits) "Perfect Day!" else "Keep it going!",
                            fontSize = 11.sp,
                            color = NaturalPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Focus Session History Log
        item {
            Text(
                text = "RECENT FOCUS ACTIVITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NaturalTextSecondary,
                letterSpacing = 1.sp
            )
        }

        if (focusSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No focus history. Link focus sessions to tasks to generate metrics!",
                        fontSize = 13.sp,
                        color = NaturalTextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(focusSessions.take(5)) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NaturalBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(16.dp))
                            Column {
                                Text(text = session.taskTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NaturalText)
                                Text(
                                    text = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(session.timestamp)),
                                    fontSize = 11.sp,
                                    color = NaturalTextSecondary
                                )
                            }
                        }
                        Text(
                            text = "+${session.durationMinutes}m focus",
                            fontSize = 12.sp,
                            color = NaturalPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NaturalPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreScreen(viewModel: ProductivityViewModel) {
    val reflection = viewModel.currentReflection

    var showMorningDialog by remember { mutableStateOf(false) }
    var showEveningDialog by remember { mutableStateOf(false) }
    var expandedSectionIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Rituals & Architectural Guide",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NaturalPrimary
        )
        Text(
            text = "Track your reflection practices and explore system specifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = NaturalTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ritual Cards Section
        Text(
            text = "DAILY PLANNING & REVIEW RITUALS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NaturalTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Morning Planning Card
            Card(
                colors = CardDefaults.cardColors(containerColor = if (reflection?.isMorningCompleted == true) NaturalPrimaryContainer else Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showMorningDialog = true }
                    .border(1.dp, NaturalBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Morning Briefing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NaturalText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (reflection?.isMorningCompleted == true) "Completed" else "Start Briefing",
                        fontSize = 11.sp,
                        color = if (reflection?.isMorningCompleted == true) NaturalPrimary else NaturalTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Evening Reflection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = if (reflection?.isEveningCompleted == true) NaturalPrimaryContainer else Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEveningDialog = true }
                    .border(1.dp, NaturalBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌌", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Evening Reflection", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NaturalText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (reflection?.isEveningCompleted == true) "Completed" else "Start Review",
                        fontSize = 11.sp,
                        color = if (reflection?.isEveningCompleted == true) NaturalPrimary else NaturalTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Product Requirements Document accordion list
        Text(
            text = "PRODUCT REQUIREMENT DOCUMENT & ROADMAP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NaturalTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Accordion item 1: Core Features
        PrdAccordionItem(
            title = "1. Core Features (The Must-Haves)",
            isExpanded = expandedSectionIndex == 1,
            onClick = { expandedSectionIndex = if (expandedSectionIndex == 1) null else 1 }
        ) {
            Text(
                text = """
                    • Smart Task Creation: Due dates, priorities categorized via Eisenhower Matrix (Q1-Q4), descriptive notes, custom categories/tags, and subtasks.
                    • Flexible Reminders: Time-triggered focus states, local system notification pathways, and persistent status indicators.
                    • Habit Tracking: Separate system module to decouple habits from tasks. Streak counting, daily check-in mechanics, and habit history logs.
                    • Calendar Integration: Schema structure prepped for calendar syncing. Exposes clean sync states for third-party calendar connections.
                """.trimIndent(),
                fontSize = 13.sp,
                color = NaturalTextSecondary,
                lineHeight = 18.sp
            )
        }

        // Accordion item 2: Advanced Features
        PrdAccordionItem(
            title = "2. Advanced 'Right Way' Features",
            isExpanded = expandedSectionIndex == 2,
            onClick = { expandedSectionIndex = if (expandedSectionIndex == 2) null else 2 }
        ) {
            Text(
                text = """
                    • Time Blocking & Focus Mode: Built-in 25-minute Pomodoro timer linked to chosen active tasks, saving focus log timestamps directly to local Room DB.
                    • Daily Planning & Review Rituals: Structured Morning Briefings (set daily energy and intentions) and Evening Reflections (review achievements).
                    • AI Advisor suggestions: Call server-side Gemini API with list of active tasks and habits to output immediate suggestions.
                """.trimIndent(),
                fontSize = 13.sp,
                color = NaturalTextSecondary,
                lineHeight = 18.sp
            )
        }

        // Accordion item 3: UI/UX Design
        PrdAccordionItem(
            title = "3. UI/UX Design Principles",
            isExpanded = expandedSectionIndex == 3,
            onClick = { expandedSectionIndex = if (expandedSectionIndex == 3) null else 3 }
        ) {
            Text(
                text = """
                    • Natural Tones theme: Uses calming natural colors (#F7F8F3, #386B1D, #E1E4D5) to minimize cognitive fatigue and task anxiety.
                    • Modern Typography: Pair lightweight display headings with bold, high-contrast status and tag elements.
                    • Positive Gamification: Streak tracking and completed session badges motivate consistent practice without gamified visual noise.
                """.trimIndent(),
                fontSize = 13.sp,
                color = NaturalTextSecondary,
                lineHeight = 18.sp
            )
        }

        // Accordion item 4: System Architecture
        PrdAccordionItem(
            title = "4. Technical Architecture",
            isExpanded = expandedSectionIndex == 4,
            onClick = { expandedSectionIndex = if (expandedSectionIndex == 4) null else 4 }
        ) {
            Text(
                text = """
                    • Mobile Stack: Native Android build using Jetpack Compose and modern Kotlin Coroutines.
                    • Database Architecture: Local SQLite engine powered by Room, declaring separate tables for Tasks, Habits, FocusSessions, and DailyReflections.
                    • Repository Pattern: Complete abstract boundaries separating DAO endpoints from the UI/ViewModel layer.
                    • AI Integration: Direct secure server-side REST API calling Gemini 3.5 Flash for rapid smart suggestion generations.
                """.trimIndent(),
                fontSize = 13.sp,
                color = NaturalTextSecondary,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Morning Ritual dialog wizard
    if (showMorningDialog) {
        var energy by remember { mutableStateOf(reflection?.energyLevel ?: "Medium") }
        var intentions by remember { mutableStateOf(reflection?.morningIntentions ?: "") }

        AlertDialog(
            onDismissRequest = { showMorningDialog = false },
            title = { Text("Morning Planning Briefing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select your daily energy level:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            val isSelected = energy == level
                            Button(
                                onClick = { energy = level },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NaturalPrimary else NaturalContainer,
                                    contentColor = if (isSelected) Color.White else NaturalText
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(level, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Enter your primary focus/intentions:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                    OutlinedTextField(
                        value = intentions,
                        onValueChange = { intentions = it },
                        placeholder = { Text("e.g., Finalize system design...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDailyReflection(
                            energyLevel = energy,
                            morningIntentions = intentions,
                            eveningReflection = reflection?.eveningReflection ?: "",
                            isMorningCompleted = true,
                            isEveningCompleted = reflection?.isEveningCompleted == true
                        )
                        showMorningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                ) {
                    Text("Complete Briefing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMorningDialog = false }) {
                    Text("Cancel", color = NaturalPrimary)
                }
            }
        )
    }

    // Evening Ritual dialog wizard
    if (showEveningDialog) {
        var reflectionText by remember { mutableStateOf(reflection?.eveningReflection ?: "") }

        AlertDialog(
            onDismissRequest = { showEveningDialog = false },
            title = { Text("Evening Review Reflection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reflect on your day, achievements, and lessons:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                    OutlinedTextField(
                        value = reflectionText,
                        onValueChange = { reflectionText = it },
                        placeholder = { Text("e.g., Felt focused on design. Need to drink more water tomorrow...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDailyReflection(
                            energyLevel = reflection?.energyLevel ?: "Medium",
                            morningIntentions = reflection?.morningIntentions ?: "",
                            eveningReflection = reflectionText,
                            isMorningCompleted = reflection?.isMorningCompleted == true,
                            isEveningCompleted = true
                        )
                        showEveningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                ) {
                    Text("Complete Reflection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEveningDialog = false }) {
                    Text("Cancel", color = NaturalPrimary)
                }
            }
        )
    }
}

// --- Component Helpers ---

@Composable
fun FocusSessionCard(viewModel: ProductivityViewModel) {
    val timerText = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        viewModel.timerSecondsRemaining / 60,
        viewModel.timerSecondsRemaining % 60
    )

    val activeTask = viewModel.selectedTaskForFocus

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(NaturalPrimary)
            .padding(20.dp)
    ) {
        // Subtle decorative background circle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CURRENT FOCUS",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = timerText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = activeTask?.title ?: "No Task Selected",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (activeTask != null) "Priority: ${activeTask.priority}" else "Select a task from planner to block time",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (viewModel.isTimerRunning) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer(activeTask)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaturalPrimaryContainer,
                        contentColor = NaturalText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (viewModel.isTimerRunning) "Pause Focus" else "Start Focus Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (viewModel.selectedTaskForFocus != null) {
                    IconButton(
                        onClick = { viewModel.stopTimer() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCardItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val isUrgent = task.priority == "Urgent & Important"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, NaturalBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom high-contrast priority checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 2.dp,
                        color = if (isUrgent) NaturalUrgent else NaturalPrimary,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .background(
                        if (task.isCompleted) (if (isUrgent) NaturalUrgent else NaturalPrimary) else Color.Transparent
                    )
                    .clickable { onToggleComplete() }
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (task.isCompleted) NaturalTextSecondary.copy(alpha = 0.5f) else NaturalText,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.priority} • ${task.category}",
                    fontSize = 11.sp,
                    color = if (task.isCompleted) NaturalTextSecondary.copy(alpha = 0.4f) else NaturalTextSecondary
                )
            }

            if (isUrgent && !task.isCompleted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NaturalUrgentBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "URGENT",
                        color = NaturalUrgent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HabitStreakItem(habit: Habit, onClick: () -> Unit) {
    val isCompletedToday = habit.lastCompletedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isCompletedToday) NaturalPrimaryContainer else NaturalContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
            .border(
                1.dp,
                if (isCompletedToday) NaturalPrimary else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .testTag("habit_card_${habit.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = habit.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = habit.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NaturalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${habit.streak} Day Streak",
                fontSize = 10.sp,
                color = NaturalPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HabitListItem(
    habit: Habit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompletedToday = habit.lastCompletedDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NaturalBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = habit.emoji, fontSize = 24.sp)
                Column {
                    Text(text = habit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "${habit.streak} day streak", fontSize = 12.sp, color = NaturalPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompletedToday) NaturalPrimary else NaturalContainer,
                        contentColor = if (isCompletedToday) Color.White else NaturalText
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = if (isCompletedToday) "Completed" else "Check In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NaturalUrgent.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun PrdAccordionItem(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, NaturalBorder, RoundedCornerShape(12.dp))
            .padding(bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NaturalPrimary)
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NaturalPrimary
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, tint = NaturalTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = NaturalTextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EisenhowerMatrixView(tasks: List<Task>, onTaskClick: (Task) -> Unit) {
    val q1 = tasks.filter { !it.isCompleted && it.priority == "Urgent & Important" }
    val q2 = tasks.filter { !it.isCompleted && it.priority == "Important but Not Urgent" }
    val q3 = tasks.filter { !it.isCompleted && it.priority == "Urgent but Not Important" }
    val q4 = tasks.filter { !it.isCompleted && it.priority == "Not Urgent & Not Important" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "EISENHOWER DECISION MATRIX",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NaturalTextSecondary,
            letterSpacing = 1.sp
        )

        // Q1 and Q2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EisenhowerQuadrant(
                title = "Do First (Q1)",
                subtitle = "Urgent & Important",
                tasks = q1,
                borderColor = NaturalUrgent,
                modifier = Modifier.weight(1f),
                onTaskClick = onTaskClick
            )
            EisenhowerQuadrant(
                title = "Schedule (Q2)",
                subtitle = "Important / Not Urgent",
                tasks = q2,
                borderColor = NaturalPrimary,
                modifier = Modifier.weight(1f),
                onTaskClick = onTaskClick
            )
        }

        // Q3 and Q4
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EisenhowerQuadrant(
                title = "Delegate (Q3)",
                subtitle = "Urgent / Not Important",
                tasks = q3,
                borderColor = Color(0xFFC0A250),
                modifier = Modifier.weight(1f),
                onTaskClick = onTaskClick
            )
            EisenhowerQuadrant(
                title = "Eliminate (Q4)",
                subtitle = "Not Urgent / Not Important",
                tasks = q4,
                borderColor = NaturalTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
fun EisenhowerQuadrant(
    title: String,
    subtitle: String,
    tasks: List<Task>,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onTaskClick: (Task) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .heightIn(min = 180.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NaturalText)
            Text(text = subtitle, fontSize = 10.sp, color = NaturalTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "Clear!",
                    fontSize = 11.sp,
                    color = NaturalTextSecondary.copy(alpha = 0.4f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                tasks.take(3).forEach { task ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NaturalContainer.copy(alpha = 0.5f))
                            .clickable { onTaskClick(task) }
                            .padding(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (tasks.size > 3) {
                    Text(text = "+${tasks.size - 3} more", fontSize = 10.sp, color = NaturalPrimary)
                }
            }
        }
    }
}

// --- Dialog Overlays ---

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") }
    var priority by remember { mutableStateOf("Urgent & Important") }
    
    // Subtask temporary creation fields
    var subtaskInput by remember { mutableStateOf("") }
    val subtasks = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Task Creation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaturalPrimary)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Task Notes / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaturalPrimary)
                )

                // Category Selection dropdown helper
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Work", "Personal", "Health", "Daily Ritual", "Finance").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Priority Selector
                Text("Eisenhower Priority Quadrant:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                listOf(
                    "Urgent & Important",
                    "Important but Not Urgent",
                    "Urgent but Not Important",
                    "Not Urgent & Not Important"
                ).forEach { pri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { priority = pri }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(selected = priority == pri, onClick = { priority = pri })
                        Text(pri, fontSize = 13.sp)
                    }
                }

                // Subtask builder
                Text("Subtasks / Milestones:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = subtaskInput,
                        onValueChange = { subtaskInput = it },
                        placeholder = { Text("Add milestone...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaturalPrimary)
                    )
                    IconButton(
                        onClick = {
                            if (subtaskInput.isNotBlank()) {
                                subtasks.add(subtaskInput.trim())
                                subtaskInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add milestone", tint = NaturalPrimary)
                    }
                }

                subtasks.forEach { sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "- $sub", fontSize = 12.sp, color = NaturalText)
                        IconButton(onClick = { subtasks.remove(sub) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = NaturalUrgent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), category, priority, notes.trim(), subtasks.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary),
                modifier = Modifier.testTag("submit_task_button")
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalPrimary)
            }
        }
    )
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("💧") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaturalPrimary)
                )

                Text("Pick an Emoji:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NaturalTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("💧", "🧘", "📖", "🏃", "🥗", "⏰").forEach { emo ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (emoji == emo) NaturalPrimaryContainer else NaturalContainer)
                                .clickable { emoji = emo },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emo, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), emoji)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
            ) {
                Text("Create Habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalPrimary)
            }
        }
    )
}

@Composable
fun TaskDetailDialog(
    task: Task,
    viewModel: ProductivityViewModel,
    onDismiss: () -> Unit
) {
    val subtasks = task.getSubtasks()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NaturalText)
                Text(text = "Category: ${task.category}", fontSize = 12.sp, color = NaturalTextSecondary)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Priority quadrant label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (task.priority == "Urgent & Important") NaturalUrgentBg else NaturalContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.priority,
                        color = if (task.priority == "Urgent & Important") NaturalUrgent else NaturalText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (task.notes.isNotBlank()) {
                    Text(text = "Notes:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NaturalTextSecondary)
                    Text(text = task.notes, fontSize = 13.sp, color = NaturalText)
                }

                // Subtask interactive check list
                if (subtasks.isNotEmpty()) {
                    Text(text = "Subtasks / Checklists:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NaturalTextSecondary)
                    subtasks.forEachIndexed { index, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSubtaskCompletion(task, index) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = { viewModel.toggleSubtaskCompletion(task, index) },
                                colors = CheckboxDefaults.colors(checkedColor = NaturalPrimary)
                            )
                            Text(
                                text = sub.title,
                                fontSize = 13.sp,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null,
                                color = if (sub.isCompleted) NaturalTextSecondary.copy(alpha = 0.6f) else NaturalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Blocking action button
                Button(
                    onClick = {
                        viewModel.startTimer(task)
                        viewModel.currentTab = 0 // Go back to home to see focus countdown timer
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select for Focus Mode", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.deleteTask(task)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = NaturalUrgent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Task", color = NaturalUrgent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NaturalPrimary)
            }
        }
    )
}
