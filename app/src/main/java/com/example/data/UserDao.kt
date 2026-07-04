package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE role = 'STUDENT' AND loginPin = :loginPin LIMIT 1")
    suspend fun getStudentByPin(loginPin: String): User?

    @Query("SELECT * FROM users WHERE role = 'STUDENT' AND fullName = :fullName AND loginPin = :loginPin LIMIT 1")
    suspend fun getStudentByNameAndPin(fullName: String, loginPin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun resetAllSessions()

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    fun getUserCountByRole(role: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUserCount(): Flow<Int>
}
