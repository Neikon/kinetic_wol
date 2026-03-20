package dev.neikon.kineticwol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WakeDeviceEntity::class],
    version = 1,
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
            ).build()
    }
}
