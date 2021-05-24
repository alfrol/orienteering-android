package ee.taltech.alfrol.hw02.data.dao

import androidx.room.*
import ee.taltech.alfrol.hw02.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE email = :email")
    fun findByEmail(email: String): Flow<User>

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}