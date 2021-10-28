package dev.nmullaney.tesladashboard

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @Provides
    fun providePandaService() : PandaService {
        return PandaService()
    }

    @Singleton
    @Provides
    fun provideOkhttpClient() : OkHttpClient {
        return OkHttpClient.Builder()
            .pingInterval(3, TimeUnit.SECONDS)
            .build()
    }
}