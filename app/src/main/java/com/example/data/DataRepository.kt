package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataRepository(private val countdownDao: CountdownDao, private val settingsDao: SettingsDao) {
    val allCountdowns: Flow<List<Countdown>> = countdownDao.getAllCountdowns()
    val settings: Flow<Settings> = settingsDao.getSettings().map { it ?: Settings() }

    suspend fun getCountdownById(id: Long): Countdown? = countdownDao.getCountdownById(id)
    
    suspend fun insertCountdown(countdown: Countdown): Long = countdownDao.insertCountdown(countdown)
    
    suspend fun deleteCountdownById(id: Long) = countdownDao.deleteCountdownById(id)
    
    suspend fun getSettingsSync(): Settings = settingsDao.getSettingsSync() ?: Settings()
    
    suspend fun insertSettings(settings: Settings) = settingsDao.insertSettings(settings)
}
