package app.candash.cluster

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @Provides
    fun providePandaService(sharedPreferences: SharedPreferences, @ApplicationContext context: Context) : PandaService {
        return PandaService(sharedPreferences, context)
    }

    @Singleton
    @Provides
    fun provideMockPandaService() : MockCANService {
        return MockCANService()
    }

    @Singleton
    @Provides
    fun provideOkhttpClient() : OkHttpClient {
        return OkHttpClient.Builder()
            .pingInterval(3, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context) : SharedPreferences {
        return context.getSharedPreferences("dash", Context.MODE_PRIVATE)
    }
}