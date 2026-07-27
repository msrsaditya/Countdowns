package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Countdown
import com.example.data.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCountdownDialog(
    countdown: Countdown?,
    existingColors: List<Color>,
    settings: Settings,
    onDismiss: () -> Unit,
    onSave: (Countdown) -> Unit
) {
    var title by remember { mutableStateOf(countdown?.title ?: "") }
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    var startDate by remember { mutableStateOf(countdown?.startDate ?: today) }
    var targetDate by remember { mutableStateOf(countdown?.targetDate ?: (today + 86400000L * 30)) }
    
    val availableColors = PresetColors.filter { it !in existingColors }
    val initialColor = countdown?.color?.let { Color(it) } ?: Color(0xFF448AFF)
    var selectedColor by remember { mutableStateOf(initialColor) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    
    val datePickerStateStart = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val datePickerStateTarget = rememberDatePickerState(initialSelectedDateMillis = targetDate)
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Text(if (countdown == null) "New Countdown" else "Edit Countdown", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.weight(1f).padding(end = 8.dp).clickable { showStartDatePicker = true }) {
                        OutlinedTextField(
                            value = getFormattedDate(startDate),
                            onValueChange = { },
                            readOnly = true,
                            enabled = true,
                            label = { Text("Start Date") },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { showStartDatePicker = true })
                    }
                    Box(modifier = Modifier.weight(1f).padding(start = 8.dp).clickable { showTargetDatePicker = true }) {
                        OutlinedTextField(
                            value = getFormattedDate(targetDate),
                            onValueChange = { },
                            readOnly = true,
                            enabled = true,
                            label = { Text("Target Date") },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { showTargetDatePicker = true })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Color", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = if (selectedColor == color) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Widget Preview", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    WidgetPreviewCanvas(startDate, targetDate, selectedColor, settings.widgetSize, settings.textSize)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newC = Countdown(
                                id = countdown?.id ?: 0L,
                                title = title,
                                startDate = startDate,
                                targetDate = targetDate,
                                color = selectedColor.toArgb()
                            )
                            onSave(newC)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerStateStart.selectedDateMillis?.let { startDate = it }
                    showStartDatePicker = false 
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateStart)
        }
    }
    if (showTargetDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showTargetDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerStateTarget.selectedDateMillis?.let { targetDate = it }
                    showTargetDatePicker = false 
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateTarget)
        }
    }
}

@Composable
fun WidgetPreviewCanvas(startDate: Long, targetDate: Long, color: Color, widgetSizePercent: Float, textSizePercent: Float) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val localStart = getLocalMidnightFromUtc(startDate)
    val localTarget = getLocalMidnightFromUtc(targetDate)
    
    val totalMillis = (localTarget - localStart).toDouble()
    val passedMillis = (now - localStart).toDouble()
    val progress = if (totalMillis > 0.0) (passedMillis / totalMillis).coerceIn(0.0, 1.0).toFloat() else 1f
    
    val daysLeft = getDaysLeft(targetDate)
    
    val baseSize = 150.dp
    val scaledSize = baseSize * (0.5f + (widgetSizePercent / 100f) * 0.5f)
    
    Box(modifier = Modifier.size(scaledSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.width * 0.08f
            val inset = strokeW / 2f
            val canvasSize = Size(size.width - strokeW, size.height - strokeW)
            
            drawArc(
                color = Color(0xFF333333),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = canvasSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            val sweepAngle = 360f * progress
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = canvasSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }
        val scaledTextSize = 24.sp * (0.5f + (textSizePercent / 100f) * 1.0f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(daysLeft.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = scaledTextSize)
            Text("days", color = Color.LightGray, fontSize = scaledTextSize * 0.4f)
        }
    }
}

@Composable
fun SettingsDialog(settings: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    var widgetSize by remember { mutableFloatStateOf(settings.widgetSize) }
    var textSize by remember { mutableFloatStateOf(settings.textSize) }
    
    val snapToQuarter: (Float) -> Float = { value ->
        val nearest = (value / 25f).roundToInt() * 25f
        if (kotlin.math.abs(value - nearest) < 5f) nearest else value
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Widget Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Widget Size: ${widgetSize.toInt()}%", color = Color.White)
                Slider(
                    value = widgetSize,
                    onValueChange = { widgetSize = snapToQuarter(it) },
                    valueRange = 0f..100f
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Text Size: ${textSize.toInt()}%", color = Color.White)
                Slider(
                    value = textSize,
                    onValueChange = { textSize = snapToQuarter(it) },
                    valueRange = 0f..100f
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    WidgetPreviewCanvas(
                        startDate = System.currentTimeMillis() - 86400000L * 5,
                        targetDate = System.currentTimeMillis() + 86400000L * 25,
                        color = Color(0xFF448AFF),
                        widgetSizePercent = widgetSize,
                        textSizePercent = textSize
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(Settings(1, widgetSize, textSize)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
