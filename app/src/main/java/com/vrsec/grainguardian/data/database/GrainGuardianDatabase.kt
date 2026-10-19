package com.vrsec.grainguardian.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

@Database(entities = [GrainInspectionEntity::class], version = 2, exportSchema = false)
abstract class GrainGuardianDatabase : RoomDatabase() {
    abstract fun inspectionDao(): GrainInspectionDao

    companion object {
        @Volatile
        private var INSTANCE: GrainGuardianDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns with safe defaults
                database.execSQL("ALTER TABLE inspections ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE inspections ADD COLUMN deviceId TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE inspections ADD COLUMN sessionId TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE inspections ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL'")
                database.execSQL("ALTER TABLE inspections ADD COLUMN source TEXT NOT NULL DEFAULT 'REAL'")
                database.execSQL("ALTER TABLE inspections ADD COLUMN ruleVersion TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE inspections ADD COLUMN statusCode TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE inspections ADD COLUMN reasonCode TEXT DEFAULT NULL")

                // Backfill existing rows with random UUIDs to ensure uniqueness
                val cursor = database.query("SELECT id FROM inspections")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val randomUuid = UUID.randomUUID().toString()
                    database.execSQL("UPDATE inspections SET uuid = ? WHERE id = ?", arrayOf(randomUuid, id))
                }
                cursor.close()

                // Create unique index on uuid
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inspections_uuid ON inspections(uuid)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope? = null): GrainGuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrainGuardianDatabase::class.java,
                    "grainguardian_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
