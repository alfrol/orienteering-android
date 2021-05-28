package ee.taltech.alfrol.hw02.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * One-to-many relation class for [Session] and [LocationPoint].
 */
data class SessionWithLocationPoints(
    @Embedded
    val session: Session,

    @Relation(parentColumn = "_id", entityColumn = "session_id")
    val locationPoints: List<LocationPoint>
)
