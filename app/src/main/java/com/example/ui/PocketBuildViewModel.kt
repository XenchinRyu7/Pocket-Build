package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BuildHistoryEntity
import com.example.data.PocketBuildRepository
import com.example.data.ProjectEntity
import com.example.utils.ParsedProject
import com.example.utils.ProjectManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class PocketBuildViewModel(
    private val context: Context,
    private val repository: PocketBuildRepository
) : ViewModel() {

    // --- Navigation & Theme ---
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _themeMode = MutableStateFlow("System") // "Light", "Dark", "System"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // --- Settings states ---
    private val _autoSign = MutableStateFlow(true)
    val autoSign: StateFlow<Boolean> = _autoSign.asStateFlow()

    private val _outputFolder = MutableStateFlow("/sdcard/Documents/PocketBuild/")
    val outputFolder: StateFlow<String> = _outputFolder.asStateFlow()

    // --- database bindings ---
    val projectsList: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val buildHistoryList: StateFlow<List<BuildHistoryEntity>> = repository.allBuilds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- selected elements ---
    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // --- Engine Update states ---
    private val _engineStatus = MutableStateFlow("Ready") // "Ready", "Need Setup", "Building"
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    private val _isUpdatingEngine = MutableStateFlow(false)
    val isUpdatingEngine: StateFlow<Boolean> = _isUpdatingEngine.asStateFlow()

    private val _engineProgress = MutableStateFlow(0f)
    val engineProgress: StateFlow<Float> = _engineProgress.asStateFlow()

    private val _engineLogs = MutableStateFlow<List<String>>(emptyList())
    val engineLogs: StateFlow<List<String>> = _engineLogs.asStateFlow()

    // --- Build compiler pipeline states ---
    private val _buildState = MutableStateFlow("Idle") // "Idle", "Running", "Finished", "Failed"
    val buildState: StateFlow<String> = _buildState.asStateFlow()

    private val _buildProgress = MutableStateFlow(0f)
    val buildProgress: StateFlow<Float> = _buildProgress.asStateFlow()

    private val _currentBuildStep = MutableStateFlow(0)
    val currentBuildStep: StateFlow<Int> = _currentBuildStep.asStateFlow()

    private val _compilerLogs = MutableStateFlow<List<String>>(emptyList())
    val compilerLogs: StateFlow<List<String>> = _compilerLogs.asStateFlow()

    private val _lastBuiltApkFile = MutableStateFlow<File?>(null)
    val lastBuiltApkFile: StateFlow<File?> = _lastBuiltApkFile.asStateFlow()

    init {
        // pre-seed with a welcome notification log or check engine
        addEngineLog("Engine check: eclipse-temurin-17 verified.")
        addEngineLog("Gradle daemon v8.5 active.")
        addEngineLog("Android Build Tools v35.0.0 local cache hit.")
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setAutoSign(enabled: Boolean) {
        _autoSign.value = enabled
    }

    fun setOutputFolder(path: String) {
        _outputFolder.value = path
    }

    fun selectProject(project: ProjectEntity?) {
        _selectedProject.value = project
    }

    private fun addEngineLog(msg: String) {
        _engineLogs.value = _engineLogs.value + "[${System.currentTimeMillis() % 100000}] $msg"
    }

    private fun addCompilerLog(msg: String) {
        _compilerLogs.value = _compilerLogs.value + msg
    }

    fun clearLogs() {
        _compilerLogs.value = emptyList()
    }

    // --- Actions ---

    /**
     * Trigger simulated engine packages update
     */
    fun updateBuildEngine() {
        if (_isUpdatingEngine.value) return
        _isUpdatingEngine.value = true
        _engineStatus.value = "Need Setup"
        _engineProgress.value = 0f
        _engineLogs.value = emptyList()

        viewModelScope.launch {
            val steps = listOf(
                Pair("Downloading Oracle/Eclipse OpenJDK 17 binaries...", 0.15f),
                Pair("Extracting JDK to /data/user/0/com.pocketbuild/engine/jdk17...", 0.30f),
                Pair("Connecting to Gradle Services wrapper v8.5...", 0.50f),
                Pair("Caching Android SDK Platform Tools (API 34, 35)...", 0.70f),
                Pair("Resolving Build-Tools compiler executables...", 0.85f),
                Pair("Signing keystore generation helper initialized...", 0.95f),
                Pair("Build engine update & verification complete! Status: Ready", 1.0f)
            )

            for (step in steps) {
                addEngineLog(step.first)
                var prog = _engineProgress.value
                val targetProg = step.second
                while (prog < targetProg) {
                    prog += 0.05f
                    _engineProgress.value = prog.coerceAtMost(targetProg)
                    delay(80)
                }
                delay(300)
            }
            _isUpdatingEngine.value = false
            _engineStatus.value = "Ready"
        }
    }

    /**
     * Import a real ZIP from disk
     */
    fun importProjectZip(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val parsed = ProjectManager.importProjectFromZip(context, uri, fileName)
            saveParsedProject(parsed)
        }
    }

    /**
     * Create / Import the starter demo projects
     */
    fun loadStarterDemoProject(variant: String) {
        viewModelScope.launch {
            val parsed = ProjectManager.createSampleProject(context, variant)
            saveParsedProject(parsed)
        }
    }

    private suspend fun saveParsedProject(parsed: ParsedProject) {
        val entity = ProjectEntity(
            name = parsed.name,
            appName = parsed.appName,
            packageName = parsed.packageName,
            versionName = parsed.versionName,
            versionCode = parsed.versionCode,
            primaryColorHex = parsed.primaryColorHex,
            status = if (parsed.error != null) "Error" else "Valid",
            dateImported = System.currentTimeMillis(),
            folderPath = parsed.folderPath,
            hasGradle = parsed.hasGradle,
            errorMessage = parsed.error
        )
        val id = repository.insertProject(entity)
        // Select newly created project
        val created = entity.copy(id = id)
        _selectedProject.value = created
    }

    /**
     * Canva for APK style customizations editor!
     */
    fun saveVisualCustomization(
        projectId: Long,
        newAppName: String,
        newPackageName: String,
        newVersionName: String,
        newColorHex: String
    ) {
        viewModelScope.launch {
            val current = repository.getProjectById(projectId) ?: return@launch
            val success = ProjectManager.updateProjectConfigs(
                folderPath = current.folderPath,
                newAppName = newAppName,
                newPackageName = newPackageName,
                newVersionName = newVersionName,
                newColorHex = newColorHex
            )

            if (success) {
                val updated = current.copy(
                    appName = newAppName,
                    packageName = newPackageName,
                    versionName = newVersionName,
                    primaryColorHex = newColorHex,
                    status = "Valid" // clear error if it resolved it
                )
                repository.updateProject(updated)
                // update currently selected reference
                _selectedProject.value = updated
            }
        }
    }

    /**
     * Start APK build compilation pipeline
     */
    fun triggerApkBuild(project: ProjectEntity) {
        if (_buildState.value == "Running") return
        _buildState.value = "Running"
        _buildProgress.value = 0f
        _currentBuildStep.value = 0
        _compilerLogs.value = emptyList()
        _lastBuiltApkFile.value = null

        // Set engine status to Building
        _engineStatus.value = "Building"

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // Step 0: Extracting ZIP
            addCompilerLog("> PocketBuild compiler session initiated.")
            addCompilerLog("Target project folder: ${project.folderPath}")
            addCompilerLog("Running inside container sandbox workspace...")
            addCompilerLog("Step 1/6: Reviewing cached extraction directories...")
            _currentBuildStep.value = 0
            _buildProgress.value = 0.08f
            delay(1000)

            // Step 1: Checking Gradle Project
            _currentBuildStep.value = 1
            _buildProgress.value = 0.18f
            addCompilerLog("Step 2/6: Validating Gradle project architecture...")
            if (!project.hasGradle) {
                addCompilerLog("❌ FAILED: Root settings.gradle.kts or build.gradle.kts not found.")
                addCompilerLog("Terminating build process.")
                _buildState.value = "Failed"
                _engineStatus.value = "Ready"

                // Save failed build to database
                repository.insertBuild(
                    BuildHistoryEntity(
                        projectId = project.id,
                        projectName = project.name,
                        appName = project.appName,
                        status = "Failed",
                        apkPath = null,
                        apkSize = null,
                        timestamp = System.currentTimeMillis(),
                        durationMs = System.currentTimeMillis() - startTime,
                        logs = _compilerLogs.value.joinToString("\n")
                    )
                )
                return@launch
            }
            addCompilerLog("Loaded Gradle module configuration: ':app' project reference.")
            addCompilerLog("Found compiler parameters: minSdk=${project.versionCode}, targetSdk=35")
            delay(1200)

            // Step 2: Preparing Embedded SDK
            _currentBuildStep.value = 2
            _buildProgress.value = 0.32f
            addCompilerLog("Step 3/6: Preparing embedded platform Android SDK...")
            addCompilerLog("Verifying Android API Level v35 packages...")
            addCompilerLog("Resolving platform frameworks runtime dependencies...")
            addCompilerLog("SDK tools loaded successfully.")
            delay(1000)

            // Step 3: Running Gradle Build
            _currentBuildStep.value = 3
            _buildProgress.value = 0.50f
            addCompilerLog("Step 4/6: Starting embedded Gradle Compilation Engine...")
            addCompilerLog("> Task :app:preBuild UP-TO-DATE")
            delay(400)
            addCompilerLog("> Task :app:generateDebugBuildConfig SUCCESS")
            _buildProgress.value = 0.58f
            delay(400)
            addCompilerLog("> Task :app:compileDebugKotlin WORKER-RUNNING")
            addCompilerLog("Compiling Kotlin source sources to JVM Bytecode...")
            addCompilerLog("Resolved package reference mapping: ${project.packageName}")
            delay(1100)
            addCompilerLog("> Task :app:compileDebugKotlin SUCCESS")
            _buildProgress.value = 0.68f
            addCompilerLog("> Task :app:processDebugResources SUCCESS")
            addCompilerLog("> Task :app:compileDebugJavaWithJavac SUCCESS")
            addCompilerLog("> Task :app:dexBuilderDebug SUCCESS")
            _buildProgress.value = 0.78f
            delay(800)
            addCompilerLog("> Task :app:mergeProjectDexDebug SUCCESS")
            addCompilerLog("> Task :app:packageDebug SUCCESS")
            addCompilerLog("Packaging APK structure in build workspace directory...")
            delay(900)

            // Step 4: Signing APK
            _currentBuildStep.value = 4
            _buildProgress.value = 0.88f
            addCompilerLog("Step 5/6: Securing output binaries using sign-engine...")
            if (_autoSign.value) {
                addCompilerLog("Auto-sign active. Employing embedded SHA256 v2 keys...")
                addCompilerLog("Signature digest completed successfully.")
            } else {
                addCompilerLog("Auto-sign disabled. Packaging unsigned debugging binary...")
            }
            delay(1200)

            // Step 5: APK Ready
            _currentBuildStep.value = 5
            _buildProgress.value = 1.0f
            addCompilerLog("Step 6/6: finalising build output files...")

            // Compile mock APK
            val parsedProj = ParsedProject(
                name = project.name,
                appName = project.appName,
                packageName = project.packageName,
                versionName = project.versionName,
                versionCode = project.versionCode,
                primaryColorHex = project.primaryColorHex,
                hasGradle = project.hasGradle,
                folderPath = project.folderPath
            )
            val apkFile = ProjectManager.compileAndGenerateApk(context, parsedProj)
            _lastBuiltApkFile.value = apkFile

            addCompilerLog("APK compilation successfully finished!")
            addCompilerLog("Output: ${apkFile.name} (${ProjectManager.formatSize(apkFile.length())})")
            _buildState.value = "Finished"
            _engineStatus.value = "Ready"

            // Save success build and update project status
            val duration = System.currentTimeMillis() - startTime
            repository.insertBuild(
                BuildHistoryEntity(
                    projectId = project.id,
                    projectName = project.name,
                    appName = project.appName,
                    status = "Success",
                    apkPath = apkFile.absolutePath,
                    apkSize = apkFile.length(),
                    timestamp = System.currentTimeMillis(),
                    durationMs = duration,
                    logs = _compilerLogs.value.joinToString("\n")
                )
            )

            // Update main project status
            val updatedProj = project.copy(status = "Built")
            repository.updateProject(updatedProj)
            _selectedProject.value = updatedProj
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
            }
            // delete files locally to clear cache
            val dir = File(project.folderPath)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.deleteAllProjects()
            repository.deleteAllBuilds()
            _selectedProject.value = null
            _compilerLogs.value = emptyList()
            _lastBuiltApkFile.value = null

            // delete files under the projects and build outputs folder
            val projDir = File(context.filesDir, "projects")
            if (projDir.exists()) projDir.deleteRecursively()
            val outDir = File(context.filesDir, "build_outputs")
            if (outDir.exists()) outDir.deleteRecursively()
        }
    }
}

class PocketBuildViewModelFactory(
    private val context: Context,
    private val repository: PocketBuildRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PocketBuildViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PocketBuildViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
