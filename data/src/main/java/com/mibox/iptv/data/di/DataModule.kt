package com.mibox.iptv.data.di

import android.content.Context
import androidx.room.Room
import com.mibox.iptv.core.DefaultDispatcherProvider
import com.mibox.iptv.core.DispatcherProvider
import com.mibox.iptv.data.local.IptvDatabase
import com.mibox.iptv.data.local.dao.CategoryDao
import com.mibox.iptv.data.local.dao.ChannelDao
import com.mibox.iptv.data.local.dao.EpgDao
import com.mibox.iptv.data.local.dao.SourceDao
import com.mibox.iptv.data.parser.M3uParser
import com.mibox.iptv.data.parser.XmltvParser
import com.mibox.iptv.data.repository.EpgRepositoryImpl
import com.mibox.iptv.data.repository.PlaylistRepositoryImpl
import com.mibox.iptv.domain.repository.EpgRepository
import com.mibox.iptv.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): IptvDatabase =
        Room.databaseBuilder(ctx, IptvDatabase::class.java, IptvDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSourceDao(db: IptvDatabase): SourceDao = db.sourceDao()
    @Provides fun provideCategoryDao(db: IptvDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideChannelDao(db: IptvDatabase): ChannelDao = db.channelDao()
    @Provides fun provideEpgDao(db: IptvDatabase): EpgDao = db.epgDao()

    @Provides @Singleton fun provideM3uParser() = M3uParser()
    @Provides @Singleton fun provideXmltvParser() = XmltvParser()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository
}
