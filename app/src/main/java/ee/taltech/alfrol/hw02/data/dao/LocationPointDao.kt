package ee.taltech.alfrol.hw02.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import ee.taltech.alfrol.hw02.data.model.LocationPoint

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(locationPoint: LocationPoint)

    @Update
    suspend fun update(locationPoint: LocationPoint)

    @Delete
    suspend fun delete(locationPoint: LocationPoint)
}