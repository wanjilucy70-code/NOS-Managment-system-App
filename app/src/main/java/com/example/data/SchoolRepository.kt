package com.example.data

import kotlinx.coroutines.flow.Flow

class SchoolRepository(
    private val userDao: UserDao,
    private val fileRecordDao: FileRecordDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allFiles: Flow<List<FileRecord>> = fileRecordDao.getAllFiles()
    val shelvesFiles: Flow<List<FileRecord>> = fileRecordDao.getShelvesFiles()

    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun deleteUserByEmail(email: String) = userDao.deleteUserByEmail(email)

    fun getUserCountByRole(role: String): Flow<Int> = userDao.getUserCountByRole(role)

    fun getTotalUserCount(): Flow<Int> = userDao.getTotalUserCount()

    fun getFilesByGrade(grade: String): Flow<List<FileRecord>> = fileRecordDao.getFilesByGrade(grade)

    fun getOfficeDispatchesForDriver(driverEmail: String): Flow<List<FileRecord>> = 
        fileRecordDao.getOfficeDispatchesForDriver(driverEmail)

    fun getDriverClocks(driverEmail: String): Flow<List<FileRecord>> = 
        fileRecordDao.getDriverClocks(driverEmail)

    suspend fun insertFile(file: FileRecord) = fileRecordDao.insertFile(file)

    suspend fun deleteFile(file: FileRecord) = fileRecordDao.deleteFile(file)

    suspend fun deleteFileById(id: Int) = fileRecordDao.deleteFileById(id)
}
