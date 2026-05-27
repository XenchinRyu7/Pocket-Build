package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BuildHistoryEntity
import com.example.data.ProjectEntity
import com.example.utils.ProjectManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PocketBuildApp(viewModel: PocketBuildViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val autoSign by viewModel.autoSign.collectAsStateWithLifecycle()
    val outputFolder by viewModel.outputFolder.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()

    var showClearCacheConfirm by remember { mutableStateOf(false) }

    // Zip File Picker
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "imported_project.zip"
            viewModel.importProjectZip(uri, fileName)
            Toast.makeText(context, "Mengekstrak project: $fileName", Toast.LENGTH_SHORT).show()
            viewModel.setActiveTab(1) // Go to projects tab
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            FloatingBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppHeader()
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 76.dp) // Leave elegant space for the floating nav bar
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { tab ->
                    when (tab) {
                        0 -> HomeTabScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding,
                            onImportClick = { zipPickerLauncher.launch("*/*") }
                        )
                        1 -> ProjectsTabScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding,
                            onImportClick = { zipPickerLauncher.launch("*/*") }
                        )
                        2 -> BuildTabScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding
                        )
                        3 -> EngineTabScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding
                        )
                        4 -> SettingsTabScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding,
                            onClearCacheClick = { showClearCacheConfirm = true }
                        )
                    }
                }
            }
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Hapus Semua Data?") },
            text = { Text("Tindakan ini akan menghapus semua project yang di-import, riwayat build, dan file APK keluaran secara permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheConfirm = false
                        Toast.makeText(context, "Selesai membersihkan cache sandbox!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- POLISHED TOP HEADER COMPONENT ---
@Composable
fun AppHeader() {
    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PocketBuild",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "MOBILE SDK V34.0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    letterSpacing = 1.2.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bell / Notification active badge item
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifikasi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Profile Avatar Gradient
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF60A5FA),
                                    Color(0xFF2563EB),
                                    Color(0xFF3B82F6)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                )
            }
        }
    }
}

// --- FLOATING BOTTOM NAVIGATION BAR ---
@Composable
fun FloatingBottomNavigation(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", Icons.Default.Home, 0),
        NavigationItem("Projects", Icons.Default.List, 1),
        NavigationItem("Build", Icons.Default.PlayArrow, 2),
        NavigationItem("Engine", Icons.Default.Info, 3),
        NavigationItem("Settings", Icons.Default.Settings, 4)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), clip = false)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = activeTab == item.index
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null, // Disable ugly standard flat ripples
                                onClick = { onTabSelected(item.index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val index: Int)

// --- HOME TAB SCREEN ---
@Composable
fun HomeTabScreen(
    viewModel: PocketBuildViewModel,
    innerPadding: PaddingValues,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projectsList.collectAsStateWithLifecycle()
    val builds by viewModel.buildHistoryList.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Hero Header
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape = RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top status descriptor block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(width = 2.dp, color = Color(0xFFD1FAE5), shape = CircleShape)
                            )
                            Text(
                                text = "Engine: Ready",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "JDK 17.0",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Interactive Import card block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF1D4ED8)
                                    )
                                )
                            )
                            .clickable { onImportClick() }
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Import ZIP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Select your Android project",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Engine Status & Quick Sandbox Starters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Engine Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Build Engine", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (engineStatus) {
                                "Ready" -> Color(0xFF4CAF50)
                                "Building" -> Color(0xFF03A9F4)
                                else -> Color(0xFFFF9800)
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when(engineStatus) {
                                    "Ready" -> "Ready (v8.5)"
                                    "Building" -> "Compiling"
                                    else -> "Need Setup"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Sandbox shortcut helper
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Try Sandbox", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${projects.size} Local Project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Interactive Sandbox Launcher Section
        if (projects.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum punya ZIP project? Coba starter kami:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.loadStarterDemoProject("Cosmic Calculator")
                                    Toast.makeText(context, "Mempersiapkan Cosmic Calculator!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Calculator", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadStarterDemoProject("Epic Todo List")
                                    Toast.makeText(context, "Mempersiapkan Epic Todo List!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Todo List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Recent Builds card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Build Terbaru",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (builds.isNotEmpty()) {
                    TextButton(onClick = { viewModel.setActiveTab(2) }) {
                        Text("Lihat Pipeline", fontSize = 12.sp)
                    }
                }
            }
        }

        if (builds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada riwayat build.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Text(
                            text = "Project yang dikompilasi berhasil atau gagal akan tampil di sini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                        )
                    }
                }
            }
        } else {
            items(builds.take(4)) { build ->
                BuildHistoryRow(build = build, context = context)
            }
        }
    }
}

@Composable
fun BuildHistoryRow(build: BuildHistoryEntity, context: Context) {
    val durationText = "${build.durationMs / 1000}s"
    val sizeText = build.apkSize?.let { ProjectManager.formatSize(it) } ?: ""
    val isSuccess = build.status == "Success"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.12f)
                        else Color(0xFFF44336).copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = build.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = build.projectName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = build.status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = "$durationText • $sizeText",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (isSuccess && build.apkPath != null) {
                IconButton(
                    onClick = { shareApkFile(context, File(build.apkPath)) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share APK",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- PROJECTS TAB SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsTabScreen(
    viewModel: PocketBuildViewModel,
    innerPadding: PaddingValues,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projectsList.collectAsStateWithLifecycle()
    val selectedProj by viewModel.selectedProject.collectAsStateWithLifecycle()

    var showEditorSheet by remember { mutableStateOf(false) }

    // visual configurations state
    var editAppName by remember { mutableStateOf("") }
    var editPackageId by remember { mutableStateOf("") }
    var editVerName by remember { mutableStateOf("") }
    var editAccentColor by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daftar Project",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Kelola resource, sesuaikan variabel build visual",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }

            IconButton(
                onClick = onImportClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add ZIP", modifier = Modifier.size(20.dp))
            }
        }

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No project imported yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Klik tambah / import ZIP diatas, atau muat starter pack kami di menu dashboard Home.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, top = 6.dp, end = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    val isSelected = selectedProj?.id == project.id
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectProject(project)
                                editAppName = project.appName
                                editPackageId = project.packageName
                                editVerName = project.versionName
                                editAccentColor = project.primaryColorHex
                                showEditorSheet = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Simulated Launcher Icon
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = Color(android.graphics.Color.parseColor(project.primaryColorHex)).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = Color(android.graphics.Color.parseColor(project.primaryColorHex)),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = project.appName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = project.packageName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Status Badge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when (project.status) {
                                        "Valid" -> Color(0xFF4CAF50)
                                        "Built" -> Color(0xFF0288D1)
                                        else -> Color(0xFFF44336)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(statusColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = project.status,
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteProject(project) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gradles: " + if (project.hasGradle) "Detected" else "Missing",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )

                                Text(
                                    text = "v${project.versionName} (${project.versionCode})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // BOTTOM CUSTOMIZER DRAWER - "Canva untuk APK"
    if (showEditorSheet && selectedProj != null) {
        val project = selectedProj!!

        ModalBottomSheet(
            onDismissRequest = { showEditorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drawer Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Canva untuk APK",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Visual Customizer Pro (Simulasi)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = Color(android.graphics.Color.parseColor(editAccentColor)).copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(android.graphics.Color.parseColor(editAccentColor)), CircleShape)
                        )
                    }
                }

                // Details Error state if any
                if (project.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(project.errorMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Editable Fields
                OutlinedTextField(
                    value = editAppName,
                    onValueChange = { editAppName = it },
                    label = { Text("Nama Aplikasi (Launcher)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPackageId,
                    onValueChange = { editPackageId = it },
                    label = { Text("Package Name (Bundle ID)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editVerName,
                        onValueChange = { editVerName = it },
                        label = { Text("Version", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = "35",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Target SDK", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Accent Theme Colors Pickers
                Column {
                    Text("Pilih Warna Utama Aplikasi (Primary Accent)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val hexColors = listOf("#0288D1", "#9C27B0", "#E91E63", "#4CAF50", "#FF9800", "#795548", "#212121")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        hexColors.forEach { hex ->
                            val isChosen = editAccentColor.lowercase() == hex.lowercase()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .clickable { editAccentColor = hex }
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveVisualCustomization(
                                projectId = project.id,
                                newAppName = editAppName,
                                newPackageName = editPackageId,
                                newVersionName = editVerName,
                                newColorHex = editAccentColor
                            )
                            Toast.makeText(context, "Resource strings & configs disimpan!", Toast.LENGTH_SHORT).show()
                            showEditorSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan & Update", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // save modifications and instantly route to compilation build
                            viewModel.saveVisualCustomization(
                                projectId = project.id,
                                newAppName = editAppName,
                                newPackageName = editPackageId,
                                newVersionName = editVerName,
                                newColorHex = editAccentColor
                            )
                            viewModel.setActiveTab(2) // build pipeline
                            showEditorSheet = false
                            // auto start build compiler
                            viewModel.triggerApkBuild(project)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compile APK", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- BUILD TAB SCREEN ---
@Composable
fun BuildTabScreen(
    viewModel: PocketBuildViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val selectedProj by viewModel.selectedProject.collectAsStateWithLifecycle()
    val buildState by viewModel.buildState.collectAsStateWithLifecycle()
    val buildProgress by viewModel.buildProgress.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentBuildStep.collectAsStateWithLifecycle()
    val compilerLogs by viewModel.compilerLogs.collectAsStateWithLifecycle()
    val lastApkFile by viewModel.lastBuiltApkFile.collectAsStateWithLifecycle()

    val consoleScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // AutoScroll terminal console on log updates
    LaunchedEffect(compilerLogs.size) {
        if (compilerLogs.isNotEmpty()) {
            coroutineScope.launch {
                consoleScrollState.animateScrollToItem(compilerLogs.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab header
        Text(
            text = "Build Pipeline Engine",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Status: " + if (selectedProj != null) "Loaded project '${selectedProj?.appName}'" else "No project selected",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedProj == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Silakan pilih project dahulu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Buka tab 'Projects' lalu ketuk pada salah satu project untuk meluncurkan pipeline build.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, top = 4.dp, end = 32.dp)
                    )
                }
            }
        } else {
            val project = selectedProj!!

            // Steps and Progress Ring Display
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compilation progress meter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { buildProgress },
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(buildProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = when(buildState) {
                                "Running" -> "Memproses Kompilasi..."
                                "Finished" -> "APK successfully built"
                                "Failed" -> "Error: Gagal Membuat APK"
                                else -> "Engine Siap (Idle)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = when(buildState) {
                                "Finished" -> Color(0xFF4CAF50)
                                "Failed" -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Text(
                            text = if (buildState == "Running") {
                                when (currentStep) {
                                    0 -> "Extracting source ZIP files..."
                                    1 -> "Checking root build folders..."
                                    2 -> "Loading dynamic libraries SDK..."
                                    3 -> "Running JVM Kotlin compiler tasks..."
                                    4 -> "Signing installer packages..."
                                    else -> "Exporting APK binaries..."
                                }
                            } else "Loaded Project: ${project.appName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Interactive Building Pipeline Stages Timeline
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val stepsList = listOf(
                        "Extracting ZIP",
                        "Checking Gradle Project",
                        "Preparing Embedded SDK",
                        "Running Gradle Build",
                        "Signing APK",
                        "APK Ready"
                    )

                    stepsList.forEachIndexed { idx, title ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val stepPassed = currentStep > idx || buildState == "Finished"
                            val stepCurrent = currentStep == idx && buildState == "Running"

                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = when {
                                            stepPassed -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            stepCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stepPassed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                } else if (stepCurrent) {
                                    CircularProgressIndicator(strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                                } else {
                                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (stepCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (stepPassed) MaterialTheme.colorScheme.onSurface
                                else if (stepCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Monospaced Console Terminal Card
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Build Console Output Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(compilerLogs.joinToString("\n")))
                            Toast.makeText(context, "Log berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Salin", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                if (compilerLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Log konsol kosong. Mulai build untuk melihat aktifitas.",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = consoleScrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(compilerLogs) { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.startsWith(">") || logLine.contains("Task")) Color(0xFF00E5FF)
                                else if (logLine.contains("FAILED") || logLine.contains("Error") || logLine.contains("❌")) Color(0xFFF44336)
                                else if (logLine.contains("successfully") || logLine.contains("SUCCESS") || logLine.contains("✓")) Color(0xFF4CAF50)
                                else Color.White.copy(alpha = 0.90f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // CTAs Float Row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.triggerApkBuild(project) },
                    enabled = buildState != "Running",
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Build APK", fontWeight = FontWeight.Bold)
                }

                if (buildState == "Finished" && lastApkFile != null) {
                    Button(
                        onClick = { shareApkFile(context, lastApkFile!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share APK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- ENGINE TAB SCREEN ---
@Composable
fun EngineTabScreen(
    viewModel: PocketBuildViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdatingEngine.collectAsStateWithLifecycle()
    val progress by viewModel.engineProgress.collectAsStateWithLifecycle()
    val logs by viewModel.engineLogs.collectAsStateWithLifecycle()

    val logsScrollState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logsScrollState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Build Engine Admin",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Informasi environment virtual SDK, SDK Gradle, compiler, platform build tools dan Java JDK bawaan.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        // Status Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (engineStatus == "Ready") Color(0xFF4CAF50).copy(alpha = 0.08f)
                else Color(0xFFFF9800).copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (engineStatus == "Ready") Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (engineStatus == "Ready") Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Status: Embedded Engine " + if (engineStatus == "Ready") "Aktif & Siap" else "Butuh Update",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "JDK 17 + Gradle 8.5 Local Host",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Engine specifications cards
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineSpecRow("JDK Runtime", "Eclipse Temurin v17.0.8 (JVM Compiler)")
                EngineSpecRow("Gradle Sync", "Gradle Wrapper Framework v8.5 API")
                EngineSpecRow("Android SDK Compiler", "Platform Target SDK 35 (Android Vanilla)")
                EngineSpecRow("Android Build Tools", "AAPT2 packaging toolchain version 35.0.0")
                EngineSpecRow("Platforms Supported", "targetSdk (21 up to 35) ARM64-v8a compatible")
                EngineSpecRow("Auto Sign Module", "Internal SHA256withRSA auto private-key build")
            }
        }

        // Live updating progress if active
        if (isUpdating) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Men-download Bundle Komponen Engine...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Progress: ${(progress * 100).toInt()}% • Harap jangan tutup aplikasi.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Engine Updates Live logger
        Text("Engine Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Log engine kosong.", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    state = logsScrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { logLine ->
                        Text(
                            text = logLine,
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Button trigger updates
        Button(
            onClick = { viewModel.updateBuildEngine() },
            enabled = !isUpdating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Update Build Engine", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EngineSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.size(16.dp))
    }
}

// --- SETTINGS TAB SCREEN ---
@Composable
fun SettingsTabScreen(
    viewModel: PocketBuildViewModel,
    innerPadding: PaddingValues,
    onClearCacheClick: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoSign by viewModel.autoSign.collectAsStateWithLifecycle()
    val outputFolder by viewModel.outputFolder.collectAsStateWithLifecycle()

    var customPathInput by remember { mutableStateOf(outputFolder) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pengaturan",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Theme Options Row Chips
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tema Aplikasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Pilih variasi mode gelap / terang untuk kenyamanan coding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val listOfThemes = listOf("Light", "Dark", "System")
                    listOfThemes.forEach { theme ->
                        val isChosen = themeMode == theme
                        FilterChip(
                            selected = isChosen,
                            onClick = { viewModel.setThemeMode(theme) },
                            label = { Text(theme, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Folder Location Outputs paths configurations
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Output Folder Directory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Lokasi folder penyimpanan file ZIP, resource mod, dan APK compile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customPathInput,
                    onValueChange = {
                        customPathInput = it
                        viewModel.setOutputFolder(it)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Auto Sign APK toggles
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Sign APK Target", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Secara otomatis menandatangani file hasil build APK menggunakan SHA256 Key bawaan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                Switch(
                    checked = autoSign,
                    onCheckedChange = { viewModel.setAutoSign(it) }
                )
            }
        }

        // Cache removal actions
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sandboxed Storage Cache", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Kosongkan direktori build dan reset database list project internal dari memori", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onClearCacheClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus Semua Data Cache", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- FILE PATH & NAME RESOLVERS HELPER ---
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

// --- FILE SHARING / INSTALL PROVIDER ROUTINES ---
private fun shareApkFile(context: Context, file: File) {
    try {
        if (!file.exists()) {
            Toast.makeText(context, "File APK tidak ditemukan!", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate content URI using safe FileProvider wrapper
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share PocketBuild Compiled APK")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
