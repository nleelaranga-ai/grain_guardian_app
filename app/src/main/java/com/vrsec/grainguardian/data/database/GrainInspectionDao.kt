package com.vrsec.grainguardian.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrainInspectionDao {
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<GrainInspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getInspectionById(id: Long): GrainInspectionEntity?

    @Query("SELECT * FROM inspections ORDER BY timestamp DESC LIMIT 1")
    fun getLatestInspection(): Flow<GrainInspectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: GrainInspectionEntity): Long

    @Delete
    suspend fun deleteInspection(inspection: GrainInspectionEntity)

    @Query("DELETE FROM inspections")
    suspend fun clearAll()
}
