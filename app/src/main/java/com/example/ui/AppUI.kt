package com.example.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.data.DataRepository
import com.example.data.Settings
import com.example.widget.CountdownWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

val PresetColors = listOf(
    Color(0xFFF44336), // Red
    Color(0xFFFF9800), // Orange
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF4CAF50), // Green
    Color(0xFF009688), // Teal
    Color(0xFF00BCD4), // Cyan
    Color(0xFF448AFF), // Blue
    Color(0xFF3F51B5), // Indigo
    Color(0xFF9C27B0), // Purple
    Color(0xFFE91E63)  // Pink
)

fun getFormattedDate(timeInMillis: Long): String {
    val cal = Calendar.getInstance().apply { setTimeInMillis(timeInMillis) }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    val year = cal.get(Calendar.YEAR)
    
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$month $day$suffix, $year"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(repo: DataRepository, initialCountdownId: Long? = null) {
    var showEditDialog by remember { mutableStateOf(initialCountdownId != null) }
    var editCountdownId by remember { mutableStateOf<Long?>(initialCountdownId) }
    var showSettings by remember { mutableStateOf(false) }
    
    val countdowns by repo.allCountdowns.collectAsState(initial = emptyList())
    val settings by repo.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var deletedCountdown by remember { mutableStateOf<Countdown?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var countdownToDelete by remember { mutableStateOf<Countdown?>(null) }

    fun updateWidgets() {
        val intent = Intent(context, CountdownWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, CountdownWidgetProvider::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Countdowns") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editCountdownId = null
                    showEditDialog = true 
                },
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Countdown")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        if (countdowns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No Countdowns Set", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(countdowns, key = { it.id }) { countdown ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                countdownToDelete = countdown
                                showDeleteConfirm = true
                            }
                            false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = Color.Red
                            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true
                    ) {
                        CountdownItem(
                            countdown = countdown,
                            onClick = { 
                                editCountdownId = countdown.id
                                showEditDialog = true 
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && countdownToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Countdown", color = Color.White) },
            text = { Text("Are you sure you want to delete '${countdownToDelete?.title}'?", color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = countdownToDelete!!
                        showDeleteConfirm = false
                        scope.launch {
                            repo.deleteCountdownById(c.id)
                            updateWidgets()
                            deletedCountdown = c
                            val result = snackbarHostState.showSnackbar(
                                message = "Countdown deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                deletedCountdown?.let { repo.insertCountdown(it) }
                                updateWidgets()
                            }
                        }
                    }
                ) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = Color.White) }
            },
            containerColor = Color(0xFF121212)
        )
    }

    if (showEditDialog) {
        var existingCountdown by remember { mutableStateOf<Countdown?>(null) }
        LaunchedEffect(editCountdownId) {
            if (editCountdownId != null) {
                existingCountdown = repo.getCountdownById(editCountdownId!!)
            }
        }
        
        if (editCountdownId == null || existingCountdown != null) {
            EditCountdownDialog(
                countdown = existingCountdown,
                existingColors = countdowns.map { Color(it.color) },
                settings = settings,
                onDismiss = { showEditDialog = false },
                onSave = { newCountdown ->
                    scope.launch {
                        repo.insertCountdown(newCountdown)
                        updateWidgets()
                        showEditDialog = false
                    }
                }
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onSave = { newSettings ->
                scope.launch {
                    repo.insertSettings(newSettings)
                    updateWidgets()
                    showSettings = false
                }
            }
        )
    }
}

fun getLocalMidnightFromUtc(utcDate: Long): Long {
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDate }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun getDaysLeft(targetDate: Long): Long {
    val todayLocal = Calendar.getInstance()
    val todayUtc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, todayLocal.get(Calendar.YEAR))
        set(Calendar.MONTH, todayLocal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, todayLocal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return max(0L, TimeUnit.MILLISECONDS.toDays(targetDate - todayUtc.timeInMillis) - 1L)
}

@Composable
fun CountdownItem(countdown: Countdown, onClick: () -> Unit) {
    val daysLeft = getDaysLeft(countdown.targetDate)
    
    val startStr = getFormattedDate(countdown.startDate)
    val targetStr = getFormattedDate(countdown.targetDate)
    val cColor = Color(countdown.color)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(countdown.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(startStr, color = Color.LightGray, fontSize = 14.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "to", tint = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp).size(16.dp))
                Text(targetStr, color = Color.LightGray, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val years = daysLeft / 365f
            val months = daysLeft / 30f
            val weeks = daysLeft / 7f
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(String.format(Locale.getDefault(), "%.1f", years), "Years", cColor)
                StatItem(String.format(Locale.getDefault(), "%.1f", months), "Months", cColor)
                StatItem(String.format(Locale.getDefault(), "%.1f", weeks), "Weeks", cColor)
                StatItem(daysLeft.toString(), "Days", cColor)
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}
