package ee.taltech.alfrol.hw02.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ee.taltech.alfrol.hw02.data.dao.SessionDao
import ee.taltech.alfrol.hw02.data.dao.UserDao
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.data.model.User

@Database(
    entities = [User::class, Session::class],
    version = AppDatabase.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "gps_sport_map_db"
    }

    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
}