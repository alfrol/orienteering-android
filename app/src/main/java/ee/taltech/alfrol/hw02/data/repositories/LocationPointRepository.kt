package ee.taltech.alfrol.hw02.data.repositories

import ee.taltech.alfrol.hw02.data.dao.LocationPointDao
import ee.taltech.alfrol.hw02.data.model.LocationPoint
import javax.inject.Inject

class LocationPointRepository @Inject constructor(private val locationPointDao: LocationPointDao) {

    suspend fun insertLocationPoint(locationPoint: LocationPoint) =
        locationPointDao.insert(locationPoint)

    suspend fun insertAllLocationPoints(locationPoints: List<LocationPoint>) =
        locationPoints.forEach { insertLocationPoint(it) }

    suspend fun updateLocationPoint(locationPoint: LocationPoint) =
        locationPointDao.update(locationPoint)

    suspend fun deleteLocationPoints(locationPoint: LocationPoint) =
        locationPointDao.delete(locationPoint)
}