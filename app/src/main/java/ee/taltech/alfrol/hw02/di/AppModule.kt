package ee.taltech.alfrol.hw02.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ee.taltech.alfrol.hw02.api.RestHandler
import ee.taltech.alfrol.hw02.data.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application) =
        Room.databaseBuilder(app, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase) = db.userDao()

    @Provides
    fun provideSessionDao(db: AppDatabase) = db.sessionDao()

    @Provides
    fun provideLocationPointDao(db: AppDatabase) = db.locationPointDao()

    @Provides
    @Singleton
    fun providesRestHandler(app: Application) = RestHandler(app)
}