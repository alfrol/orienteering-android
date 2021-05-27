package ee.taltech.alfrol.hw02.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity
data class Session(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0L,

    @ColumnInfo(name = "external_id")
    val externalId: String? = null,

    val name: String = DEFAULT_NAME,
    val description: String = DEFAULT_DESCRIPTION,

    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis(),

    val distance: Float = 0.0f,
    val duration: Long = 0,
    val pace: Float = 0.0f
) {
    companion object {
        private const val DEFAULT_NAME = "Session Description"
        private const val DEFAULT_DESCRIPTION = "Session Description"
    }

    /**
     * ISO-8601 formatted date of session start.
     */
    val recordedAtIso: String
        get() {
            val instant = Instant.ofEpochMilli(recordedAt)
            return OffsetDateTime
                .ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

    val recordedAtIsoShort: String
        get() {
            val instant = Instant.ofEpochMilli(recordedAt)
            return OffsetDateTime
                .ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
}
