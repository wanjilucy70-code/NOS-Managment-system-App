package com.example.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FileRecord
import com.example.data.SchoolRepository
import com.example.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class SchoolViewModel(private val repository: SchoolRepository) : ViewModel() {

    // UI Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.RoleSelection)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedStudentGrade = MutableStateFlow<String?>(null)
    val selectedStudentGrade: StateFlow<String?> = _selectedStudentGrade.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // File Action States
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    // Flow lists
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<FileRecord>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shelvesFiles: StateFlow<List<FileRecord>> = repository.shelvesFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role Stats
    val adminCount: StateFlow<Int> = repository.getUserCountByRole("ADMIN")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val teacherCount: StateFlow<Int> = repository.getUserCountByRole("TEACHER")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val driverCount: StateFlow<Int> = repository.getUserCountByRole("DRIVER")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val officeCount: StateFlow<Int> = repository.getUserCountByRole("OFFICE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val studentCount: StateFlow<Int> = repository.getUserCountByRole("STUDENT")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _authError.value = null
    }

    fun logout() {
        _currentUser.value = null
        _selectedStudentGrade.value = null
        _currentScreen.value = Screen.RoleSelection
        _authError.value = null
    }

    // Role-based Gmail Login & Access Verification
    fun attemptLogin(emailStr: String, role: String, fullName: String = "") {
        val email = emailStr.trim().lowercase()
        if (email.isEmpty()) {
            _authError.value = "Email address is required."
            return
        }
        if (!email.endsWith("@gmail.com")) {
            _authError.value = "Only standard Gmail accounts (@gmail.com) are allowed."
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null) {
                // Verified email! Log in.
                if (user.role == role || (user.role == "ADMIN" && role == "ADMIN")) {
                    _currentUser.value = user
                    _authError.value = null
                    when (role) {
                        "ADMIN" -> navigateTo(Screen.AdminDashboard)
                        "TEACHER" -> navigateTo(Screen.TeacherDashboard)
                        "OFFICE" -> navigateTo(Screen.OfficeDashboard)
                        "DRIVER" -> navigateTo(Screen.DriverDashboard)
                    }
                } else {
                    _authError.value = "This email is registered under role ${user.role}, not $role."
                }
            } else {
                // Denied Access as per instructions
                _authError.value = "Access Denied. Your email is not registered for $role dashboard. Please contact Admin at wanjilucy70@gmail.com."
            }
        }
    }

    // Free Student Entrance
    fun enterAsStudent(fullName: String, grade: String) {
        if (fullName.trim().isEmpty()) {
            _authError.value = "Please enter your name."
            return
        }
        _selectedStudentGrade.value = grade
        val dummyEmail = "student.${fullName.trim().lowercase().replace(" ", "")}@neema.edu"
        
        viewModelScope.launch {
            // Save/Insert student profile dynamically to populate the stats
            val existing = repository.getUserByEmail(dummyEmail)
            if (existing == null) {
                repository.insertUser(
                    User(
                        email = dummyEmail,
                        fullName = fullName,
                        role = "STUDENT"
                    )
                )
            }
            _currentUser.value = User(dummyEmail, fullName, "STUDENT")
            navigateTo(Screen.StudentDashboard)
        }
    }

    // Admin Action: Pre-register an email
    fun preRegisterUser(emailStr: String, fullName: String, role: String) {
        val email = emailStr.trim().lowercase()
        if (email.isEmpty() || fullName.trim().isEmpty()) return
        viewModelScope.launch {
            repository.insertUser(
                User(
                    email = email,
                    fullName = fullName,
                    role = role
                )
            )
        }
    }

    // Admin Action: Revoke an email
    fun revokeUser(email: String) {
        viewModelScope.launch {
            repository.deleteUserByEmail(email)
        }
    }

    // File Upload Handler
    fun handleFileUpload(
        context: Context,
        uri: Uri,
        customFileName: String,
        description: String,
        category: String,
        grade: String?,
        targetDriverEmail: String?
    ) {
        _uploading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileExtension = "dat"
                contentResolver.getType(uri)?.let { mime ->
                    fileExtension = mime.substringAfterLast("/")
                }
                
                val cleanName = if (customFileName.trim().isNotEmpty()) {
                    if (customFileName.contains(".")) customFileName else "$customFileName.$fileExtension"
                } else {
                    "File_${System.currentTimeMillis()}.$fileExtension"
                }

                // Copy file to internal space
                val uploadDir = File(context.filesDir, "uploads")
                if (!uploadDir.exists()) uploadDir.mkdirs()

                val uniqueName = "${System.currentTimeMillis()}_$cleanName"
                val destFile = File(uploadDir, uniqueName)

                var size = 0L
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            size += bytesRead
                        }
                    }
                }

                val currentStaff = _currentUser.value
                val fileRecord = FileRecord(
                    fileName = cleanName,
                    fileDescription = description,
                    category = category,
                    grade = grade,
                    targetDriverEmail = targetDriverEmail,
                    senderEmail = currentStaff?.email ?: "anonymous@gmail.com",
                    senderRole = currentStaff?.role ?: "GUEST",
                    localPath = destFile.absolutePath,
                    fileSize = size
                )

                repository.insertFile(fileRecord)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Successfully uploaded: $cleanName", Toast.LENGTH_SHORT).show()
                    _uploading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    _uploading.value = false
                }
            }
        }
    }

    // Clock In / Clock Out handler for Drivers
    fun handleDriverClockInOut(
        context: Context,
        isClockIn: Boolean,
        uri: Uri?,
        notes: String
    ) {
        _uploading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val driver = _currentUser.value ?: return@launch
                val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val clockType = if (isClockIn) "Clock In" else "Clock Out"
                val fileName = "${driver.fullName} $clockType - $timestampStr.txt"
                
                var localPath: String? = null
                var fileSize = 120L

                if (uri != null) {
                    // Copy selected image / document as proof
                    val uploadDir = File(context.filesDir, "uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs()
                    val destFile = File(uploadDir, "${System.currentTimeMillis()}_proof.jpg")
                    
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(destFile).use { outputStream ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                fileSize += bytesRead
                            }
                        }
                    }
                    localPath = destFile.absolutePath
                }

                val description = if (isClockIn) {
                    "Driver clocked in at $timestampStr. Verification notes: $notes"
                } else {
                    "Driver clocked out at $timestampStr. Verification notes: $notes"
                }

                val clockFile = FileRecord(
                    fileName = fileName,
                    fileDescription = description,
                    category = "CLOCK_IN_OUT",
                    grade = null,
                    targetDriverEmail = null,
                    senderEmail = driver.email,
                    senderRole = "DRIVER",
                    localPath = localPath,
                    fileSize = fileSize,
                    clockInTime = if (isClockIn) timestampStr else null,
                    clockOutTime = if (!isClockIn) timestampStr else null
                )

                repository.insertFile(clockFile)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Successfully verified $clockType!", Toast.LENGTH_SHORT).show()
                    _uploading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                    _uploading.value = false
                }
            }
        }
    }

    // File Download / Export handler
    fun downloadFile(context: Context, fileRecord: FileRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileRecord.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        if (fileRecord.localPath != null) {
                            // Copy actual saved file
                            val sourceFile = File(fileRecord.localPath)
                            if (sourceFile.exists()) {
                                sourceFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            } else {
                                // Fallback content if file was deleted
                                outputStream.write("File content was lost on disk.\nDescription: ${fileRecord.fileDescription}".toByteArray())
                            }
                        } else {
                            // Synthesize file for preloaded curriculumoverviews
                            val docHeader = "========================================================\n" +
                                            "              NEEMA OASIS SCHOOL OFFICIAL PORTAL\n" +
                                            "========================================================\n" +
                                            "File: ${fileRecord.fileName}\n" +
                                            "Category: ${fileRecord.category}\n" +
                                            "Class/Grade: ${fileRecord.grade ?: "Administrative/N/A"}\n" +
                                            "Sender: ${fileRecord.senderEmail} (${fileRecord.senderRole})\n" +
                                            "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(fileRecord.timestamp))}\n" +
                                            "--------------------------------------------------------\n\n" +
                                            "DESCRIPTION:\n" +
                                            "${fileRecord.fileDescription}\n\n" +
                                            "--------------------------------------------------------\n" +
                                            "Designed by Jim-0713244989. Powered by Neema Oasis School Cabinet app.\n" +
                                            "========================================================\n"
                            outputStream.write(docHeader.toByteArray())
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Downloaded to public Downloads: ${fileRecord.fileName}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    throw Exception("Could not open output stream in Downloads directory.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Delete file record
    fun deleteFile(fileRecord: FileRecord, context: Context) {
        viewModelScope.launch {
            repository.deleteFile(fileRecord)
            if (fileRecord.localPath != null) {
                withContext(Dispatchers.IO) {
                    val f = File(fileRecord.localPath)
                    if (f.exists()) f.delete()
                }
            }
            Toast.makeText(context, "Deleted file: ${fileRecord.fileName}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Navigation Screens
sealed class Screen {
    object RoleSelection : Screen()
    object AdminDashboard : Screen()
    object TeacherDashboard : Screen()
    object OfficeDashboard : Screen()
    object DriverDashboard : Screen()
    object StudentDashboard : Screen()
}

class SchoolViewModelFactory(private val repository: SchoolRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchoolViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchoolViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
