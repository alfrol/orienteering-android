package ee.taltech.alfrol.hw02.data.repositories

import ee.taltech.alfrol.hw02.data.dao.SessionDao
import ee.taltech.alfrol.hw02.data.model.Session
import javax.inject.Inject

class SessionRepository @Inject constructor(private val sessionDao: SessionDao) {

    suspend fun insertSession(session: Session) = sessionDao.insert(session)

    suspend fun updateSession(session: Session) = sessionDao.update(session)

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)

    fun findAllSortedByRecordedAt() = sessionDao.findAllSortedByRecordedAt()

    fun findAllSortedByDistance() = sessionDao.findAllSortedByDistance()

    fun findAllSortedByDuration() = sessionDao.findAllSortedByDuration()

    fun findAllSortedByPace() = sessionDao.findAllSortedByPace()

    fun findByExternalId(externalId: String) = sessionDao.findByExternalId(externalId)

    fun findByIdWithLocationPoints(id: Long) = sessionDao.findByIdWithLocationPoints(id)

    fun getTotalDistance() = sessionDao.getTotalDistance()

    fun getAverageDistance() = sessionDao.getAverageDistance()

    fun getAverageDuration() = sessionDao.getAverageDuration()

    fun getAveragePace() = sessionDao.getAveragePace()
}