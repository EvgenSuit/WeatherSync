package com.weathersync.features.home.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.Suggestions

@Dao
interface CurrentWeatherDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(currentWeather: CurrentWeather)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: Suggestions)

    @Query("SELECT * FROM CurrentWeather")
    suspend fun getWeather(): CurrentWeather?
    @Query("SELECT * FROM Suggestions")
    suspend fun getSuggestions(): Suggestions?
}