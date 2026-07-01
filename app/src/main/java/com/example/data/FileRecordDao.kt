package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileRecordDao {
    @Query("SELECT * FROM file_records ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<FileRecord>>

    @Query("SELECT * FROM file_records WHERE grade = :grade ORDER BY timestamp DESC")
    fun getFilesByGrade(grade: String): Flow<List<FileRecord>>

    @Query("SELECT * FROM file_records WHERE category = 'SHELVES' ORDER BY timestamp DESC")
    fun getShelvesFiles(): Flow<List<FileRecord>>

    @Query("SELECT * FROM file_records WHERE targetDriverEmail = :driverEmail AND senderRole IN ('ADMIN', 'OFFICE') ORDER BY timestamp DESC")
    fun getOfficeDispatchesForDriver(driverEmail: String): Flow<List<FileRecord>>

    @Query("SELECT * FROM file_records WHERE senderEmail = :driverEmail AND category = 'CLOCK_IN_OUT' ORDER BY timestamp DESC")
    fun getDriverClocks(driverEmail: String): Flow<List<FileRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileRecord)

    @Delete
    suspend fun deleteFile(file: FileRecord)

    @Query("DELETE FROM file_records WHERE id = :id")
    suspend fun deleteFileById(id: Int)
}
