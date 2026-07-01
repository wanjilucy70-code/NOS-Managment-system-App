package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.FileRecord
import com.example.data.SchoolRepository
import com.example.data.User
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.Screen
import com.example.viewmodel.SchoolViewModel
import com.example.viewmodel.SchoolViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = SchoolRepository(database.userDao(), database.fileRecordDao())
        val factory = SchoolViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: SchoolViewModel = viewModel(factory = factory)
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val selectedGrade by viewModel.selectedStudentGrade.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Full page scenic background image
                    Image(
                        painter = painterResource(id = R.drawable.img_school_backdrop),
                        contentDescription = "School Background Backdrop",
                        alpha = 0.7f,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(10.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Main App Content
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        topBar = { TopMarqueeTicker() }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                is Screen.RoleSelection -> RoleSelectionScreen(viewModel)
                                is Screen.AdminDashboard -> AdminDashboardScreen(viewModel)
                                is Screen.TeacherDashboard -> TeacherDashboardScreen(viewModel)
                                is Screen.OfficeDashboard -> OfficeDashboardScreen(viewModel)
                                is Screen.DriverDashboard -> DriverDashboardScreen(viewModel)
                                is Screen.StudentDashboard -> StudentDashboardScreen(viewModel, selectedGrade ?: "Play Group")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global school configurations
val GRADES = listOf(
    "Play Group", "PP1", "PP2", "Grade 1", "Grade 2", "Grade 3",
    "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9"
)

// High-contrast Maroon/White definitions
val DeepMaroon = Color(0xFF7F0000)
val DarkMaroon = Color(0xFF450A0A)
val BrightMaroon = Color(0xFFB31414)
val AmberGold = Color(0xFFFFFFFF) // Redefined to pure white for Dark Red and White theme
val CreamWhite = Color(0xFFFFF5F5)

// Category Buttons definitions with specific background colors (Dark Red and Crimson variants)
data class CategoryBtn(val name: String, val categoryKey: String, val color: Color, val icon: ImageVector)

val CategoryButtons = listOf(
    CategoryBtn("Register", "REGISTER", Color(0xFFD32F2F), Icons.Default.AssignmentInd),
    CategoryBtn("Report", "REPORT", Color(0xFFB31414), Icons.Default.BarChart),
    CategoryBtn("Scheme", "SCHEME", Color(0xFF9E1B1B), Icons.Default.MenuBook),
    CategoryBtn("Exams", "EXAMS", Color(0xFF800C0C), Icons.Default.Description),
    CategoryBtn("Homework", "HOMEWORK", Color(0xFFC2185B), Icons.Default.HomeWork),
    CategoryBtn("Record of Work", "RECORD_OF_WORK", Color(0xFF6B0505), Icons.Default.FolderZip),
    CategoryBtn("Timetables", "TIMETABLES", Color(0xFFE53935), Icons.Default.CalendarToday)
)

// 1. Top Marquee Scrolling Ticker
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopMarqueeTicker() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(AmberGold)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✦ DESIGNED BY JIM-0713244989 ✦     ✦ DESIGNED BY JIM-0713244989 ✦     ✦ DESIGNED BY JIM-0713244989 ✦     ✦ DESIGNED BY JIM-0713244989 ✦",
            color = Color(0xFF450A0A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE
            )
        )
    }
}

// 2. Role Entrance / Selection Hub
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(viewModel: SchoolViewModel) {
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var emailInput by remember { mutableStateOf("") }
    var studentNameInput by remember { mutableStateOf("") }
    var studentGradeInput by remember { mutableStateOf("Play Group") }
    var isGradeDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0202)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color.White.copy(0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // School Crest / Logo
                    Image(
                        painter = painterResource(id = R.drawable.img_school_logo),
                        contentDescription = "Neema Oasis School Emblem",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(3.dp, Color.White, RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "NEEMA OASIS SCHOOL",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Academic Cabinet & Transport Hub",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0202)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "Select Portal to Log In:",
                    color = CreamWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Color-coded role selector triggers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleCard(
                    title = "Admin",
                    icon = Icons.Default.AdminPanelSettings,
                    color = Color(0xFFB31414), // Dark Red
                    modifier = Modifier.weight(1f),
                    isSelected = selectedRole == "ADMIN"
                ) {
                    selectedRole = "ADMIN"
                    viewModel.logout()
                }

                RoleCard(
                    title = "Teacher",
                    icon = Icons.Default.School,
                    color = Color(0xFF9E1B1B), // Dark Red Crimson
                    modifier = Modifier.weight(1f),
                    isSelected = selectedRole == "TEACHER"
                ) {
                    selectedRole = "TEACHER"
                    viewModel.logout()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleCard(
                    title = "Drivers",
                    icon = Icons.Default.DirectionsBus,
                    color = Color(0xFF800C0C), // Deep Red Maroon
                    modifier = Modifier.weight(1f),
                    isSelected = selectedRole == "DRIVER"
                ) {
                    selectedRole = "DRIVER"
                    viewModel.logout()
                }

                RoleCard(
                    title = "Office",
                    icon = Icons.Default.Business,
                    color = Color(0xFF6B0505), // Very Dark Red
                    modifier = Modifier.weight(1f),
                    isSelected = selectedRole == "OFFICE"
                ) {
                    selectedRole = "OFFICE"
                    viewModel.logout()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            RoleCard(
                title = "Student (100% Free - No Credentials Required)",
                icon = Icons.Default.Face,
                color = Color(0xFFD32F2F), // Bright Red
                modifier = Modifier.fillMaxWidth(),
                isSelected = selectedRole == "STUDENT"
            ) {
                selectedRole = "STUDENT"
                viewModel.logout()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Login UI Form for chosen roles
        selectedRole?.let { role ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (role == "STUDENT") "Student Quick Access Entrance" else "$role Secured Terminal",
                            color = AmberGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (role == "STUDENT") {
                                "Pick your classroom and type your name for instant free portal access."
                            } else {
                                "Pre-authorization is strictly required. Enter your registered email."
                            },
                            color = CreamWhite.copy(0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (role == "STUDENT") {
                            // Student Name
                            OutlinedTextField(
                                value = studentNameInput,
                                onValueChange = { studentNameInput = it },
                                label = { Text("Student's Full Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = Color.White.copy(0.4f),
                                    focusedLabelColor = AmberGold,
                                    unfocusedLabelColor = Color.White.copy(0.8f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1E0202),
                                    unfocusedContainerColor = Color(0xFF1E0202)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Student Grade Picker Dropdown
                            ExposedDropdownMenuBox(
                                expanded = isGradeDropdownExpanded,
                                onExpandedChange = { isGradeDropdownExpanded = !isGradeDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = studentGradeInput,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Class / Grade") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGradeDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AmberGold,
                                        unfocusedBorderColor = Color.White.copy(0.4f),
                                        focusedLabelColor = AmberGold,
                                        unfocusedLabelColor = Color.White.copy(0.8f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF1E0202),
                                        unfocusedContainerColor = Color(0xFF1E0202)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isGradeDropdownExpanded,
                                    onDismissRequest = { isGradeDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF2C0303))
                                ) {
                                    GRADES.forEach { gradeOption ->
                                        DropdownMenuItem(
                                            text = { Text(gradeOption, color = Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                studentGradeInput = gradeOption
                                                isGradeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.enterAsStudent(studentNameInput, studentGradeInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Enter Student Portal Free ➔", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }

                        } else {
                            // Staff Pre-registered Gmail verification
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Authorized Gmail Address") },
                                placeholder = { Text("name@gmail.com", color = Color.White.copy(0.4f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberGold,
                                    unfocusedBorderColor = Color.White.copy(0.4f),
                                    focusedLabelColor = AmberGold,
                                    unfocusedLabelColor = Color.White.copy(0.8f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1E0202),
                                    unfocusedContainerColor = Color(0xFF1E0202)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.attemptLogin(emailInput, role) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Secure Login / Verify", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Assistant quick triggers for quick developer evaluation
                            Text(
                                text = "Authorized testing credentials (Tap to select):",
                                color = AmberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val testAccounts = when (role) {
                                "ADMIN" -> listOf("wanjilucy70@gmail.com")
                                "TEACHER" -> listOf("teacher.mary@gmail.com")
                                "DRIVER" -> listOf("driver.john@gmail.com", "driver.charles@gmail.com")
                                "OFFICE" -> listOf("office.lucy@gmail.com")
                                else -> emptyList()
                            }

                            testAccounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color.White.copy(0.05f), RoundedCornerShape(6.dp))
                                        .clickable { emailInput = acc }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = "Pre-approved", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(acc, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Display login failure alerts if invalid
                        authError?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x60D32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(err, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color else color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) AmberGold else color.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}


// 3. Admin Secured Terminal Dashboard
@Composable
fun AdminDashboardScreen(viewModel: SchoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Local stats
    val sc by viewModel.studentCount.collectAsStateWithLifecycle()
    val tc by viewModel.teacherCount.collectAsStateWithLifecycle()
    val dc by viewModel.driverCount.collectAsStateWithLifecycle()
    val ac by viewModel.adminCount.collectAsStateWithLifecycle()
    val oc by viewModel.officeCount.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("ACADEMIC") } // ACADEMIC, PRE_REGISTRY, DISPATCHES, SHELVES

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            HeaderDashboardArea(
                title = "Admin Terminal Dashboard",
                email = currentUser?.email ?: "",
                onLogout = { viewModel.logout() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic User Statistics Section
            StatsArea(sc, tc, dc, ac, oc)

            Spacer(modifier = Modifier.height(24.dp))

            // Tab bar switcher
            TabSelectionBar(
                tabs = listOf("ACADEMIC", "PRE_REGISTRY", "DISPATCHES", "SHELVES"),
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        when (activeTab) {
            "ACADEMIC" -> {
                item {
                    CategoryAcademicPanel(
                        viewModel = viewModel,
                        allFiles = allFiles,
                        allowDelete = true
                    )
                }
            }
            "PRE_REGISTRY" -> {
                item {
                    AdminPreRegistryConsole(
                        allUsers = allUsers,
                        onPreRegister = { email, name, role -> viewModel.preRegisterUser(email, name, role) },
                        onRevoke = { email -> viewModel.revokeUser(email) }
                    )
                }
            }
            "DISPATCHES" -> {
                item {
                    AdminOfficeDriversInboxPanel(
                        allFiles = allFiles,
                        viewModel = viewModel
                    )
                }
            }
            "SHELVES" -> {
                item {
                    AdminOfficeShelvesPanel(
                        viewModel = viewModel
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// 4. Academic Teacher Terminal Dashboard
@Composable
fun TeacherDashboardScreen(viewModel: SchoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            HeaderDashboardArea(
                title = "Teacher Console Dashboard",
                email = currentUser?.email ?: "",
                onLogout = { viewModel.logout() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CategoryAcademicPanel(
                viewModel = viewModel,
                allFiles = allFiles,
                allowDelete = true
            )
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// 5. Administration Office Dashboard
@Composable
fun OfficeDashboardScreen(viewModel: SchoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()

    // Office statistics
    val sc by viewModel.studentCount.collectAsStateWithLifecycle()
    val tc by viewModel.teacherCount.collectAsStateWithLifecycle()
    val dc by viewModel.driverCount.collectAsStateWithLifecycle()
    val ac by viewModel.adminCount.collectAsStateWithLifecycle()
    val oc by viewModel.officeCount.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("ACADEMIC") } // ACADEMIC, DISPATCHES, SHELVES

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            HeaderDashboardArea(
                title = "Administration Office Board",
                email = currentUser?.email ?: "",
                onLogout = { viewModel.logout() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Office stats
            StatsArea(sc, tc, dc, ac, oc)

            Spacer(modifier = Modifier.height(20.dp))

            // Tab bar
            TabSelectionBar(
                tabs = listOf("ACADEMIC", "DISPATCHES", "SHELVES"),
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        when (activeTab) {
            "ACADEMIC" -> {
                item {
                    CategoryAcademicPanel(
                        viewModel = viewModel,
                        allFiles = allFiles,
                        allowDelete = true
                    )
                }
            }
            "DISPATCHES" -> {
                item {
                    AdminOfficeDriversInboxPanel(
                        allFiles = allFiles,
                        viewModel = viewModel
                    )
                }
            }
            "SHELVES" -> {
                item {
                    AdminOfficeShelvesPanel(
                        viewModel = viewModel
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// 6. Transport Driver Restricted Dashboard
@Composable
fun DriverDashboardScreen(viewModel: SchoolViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val driverEmail = currentUser?.email ?: ""
    val driverDispatches = allFiles.filter { it.targetDriverEmail == driverEmail && it.senderRole in listOf("ADMIN", "OFFICE") }
    val driverClocks = allFiles.filter { it.senderEmail == driverEmail && it.category == "CLOCK_IN_OUT" }

    var clockNotes by remember { mutableStateOf("") }
    var selectedProofUri by remember { mutableStateOf<Uri?>(null) }
    val proofLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedProofUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            HeaderDashboardArea(
                title = "🚌 Driver Terminal Route",
                email = driverEmail,
                onLogout = { viewModel.logout() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clock In / Clock Out Form Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Odometer / Shift Verification",
                        color = AmberGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Please verify your route shift. Add odometer readings/photos to submit directly to the Admin/Office.",
                        color = CreamWhite.copy(0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clockNotes,
                        onValueChange = { clockNotes = it },
                        label = { Text("Odometer reading / Shift log notes") },
                        placeholder = { Text("e.g. Bus KBA 123, Odometer 45220, route 4 start") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedLabelColor = AmberGold,
                            unfocusedLabelColor = Color.White.copy(0.8f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E0202),
                            unfocusedContainerColor = Color(0xFF1E0202)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { proofLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800C0C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Upload Proof", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedProofUri != null) "Proof Selected ✓" else "Select Log Proof Photo", fontSize = 12.sp)
                        }

                        if (selectedProofUri != null) {
                            IconButton(onClick = { selectedProofUri = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear proof", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.handleDriverClockInOut(context, isClockIn = true, uri = selectedProofUri, notes = clockNotes)
                                clockNotes = ""
                                selectedProofUri = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB31414)), // Bright Maroon
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                        ) {
                            Text("Clock In 🕒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.handleDriverClockInOut(context, isClockIn = false, uri = selectedProofUri, notes = clockNotes)
                                clockNotes = ""
                                selectedProofUri = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B0505)), // Dark Maroon
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                        ) {
                            Text("Clock Out ⏰", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Driver Dashboard Segmented List
            Text(
                "Direct Official Dispatches for you:",
                color = AmberGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (driverDispatches.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "No alerts", tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No direct dispatches or route plans uploaded by the Office for you yet.", color = CreamWhite.copy(0.6f), textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(driverDispatches) { dispatch ->
                FileRecordCard(
                    file = dispatch,
                    showClassTag = false,
                    onDownload = { viewModel.downloadFile(context, dispatch) },
                    onDelete = null,
                    customTag = "DIRECT DISPATCH"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Your Shift Verification Logs History:",
                color = AmberGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (driverClocks.isEmpty()) {
            item {
                Text(
                    "No clocked shifts logged yet.",
                    color = CreamWhite.copy(0.5f),
                    fontSize = 13.sp
                )
            }
        } else {
            items(driverClocks) { clock ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(clock.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(clock.fileDescription, color = CreamWhite.copy(0.7f), fontSize = 12.sp)
                        }
                        if (clock.localPath != null) {
                            IconButton(onClick = { viewModel.downloadFile(context, clock) }) {
                                Icon(Icons.Default.Download, contentDescription = "Download log proof", tint = AmberGold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// 7. Student Dashboard Screen (Grade-specific & Locked)
@Composable
fun StudentDashboardScreen(viewModel: SchoolViewModel, assignedGrade: String) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Filter academic files specifically assigned to student's grade
    val filteredFiles = allFiles.filter { it.grade == assignedGrade && it.category != "SHELVES" && it.category != "CLOCK_IN_OUT" }

    // Dialog trigger for uploading homework/projects
    var isUploadDialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎒 CLASS: ${assignedGrade.uppercase()}",
                            color = AmberGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Student Portal - ${currentUser?.fullName ?: "Guest"}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exit ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Classroom Material & Assignments:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )

                Button(
                    onClick = { isUploadDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload homework", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (filteredFiles.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "No files", tint = Color.White.copy(0.2f), modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No classroom study materials, schemes or homework published yet for $assignedGrade.", color = CreamWhite.copy(0.7f), textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredFiles) { file ->
                FileRecordCard(
                    file = file,
                    showClassTag = false,
                    onDownload = { viewModel.downloadFile(context, file) },
                    onDelete = null
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    // Dynamic Upload Dialog specifically configured for Student Grade
    if (isUploadDialogOpen) {
        UploadFileDialog(
            viewModel = viewModel,
            predefinedCategory = "HOMEWORK",
            predefinedGrade = assignedGrade,
            onDismiss = { isUploadDialogOpen = false }
        )
    }
}


// Shared Common Header Area for Portals
@Composable
fun HeaderDashboardArea(
    title: String,
    email: String,
    onLogout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    color = AmberGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Active user", tint = CreamWhite.copy(0.7f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = email,
                        color = CreamWhite.copy(0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// Common Real-time statistics displaying area
@Composable
fun StatsArea(sc: Int, tc: Int, dc: Int, ac: Int, oc: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SYSTEM USAGE STATISTICS (REAL-TIME)",
                color = AmberGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCardItem("STUDENTS", sc, Color(0xFFD32F2F), modifier = Modifier.weight(1f))
                StatCardItem("TEACHERS", tc, Color(0xFFB31414), modifier = Modifier.weight(1f))
                StatCardItem("DRIVERS", dc, Color(0xFF9E1B1B), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCardItem("ADMINS", ac, Color(0xFF800C0C), modifier = Modifier.weight(1f))
                StatCardItem("OFFICE", oc, Color(0xFF6B0505), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCardItem(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0202)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}


// Shared segment navigation tabs bar
@Composable
fun TabSelectionBar(
    tabs: List<String>,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.04f), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) DeepMaroon else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.replace("_", " "),
                    color = if (isSelected) Color.White else CreamWhite.copy(0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}


// Shared Panel containing academic grade directories & file categorized listings
@Composable
fun CategoryAcademicPanel(
    viewModel: SchoolViewModel,
    allFiles: List<FileRecord>,
    allowDelete: Boolean
) {
    var selectedGradeFilter by remember { mutableStateOf("Play Group") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var isUploadDialogOpen by remember { mutableStateOf(false) }
    var activeFormCategory by remember { mutableStateOf("REGISTER") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Grade Classrooms (Play Group to Grade 9)",
            color = AmberGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal scrolling buttons for school grades selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                horizontalAlignment = Alignment.Start
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GRADES.take(6).forEach { grade ->
                            GradeBadge(grade, selectedGradeFilter == grade) { selectedGradeFilter = grade }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GRADES.drop(6).forEach { grade ->
                            GradeBadge(grade, selectedGradeFilter == grade) { selectedGradeFilter = grade }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Color-Coded Portals / Categories:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Category grids with individual distinct colors & individual custom upload trigger
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryButtons.forEach { btn ->
                val count = allFiles.count { it.grade == selectedGradeFilter && it.category == btn.categoryKey }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (selectedCategoryFilter == btn.categoryKey) btn.color else btn.color.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, btn.color, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCategoryFilter = if (selectedCategoryFilter == btn.categoryKey) null else btn.categoryKey
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(btn.icon, contentDescription = btn.name, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(btn.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$count files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Direct Upload Trigger icon button
                    IconButton(
                        onClick = {
                            activeFormCategory = btn.categoryKey
                            isUploadDialogOpen = true
                        },
                        modifier = Modifier
                            .background(btn.color, RoundedCornerShape(8.dp))
                            .size(45.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Upload ${btn.name}", tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Output listings based on chosen configurations
        Text(
            text = "Class Files Panel: ${selectedGradeFilter.uppercase()}" + if (selectedCategoryFilter != null) " - ${selectedCategoryFilter}" else "",
            color = AmberGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))

        val displayedFiles = allFiles.filter {
            it.grade == selectedGradeFilter &&
            (selectedCategoryFilter == null || it.category == selectedCategoryFilter) &&
            it.category != "SHELVES" && it.category != "CLOCK_IN_OUT"
        }

        if (displayedFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Empty", tint = Color.White.copy(0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No files currently matched in this cabinet sector.", color = CreamWhite.copy(0.7f), fontSize = 12.sp)
                }
            }
        } else {
            displayedFiles.forEach { file ->
                FileRecordCard(
                    file = file,
                    showClassTag = false,
                    onDownload = { viewModel.downloadFile(context, file) },
                    onDelete = if (allowDelete) { { viewModel.deleteFile(file, context) } } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Modal dialogue
    if (isUploadDialogOpen) {
        UploadFileDialog(
            viewModel = viewModel,
            predefinedCategory = activeFormCategory,
            predefinedGrade = selectedGradeFilter,
            onDismiss = { isUploadDialogOpen = false }
        )
    }
}

@Composable
fun GradeBadge(grade: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrightMaroon else Color.White.copy(0.08f))
            .border(1.dp, if (isSelected) AmberGold else Color.White.copy(0.15f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// Administrative Private Shelves Workspace Panel
@Composable
fun AdminOfficeShelvesPanel(viewModel: SchoolViewModel) {
    val shelvesFiles by viewModel.shelvesFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isUploadShelvesOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🗄️ Secure Administrative Shelves",
                    color = AmberGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Files saved here are isolated from student views.",
                    color = CreamWhite.copy(0.6f),
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { isUploadShelvesOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Upload Shelves", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload Shelf", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (shelvesFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "Empty Shelves", tint = Color.White.copy(0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The administrative archives are empty. Save important files here.", color = CreamWhite.copy(0.7f), fontSize = 12.sp)
                }
            }
        } else {
            shelvesFiles.forEach { file ->
                FileRecordCard(
                    file = file,
                    showClassTag = false,
                    onDownload = { viewModel.downloadFile(context, file) },
                    onDelete = { viewModel.deleteFile(file, context) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (isUploadShelvesOpen) {
        UploadFileDialog(
            viewModel = viewModel,
            predefinedCategory = "SHELVES",
            predefinedGrade = null,
            onDismiss = { isUploadShelvesOpen = false }
        )
    }
}


// Administrative Transport / Driver logs Inbox
@Composable
fun AdminOfficeDriversInboxPanel(
    allFiles: List<FileRecord>,
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val clocks = allFiles.filter { it.category == "CLOCK_IN_OUT" }
    val dispatches = allFiles.filter { it.category == "CLOCK_IN_OUT" || (it.targetDriverEmail != null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🚌 Central Drivers' Transmissions Inbox",
            color = AmberGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Track driver clocks, shift proofs, logs and dispatches.",
            color = CreamWhite.copy(0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (dispatches.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PendingActions, contentDescription = "Empty", tint = Color.White.copy(0.2f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No logs or odometer uploads submitted by drivers yet.", color = CreamWhite.copy(0.7f), fontSize = 12.sp)
                }
            }
        } else {
            dispatches.forEach { file ->
                val clockBadge = if (file.clockInTime != null) "CLOCKED IN 🕒" else if (file.clockOutTime != null) "CLOCKED OUT ⏰" else "DISPATCH"
                FileRecordCard(
                    file = file,
                    showClassTag = false,
                    onDownload = { viewModel.downloadFile(context, file) },
                    onDelete = { viewModel.deleteFile(file, context) },
                    customTag = clockBadge
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


// Shared File Item presentation card
@Composable
fun FileRecordCard(
    file: FileRecord,
    showClassTag: Boolean,
    onDownload: () -> Unit,
    onDelete: (() -> Unit)?,
    customTag: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header details
                val tagToShow = customTag ?: file.category
                val badgeColor = when (file.category) {
                    "REGISTER" -> Color(0xFFD32F2F)
                    "REPORT" -> Color(0xFFB31414)
                    "SCHEME" -> Color(0xFF9E1B1B)
                    "EXAMS" -> Color(0xFF800C0C)
                    "HOMEWORK" -> Color(0xFFC2185B)
                    "RECORD_OF_WORK" -> Color(0xFF6B0505)
                    "TIMETABLES" -> Color(0xFFE53935)
                    else -> Color(0xFF450A0A)
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tagToShow.replace("_", " "),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (showClassTag && file.grade != null) {
                    Text(
                        text = "Class: ${file.grade}",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(file.timestamp))
                Text(
                    text = dateStr,
                    color = CreamWhite.copy(0.5f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Name & Description
            Text(
                text = file.fileName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = file.fileDescription,
                color = CreamWhite.copy(0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sender attribution details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = "Sender", tint = CreamWhite.copy(0.4f), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "By: ${file.senderEmail} (${file.senderRole})",
                    color = CreamWhite.copy(0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Get File", tint = Color(0xFF450A0A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get File", color = Color(0xFF450A0A), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


// 8. Admin Security Registrations Console Panel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegistryConsole(
    allUsers: List<User>,
    onPreRegister: (String, String, String) -> Unit,
    onRevoke: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("TEACHER") }
    var isRoleExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🛡️ STAFF & DRIVERS REGISTRATION CONTROL",
                color = AmberGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pre-authorize teachers, office staff and drivers. Unauthorized emails are denied login access.",
                color = CreamWhite.copy(0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Registration Fields
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Pre-authorize Gmail Address") },
                placeholder = { Text("user@gmail.com") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberGold,
                    unfocusedBorderColor = Color.White.copy(0.4f),
                    focusedLabelColor = AmberGold,
                    unfocusedLabelColor = Color.White.copy(0.8f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E0202),
                    unfocusedContainerColor = Color(0xFF1E0202)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Mary Wanjiku") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberGold,
                    unfocusedBorderColor = Color.White.copy(0.4f),
                    focusedLabelColor = AmberGold,
                    unfocusedLabelColor = Color.White.copy(0.8f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E0202),
                    unfocusedContainerColor = Color(0xFF1E0202)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = isRoleExpanded,
                onExpandedChange = { isRoleExpanded = !isRoleExpanded }
            ) {
                OutlinedTextField(
                    value = roleInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign Staff Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = Color.White.copy(0.4f),
                        focusedLabelColor = AmberGold,
                        unfocusedLabelColor = Color.White.copy(0.8f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E0202),
                        unfocusedContainerColor = Color(0xFF1E0202)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = isRoleExpanded,
                    onDismissRequest = { isRoleExpanded = false },
                    modifier = Modifier.background(Color(0xFF2C0303))
                ) {
                    listOf("ADMIN", "TEACHER", "OFFICE", "DRIVER").forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role, color = Color.White, fontWeight = FontWeight.Bold) },
                            onClick = {
                                roleInput = role
                                isRoleExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (emailInput.trim().isNotEmpty() && nameInput.trim().isNotEmpty()) {
                        onPreRegister(emailInput, nameInput, roleInput)
                        emailInput = ""
                        nameInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Authorize New Account", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Authorized Users Registry:",
                color = AmberGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            allUsers.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(user.email, color = CreamWhite.copy(0.6f), fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(Color.White.copy(0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(user.role, color = AmberGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Keep a check so primary admin cannot accidentally delete themselves
                    if (user.email != "wanjilucy70@gmail.com") {
                        IconButton(onClick = { onRevoke(user.email) }) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = "Revoke", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }
        }
    }
}


// Shared Upload Dialog Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadFileDialog(
    viewModel: SchoolViewModel,
    predefinedCategory: String,
    predefinedGrade: String?, // Null for Shelves or general dispatches
    onDismiss: () -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var fileDescriptionInput by remember { mutableStateOf("") }
    var targetGradeInput by remember { mutableStateOf(predefinedGrade ?: "Play Group") }
    var isGradeDropdownExpanded by remember { mutableStateOf(false) }

    // Driver target tag for dispatches
    var targetDriverEmailInput by remember { mutableStateOf<String?>(null) }
    var isDriverDropdownExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        uri?.let {
            // Pick default original file name
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        selectedFileName = c.getString(nameIndex)
                    }
                }
            }
        }
    }

    val availableDrivers = allUsers.filter { it.role == "DRIVER" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0303)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AmberGold, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Publish ${predefinedCategory.replace("_", " ")} Material",
                    color = AmberGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightMaroon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Select File")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Any File / Photo", fontWeight = FontWeight.Bold)
                }

                if (selectedFileUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selected: $selectedFileName",
                        color = Color.Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Display Name input
                OutlinedTextField(
                    value = selectedFileName,
                    onValueChange = { selectedFileName = it },
                    label = { Text("Display File Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = Color.White.copy(0.4f),
                        focusedLabelColor = AmberGold,
                        unfocusedLabelColor = Color.White.copy(0.8f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E0202),
                        unfocusedContainerColor = Color(0xFF1E0202)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description Input
                OutlinedTextField(
                    value = fileDescriptionInput,
                    onValueChange = { fileDescriptionInput = it },
                    label = { Text("File Description / Instructions") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = Color.White.copy(0.4f),
                        focusedLabelColor = AmberGold,
                        unfocusedLabelColor = Color.White.copy(0.8f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E0202),
                        unfocusedContainerColor = Color(0xFF1E0202)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Render Class selector only if it is NOT private shelves
                if (predefinedCategory != "SHELVES") {
                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = isGradeDropdownExpanded,
                        onExpandedChange = { isGradeDropdownExpanded = !isGradeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetGradeInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Class / Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGradeDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = Color.White.copy(0.4f),
                                focusedLabelColor = AmberGold,
                                unfocusedLabelColor = Color.White.copy(0.8f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E0202),
                                unfocusedContainerColor = Color(0xFF1E0202)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isGradeDropdownExpanded,
                            onDismissRequest = { isGradeDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF2C0303))
                        ) {
                            GRADES.forEach { gradeOption ->
                                DropdownMenuItem(
                                    text = { Text(gradeOption, color = Color.White, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        targetGradeInput = gradeOption
                                        isGradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Drivers selector trigger
                    ExposedDropdownMenuBox(
                        expanded = isDriverDropdownExpanded,
                        onExpandedChange = { isDriverDropdownExpanded = !isDriverDropdownExpanded }
                    ) {
                        val displayDriver = availableDrivers.find { it.email == targetDriverEmailInput }?.fullName ?: "General (No driver tagged)"
                        OutlinedTextField(
                            value = displayDriver,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tag Specific Driver (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDriverDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberGold,
                                unfocusedBorderColor = Color.White.copy(0.4f),
                                focusedLabelColor = AmberGold,
                                unfocusedLabelColor = Color.White.copy(0.8f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E0202),
                                unfocusedContainerColor = Color(0xFF1E0202)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDriverDropdownExpanded,
                            onDismissRequest = { isDriverDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF2C0303))
                        ) {
                            DropdownMenuItem(
                                text = { Text("General (No driver tagged)", color = Color.White) },
                                onClick = {
                                    targetDriverEmailInput = null
                                    isDriverDropdownExpanded = false
                                }
                            )

                            availableDrivers.forEach { driver ->
                                DropdownMenuItem(
                                    text = { Text("${driver.fullName} (${driver.email})", color = Color.White) },
                                    onClick = {
                                        targetDriverEmailInput = driver.email
                                        isDriverDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = borderStroke(1.dp, Color.White.copy(0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (selectedFileUri != null && selectedFileName.trim().isNotEmpty()) {
                                viewModel.handleFileUpload(
                                    context = context,
                                    uri = selectedFileUri!!,
                                    customFileName = selectedFileName,
                                    description = fileDescriptionInput,
                                    category = predefinedCategory,
                                    grade = if (predefinedCategory == "SHELVES") null else targetGradeInput,
                                    targetDriverEmail = targetDriverEmailInput
                                )
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Please select a file first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Confirm Upload", color = Color(0xFF450A0A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Utility extension helper
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
