package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, FileRecord::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun fileRecordDao(): FileRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neema_oasis_school_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.userDao(), database.fileRecordDao())
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    // Always make sure Admin email is registered so they never get locked out
                    database.userDao().insertUser(
                        User(
                            email = "wanjilucy70@gmail.com",
                            fullName = "Jim (Admin)",
                            role = "ADMIN"
                        )
                    )
                    // Also seed some test drivers and teachers for evaluation
                    database.userDao().insertUser(
                        User(
                            email = "driver.john@gmail.com",
                            fullName = "John Doe (Driver)",
                            role = "DRIVER"
                        )
                    )
                    database.userDao().insertUser(
                        User(
                            email = "driver.charles@gmail.com",
                            fullName = "Charles Njogu (Driver)",
                            role = "DRIVER"
                        )
                    )
                    database.userDao().insertUser(
                        User(
                            email = "teacher.mary@gmail.com",
                            fullName = "Mary Wanjiku (Teacher)",
                            role = "TEACHER"
                        )
                    )
                    database.userDao().insertUser(
                        User(
                            email = "office.lucy@gmail.com",
                            fullName = "Lucy Wambui (Office Staff)",
                            role = "OFFICE"
                        )
                    )
                }
            }
        }

        suspend fun populateDatabase(userDao: UserDao, fileDao: FileRecordDao) {
            // Primary seeding
            userDao.insertUser(
                User(
                    email = "wanjilucy70@gmail.com",
                    fullName = "Jim (Admin)",
                    role = "ADMIN"
                )
            )
            userDao.insertUser(
                User(
                    email = "driver.john@gmail.com",
                    fullName = "John Doe (Driver)",
                    role = "DRIVER"
                )
            )
            userDao.insertUser(
                User(
                    email = "driver.charles@gmail.com",
                    fullName = "Charles Njogu (Driver)",
                    role = "DRIVER"
                )
            )
            userDao.insertUser(
                User(
                    email = "teacher.mary@gmail.com",
                    fullName = "Mary Wanjiku (Teacher)",
                    role = "TEACHER"
                )
            )
            userDao.insertUser(
                User(
                    email = "office.lucy@gmail.com",
                    fullName = "Lucy Wambui (Office Staff)",
                    role = "OFFICE"
                )
            )

            // Seed a few initial syllabus or files for school grades to show it is fully working!
            val grades = listOf("Play Group", "PP1", "PP2", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
            for (grade in grades) {
                fileDao.insertFile(
                    FileRecord(
                        fileName = "$grade Curriculum Overview.txt",
                        fileDescription = "The official academic syllabus, schedule and learning guide for $grade.",
                        category = "SCHEME",
                        grade = grade,
                        targetDriverEmail = null,
                        senderEmail = "office.lucy@gmail.com",
                        senderRole = "OFFICE",
                        localPath = null,
                        fileSize = 1024
                    )
                )
                fileDao.insertFile(
                    FileRecord(
                        fileName = "$grade Homework Assignment 1.txt",
                        fileDescription = "Opening term homework assignments and study notes for all $grade students.",
                        category = "HOMEWORK",
                        grade = grade,
                        targetDriverEmail = null,
                        senderEmail = "teacher.mary@gmail.com",
                        senderRole = "TEACHER",
                        localPath = null,
                        fileSize = 512
                    )
                )
            }
        }
    }
}
