package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val primaryColorHex: String,
    val status: String, // "Valid", "Error", "Built"
    val dateImported: Long,
    val folderPath: String,
    val hasGradle: Boolean,
    val errorMessage: String? = null,
    val targetSdk: Int = 35,
    val gradleVersion: String = "8.5",
    val jdkVersion: String = "17"
)

@Entity(tableName = "builds")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val projectName: String,
    val appName: String,
    val status: String, // "Success", "Failed"
    val apkPath: String?,
    val apkSize: Long?,
    val timestamp: Long,
    val durationMs: Long,
    val logs: String
)

// --- daos ---

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY dateImported DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface BuildDao {
    @Query("SELECT * FROM builds ORDER BY timestamp DESC")
    fun getAllBuilds(): Flow<List<BuildHistoryEntity>>

    @Query("SELECT * FROM builds WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getBuildsForProject(projectId: Long): Flow<List<BuildHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: BuildHistoryEntity): Long

    @Query("DELETE FROM builds WHERE id = :id")
    suspend fun deleteBuildById(id: Long)

    @Query("DELETE FROM builds")
    suspend fun deleteAllBuilds()
}

// --- database ---

@Database(entities = [ProjectEntity::class, BuildHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun buildDao(): BuildDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocketbuild_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- repository ---

class PocketBuildRepository(private val db: AppDatabase) {
    val projectDao = db.projectDao()
    val buildDao = db.buildDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allBuilds: Flow<List<BuildHistoryEntity>> = buildDao.getAllBuilds()

    suspend fun getProjectById(id: Long) = projectDao.getProjectById(id)

    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) = projectDao.deleteProject(project)

    suspend fun deleteAllProjects() = projectDao.deleteAllProjects()

    suspend fun insertBuild(build: BuildHistoryEntity): Long = buildDao.insertBuild(build)

    fun getBuildsForProject(projectId: Long) = buildDao.getBuildsForProject(projectId)

    suspend fun deleteBuildById(id: Long) = buildDao.deleteBuildById(id)

    suspend fun deleteAllBuilds() = buildDao.deleteAllBuilds()
}
