package dev.neikon.kineticwol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WakeDeviceEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class KineticWolDatabase : RoomDatabase() {
    abstract fun wakeDeviceDao(): WakeDeviceDao

    companion object {
        fun create(context: Context): KineticWolDatabase =
            Room.databaseBuilder(
                context,
                KineticWolDatabase::class.java,
                "kinetic_wol.db",
            ).addMigrations(MIGRATION_1_2)
                .build()
    }
}

private val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE wake_devices
                ADD COLUMN shutdown_enabled INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE wake_devices
                ADD COLUMN shutdown_method TEXT
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE wake_devices
                ADD COLUMN shutdown_agent_base_url TEXT
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE wake_devices
                ADD COLUMN shutdown_agent_auth_token TEXT
                """.trimIndent(),
            )
        }
    }
