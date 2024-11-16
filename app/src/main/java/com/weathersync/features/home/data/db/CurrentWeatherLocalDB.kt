package com.weathersync.features.home.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.data.Suggestions

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        val listType = object: TypeToken<List<String>>() {}.type
        return gson.fromJson(data, listType)
    }
}

@Database(entities = [CurrentWeather::class, Suggestions::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CurrentWeatherLocalDB: RoomDatabase() {
    abstract fun currentWeatherDao(): CurrentWeatherDAO
}