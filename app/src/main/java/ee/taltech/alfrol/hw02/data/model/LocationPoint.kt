package ee.taltech.alfrol.hw02.data.model

import androidx.room.*

@Entity(
    tableName = "location_point",
    foreignKeys = [ForeignKey(
        onDelete = ForeignKey.CASCADE,
        entity = Session::class,
        parentColumns = ["_id"],
        childColumns = ["session_id"]
    )],
    indices = [Index("session_id")]
)
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
