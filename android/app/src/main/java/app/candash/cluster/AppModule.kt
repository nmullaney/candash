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
    fun providePandaServiceImpl(sharedPreferences: SharedPreferences, @ApplicationContext context: Context) : PandaServiceImpl {
        return PandaServiceImpl(sharedPreferences, context)
    }

    @Singleton
    @Provides
    fun provideMockPandaService() : MockPandaService {
        return MockPandaService()
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
        return context.getSharedPreferences("pref", Context.MODE_PRIVATE)
    }
}