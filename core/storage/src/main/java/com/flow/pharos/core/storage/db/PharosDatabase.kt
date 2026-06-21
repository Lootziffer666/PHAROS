package com.flow.pharos.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `claims` (
                        `id` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `sourceFileId` TEXT NOT NULL,
                        `sourceFileName` TEXT NOT NULL,
                        `sourceTimestamp` INTEGER NOT NULL,
                        `extractedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `confidence` REAL NOT NULL,
                        `clusterId` TEXT,
                        `supersededById` TEXT,
                        `supersedes` TEXT,
                        `aiRationale` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceFileId`) REFERENCES `files`(`id`) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_sourceFileId` ON `claims`(`sourceFileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_status` ON `claims`(`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_clusterId` ON `claims`(`clusterId`)")
            }
        }

        fun getInstance(context: Context): PharosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PharosDatabase::class.java,
                    "pharos_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
