package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.data.DataRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        val repo = DataRepository(db.countdownDao(), db.settingsDao())

        setContent {
            MyApplicationTheme {
                WidgetConfigureScreen(
                    repo = repo,
                    onConfigure = { countdownId, viewMode ->
                        WidgetPreferences.saveWidgetPref(this, appWidgetId, countdownId, viewMode)
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        updateAppWidget(this, appWidgetManager, appWidgetId)
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigureScreen(repo: DataRepository, onConfigure: (Long, String) -> Unit) {
    val countdowns by repo.allCountdowns.collectAsState(initial = emptyList())
    var selectedCountdown by remember { mutableStateOf<Countdown?>(null) }
    var selectedViewMode by remember { mutableStateOf("DAYS") }
    val viewModes = listOf("DAYS", "WEEKS", "MONTHS", "YEARS")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Select Countdown:", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(countdowns) { countdown ->
                    val isSelected = countdown == selectedCountdown
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedCountdown = countdown },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF333333) else Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = countdown.title,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                if (countdowns.isEmpty()) {
                    item {
                        Text("No Countdowns Set", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select View Mode:", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                viewModes.forEach { mode ->
                    FilterChip(
                        selected = selectedViewMode == mode,
                        onClick = { selectedViewMode = mode },
                        label = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF121212),
                            labelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    selectedCountdown?.let {
                        onConfigure(it.id, selectedViewMode)
                    }
                },
                enabled = selectedCountdown != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("Save Widget")
            }
        }
    }
}
