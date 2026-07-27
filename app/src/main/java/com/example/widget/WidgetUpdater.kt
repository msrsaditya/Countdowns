package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val countdownId = WidgetPreferences.loadCountdownIdPref(context, appWidgetId)
    if (countdownId == -1L) return
    val viewMode = WidgetPreferences.loadViewModePref(context, appWidgetId)

    CoroutineScope(Dispatchers.IO).launch {
        val database = AppDatabase.getDatabase(context)
        val repo = DataRepository(database.countdownDao(), database.settingsDao())
        val countdown = repo.getCountdownById(countdownId) ?: return@launch
        val settings = repo.getSettingsSync()

        val bitmap = drawWidgetBitmap(context, countdown, viewMode, settings.widgetSize, settings.textSize)
        
        val views = RemoteViews(context.packageName, com.example.R.layout.widget_layout)
        views.setImageViewBitmap(com.example.R.id.widget_image, bitmap)

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("countdown_id", countdownId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(com.example.R.id.widget_image, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

fun drawWidgetBitmap(
    context: Context,
    countdown: Countdown,
    viewMode: String,
    widgetSizePercent: Float,
    textSizePercent: Float
): Bitmap {
    val size = 400
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val scaledSize = 200f + (widgetSizePercent / 100f) * 200f
    val padding = (400f - scaledSize) / 2f
    val strokeWidth = scaledSize * 0.08f

    val rect = RectF(
        padding + strokeWidth / 2,
        padding + strokeWidth / 2,
        size - padding - strokeWidth / 2,
        size - padding - strokeWidth / 2
    )

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        color = Color.parseColor("#333333") // Dark gray track
    }

    val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = countdown.color
    }

    canvas.drawArc(rect, 0f, 360f, false, bgPaint)

    val now = System.currentTimeMillis()
    val localStart = com.example.ui.getLocalMidnightFromUtc(countdown.startDate)
    val localTarget = com.example.ui.getLocalMidnightFromUtc(countdown.targetDate)
    
    val totalMillis = (localTarget - localStart).toDouble()
    val passedMillis = (now - localStart).toDouble()
    
    val daysLeft = com.example.ui.getDaysLeft(countdown.targetDate)

    val progress = if (totalMillis > 0.0) (passedMillis / totalMillis).coerceIn(0.0, 1.0).toFloat() else 1f
    val sweepAngle = 360f * progress
    if (sweepAngle > 0f) {
        canvas.drawArc(rect, -90f, sweepAngle, false, fgPaint)
    }

    val (value, unit) = when (viewMode) {
        "YEARS" -> {
            val years = daysLeft / 365f
            String.format(java.util.Locale.getDefault(), "%.1f", years) to "years"
        }
        "MONTHS" -> {
            val months = daysLeft / 30f
            String.format(java.util.Locale.getDefault(), "%.1f", months) to "months"
        }
        "WEEKS" -> {
            val weeks = daysLeft / 7f
            String.format(java.util.Locale.getDefault(), "%.1f", weeks) to "weeks"
        }
        else -> {
            daysLeft.toString() to "days"
        }
    }

    val baseTextSize = scaledSize * 0.25f
    val scaledTextSize = baseTextSize * (0.5f + (textSizePercent / 100f) * 1.0f)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = scaledTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = scaledTextSize * 0.4f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    val textY = size / 2f - ((textPaint.descent() + textPaint.ascent()) / 2) - (scaledTextSize * 0.2f)
    canvas.drawText(value, size / 2f, textY, textPaint)
    canvas.drawText(unit, size / 2f, textY + scaledTextSize * 0.7f, unitPaint)

    return bitmap
}
