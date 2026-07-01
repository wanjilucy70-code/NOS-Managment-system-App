package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_records")
data class FileRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileDescription: String,
    val category: String, // "REGISTER", "REPORT", "SCHEME", "EXAMS", "HOMEWORK", "RECORD_OF_WORK", "TIMETABLES", "SHELVES", "CLOCK_IN_OUT"
    val grade: String?, // "Play Group" to "Grade 9", or null
    val targetDriverEmail: String?, // Assigned driver email for dispatches from Office, or null
    val senderEmail: String,
    val senderRole: String, // "ADMIN", "TEACHER", "OFFICE", "DRIVER"
    val localPath: String?, // Absolute file path on disk
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val clockInTime: String? = null,
    val clockOutTime: String? = null
)
