package com.max.aiassistant.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        MemoryEntityRecord::class,
        MemoryRelationRecord::class,
        MemoryFactRecord::class,
        WeatherCacheEntity::class,
        TaskEntity::class,
        SubTaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MaxDatabase : RoomDatabase() {

    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryGraphDao(): MemoryGraphDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weather_cache (
                        cache_key TEXT NOT NULL,
                        city_name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        current_temperature REAL NOT NULL,
                        current_humidity INTEGER NOT NULL,
                        current_wind_speed REAL NOT NULL,
                        weather_code INTEGER NOT NULL,
                        hourly_forecasts_json TEXT NOT NULL,
                        daily_forecasts_json TEXT NOT NULL,
                        grass_pollen REAL,
                        birch_pollen REAL,
                        alder_pollen REAL,
                        olive_pollen REAL,
                        mugwort_pollen REAL,
                        ragweed_pollen REAL,
                        weather_fetched_at INTEGER NOT NULL,
                        pollen_fetched_at INTEGER,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(cache_key)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_weather_cache_city_name ON weather_cache(city_name)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_weather_cache_updated_at ON weather_cache(updated_at)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        note TEXT NOT NULL,
                        status TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        deadline_date TEXT NOT NULL,
                        category TEXT NOT NULL,
                        estimated_duration TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sub_tasks (
                        id TEXT NOT NULL,
                        task_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        is_completed INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(task_id) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_updated_at ON tasks(updated_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sub_tasks_task_id ON sub_tasks(task_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sub_tasks_updated_at ON sub_tasks(updated_at)")
            }
        }

        @Volatile
        private var INSTANCE: MaxDatabase? = null

        fun getInstance(context: Context): MaxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MaxDatabase::class.java,
                    "max_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
