package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DecisionLogEntity::class], version = 1, exportSchema = false)
abstract class CricketVisionDatabase : RoomDatabase() {

    abstract fun decisionLogDao(): DecisionLogDao

    companion object {
        @Volatile
        private var INSTANCE: CricketVisionDatabase? = null

        fun getDatabase(context: Context): CricketVisionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketVisionDatabase::class.java,
                    "cricket_vision_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
