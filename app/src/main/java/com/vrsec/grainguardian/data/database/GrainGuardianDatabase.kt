package com.vrsec.grainguardian.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.CoroutineScope

@Database(entities = [GrainInspectionEntity::class], version = 1, exportSchema = false)
abstract class GrainGuardianDatabase : RoomDatabase() {
    abstract fun inspectionDao(): GrainInspectionDao

    companion object {
        @Volatile
        private var INSTANCE: GrainGuardianDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GrainGuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrainGuardianDatabase::class.java,
                    "grainguardian_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
