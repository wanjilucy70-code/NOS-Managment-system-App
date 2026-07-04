package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val fullName: String,
    val role: String, // "ADMIN", "TEACHER", "OFFICE", "DRIVER", "STUDENT"
    val isPreRegistered: Boolean = true,
    val studentGrade: String? = null,
    val loginPin: String? = null,
    val isLoggedIn: Boolean = false
)
