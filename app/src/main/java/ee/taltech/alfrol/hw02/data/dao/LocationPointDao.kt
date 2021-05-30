package ee.taltech.alfrol.hw02.data.dao

import androidx.room.*
import ee.taltech.alfrol.hw02.data.model.LocationPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(locationPoint: LocationPoint)

    @Update
    suspend fun update(locationPoint: LocationPoint)

    @Delete
    suspend fun delete(locationPoint: LocationPoint)

    @Query("SELECT COUNT(*) FROM location_point WHERE type = :type")
    fun findPointCountByType(type: String): Flow<Long>
}