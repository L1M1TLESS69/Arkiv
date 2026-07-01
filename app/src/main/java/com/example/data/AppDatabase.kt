package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, CategoryEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
