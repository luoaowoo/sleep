package com.sleep.snore.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepDatabaseMigrationTest {

    private lateinit var databaseFile: File
    private lateinit var database: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseFile = File(context.cacheDir, "migration-smoke.db")
        databaseFile.delete()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseFile.absolutePath)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersionOneSchema(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        database = helper.writableDatabase
    }

    @After
    fun tearDown() {
        database.close()
        databaseFile.delete()
    }

    @Test
    fun migrationOneToTwoCreatesExpectedIndexes() {
        SleepDatabaseModule.MIGRATION_1_2.migrate(database)

        val indexes = queryIndexes()

        assertTrue(indexes.contains("index_sleep_records_start_time"))
        assertTrue(indexes.contains("index_sleep_records_created_at"))
        assertTrue(indexes.contains("index_snore_events_record_id_start_timestamp"))
        assertTrue(indexes.contains("index_snore_events_start_timestamp"))
    }

    private fun queryIndexes(): Set<String> {
        val indexes = mutableSetOf<String>()
        database.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(0))
            }
        }
        return indexes
    }

    private fun createVersionOneSchema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sleep_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `start_time` INTEGER NOT NULL,
                `end_time` INTEGER NOT NULL,
                `sleep_duration_min` INTEGER NOT NULL,
                `snore_score` INTEGER NOT NULL,
                `severity` TEXT NOT NULL,
                `est_ahi` REAL NOT NULL,
                `snore_duration_min` INTEGER NOT NULL,
                `snore_ratio` REAL NOT NULL,
                `avg_db` REAL NOT NULL,
                `max_db` REAL NOT NULL,
                `snore_event_count` INTEGER NOT NULL,
                `apnea_event_count` INTEGER NOT NULL,
                `longest_apnea_sec` INTEGER NOT NULL,
                `snore_type_distribution` TEXT NOT NULL,
                `hourly_distribution` TEXT NOT NULL,
                `ai_summary` TEXT NOT NULL,
                `ai_evaluation` TEXT NOT NULL,
                `ai_suggestions` TEXT NOT NULL,
                `is_favorited` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `snore_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `record_id` INTEGER NOT NULL,
                `start_timestamp` INTEGER NOT NULL,
                `duration_ms` INTEGER NOT NULL,
                `peak_db` REAL NOT NULL,
                `avg_db` REAL NOT NULL,
                `dominant_freq` REAL NOT NULL,
                `snore_type` TEXT NOT NULL,
                `audio_file_path` TEXT NOT NULL,
                `audio_file_size_bytes` INTEGER NOT NULL,
                `ai_type_label` TEXT NOT NULL,
                `is_favorited` INTEGER NOT NULL,
                FOREIGN KEY(`record_id`) REFERENCES `sleep_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_snore_events_record_id` ON `snore_events` (`record_id`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `factor_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `record_id` INTEGER,
                `date` TEXT NOT NULL,
                `alcohol_drinks` INTEGER NOT NULL,
                `sleep_position` TEXT NOT NULL,
                `weight_kg` REAL,
                `nasal_congestion` INTEGER NOT NULL,
                `notes` TEXT
            )
            """.trimIndent()
        )
    }
}
