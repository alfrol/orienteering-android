package ee.taltech.alfrol.hw02.data.dao

import androidx.room.*
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.model.SessionWithLocationPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM Session ORDER BY recorded_at DESC")
    fun findAll(): Flow<List<Session>>

    @Transaction
    @Query("SELECT * FROM Session WHERE external_id = :id")
    fun findByExternalId(id: String): Flow<List<SessionWithLocationPoints>>

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}