package dev.neikon.kineticwol.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeDeviceDao {
    @Query("SELECT * FROM wake_devices ORDER BY normalized_name ASC")
    fun observeAll(): Flow<List<WakeDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WakeDeviceEntity)

    @Query("DELETE FROM wake_devices WHERE id = :deviceId")
    suspend fun deleteById(deviceId: String)

    @Query("SELECT * FROM wake_devices WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): WakeDeviceEntity?
}
