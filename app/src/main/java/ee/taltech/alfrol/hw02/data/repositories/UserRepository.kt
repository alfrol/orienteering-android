package ee.taltech.alfrol.hw02.data.repositories

import ee.taltech.alfrol.hw02.data.dao.UserDao
import ee.taltech.alfrol.hw02.data.model.User
import javax.inject.Inject

class UserRepository @Inject constructor(private val userDao: UserDao) {

    suspend fun insertUser(user: User) = userDao.insert(user)

    suspend fun updateUser(user: User) = userDao.update(user)

    suspend fun deleteUser(user: User) = userDao.delete(user)

    fun findByEmail(email: String) = userDao.findByEmail(email)
}