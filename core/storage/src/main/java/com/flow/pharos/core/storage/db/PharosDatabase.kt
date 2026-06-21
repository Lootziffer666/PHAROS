package com.flow.pharos.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.core.model.entity.FileEntity
import com.flow.pharos.core.model.entity.FolderEntity
import com.flow.pharos.core.model.entity.ProjectEntity
import com.flow.pharos.core.model.entity.ProjectFileCrossRef
import com.flow.pharos.core.storage.db.converter.Converters
import com.flow.pharos.core.storage.db.dao.AnalysisDao
import com.flow.pharos.core.storage.db.dao.ClaimDao
import com.flow.pharos.core.storage.db.dao.FileDao
import com.flow.pharos.core.storage.db.dao.FolderDao
import com.flow.pharos.core.storage.db.dao.ProjectDao
import com.flow.pharos.core.storage.db.dao.ProjectFileCrossRefDao

@Database(
    entities = [
        FolderEntity::class,
        FileEntity::class,
        AnalysisEntity::class,
        ProjectEntity::class,
        ProjectFileCrossRef::class,
        ClaimEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PharosDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileCrossRefDao(): ProjectFileCrossRefDao
    abstract fun claimDao(): ClaimDao

    companion object {
        @Volatile
        private var INSTANCE: PharosDatabase? = null

        fun getInstance(context: Context): PharosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PharosDatabase::class.java,
                    "pharos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
