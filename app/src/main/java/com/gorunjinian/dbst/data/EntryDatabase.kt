package com.gorunjinian.dbst.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DBT::class, DST::class], version = 3, exportSchema = false)
abstract class EntryDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: EntryDatabase? = null

        fun getDatabase(context: Context): EntryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EntryDatabase::class.java,
                    "entry_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}