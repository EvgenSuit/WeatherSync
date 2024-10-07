package com.weathersync.features.home.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.Suggestions

class Converters {
    @TypeConverter
    fun fromList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toList(data: String?): List<String> {
        return data?.split(",")?.map { it.trim() } ?: emptyList()
    }
}

@Database(entities = [CurrentWeather::class, Suggestions::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CurrentWeatherLocalDB: RoomDatabase() {
    abstract fun currentWeatherDao(): CurrentWeatherDAO
}