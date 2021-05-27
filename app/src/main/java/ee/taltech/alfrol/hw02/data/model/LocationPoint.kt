package ee.taltech.alfrol.hw02.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_point")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0L,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,

    val latitude: Double,
    val longitude: Double,
    val type: String
)
