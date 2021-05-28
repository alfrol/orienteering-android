package ee.taltech.alfrol.hw02.data.dao

import androidx.room.*
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.model.SessionWithLocationPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("SELECT * FROM Session ORDER BY recorded_at DESC")
    fun findAllSortedByRecordedAt(): Flow<List<Session>>

    @Query("SELECT * FROM Session ORDER BY distance DESC")
    fun findAllSortedByDistance(): Flow<List<Session>>

    @Query("SELECT * FROM Session ORDER BY duration DESC")
    fun findAllSortedByDuration(): Flow<List<Session>>

    @Query("SELECT * FROM Session ORDER BY pace DESC")
    fun findAllSortedByPace(): Flow<List<Session>>

    @Transaction
    @Query("SELECT * FROM Session WHERE external_id = :id")
    fun findByExternalId(id: String): Flow<List<SessionWithLocationPoints>>
}