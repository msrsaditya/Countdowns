package com.example.widget

import android.content.Context

object WidgetPreferences {
    private const val PREFS_NAME = "com.example.widget.CountdownWidget"
    private const val PREF_PREFIX_KEY = "appwidget_"
    private const val VIEW_MODE_KEY = "_viewmode"

    fun saveWidgetPref(context: Context, appWidgetId: Int, countdownId: Long, viewMode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, countdownId)
        prefs.putString(PREF_PREFIX_KEY + appWidgetId + VIEW_MODE_KEY, viewMode)
        prefs.apply()
    }

    fun loadCountdownIdPref(context: Context, appWidgetId: Int): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1L)
    }

    fun loadViewModePref(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0)
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId + VIEW_MODE_KEY, "DAYS") ?: "DAYS"
    }

    fun deleteWidgetPref(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
        prefs.remove(PREF_PREFIX_KEY + appWidgetId)
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + VIEW_MODE_KEY)
        prefs.apply()
    }
}
