package com.sample.app.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sample.app.BuildConfig
import com.sample.app.data.api.ApiService
import com.sample.app.data.repository.SampleRepository
import com.sample.app.domain.repository.ISampleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val TIME_OUT = 30L

@InstallIn(SingletonComponent::class)
@Module
object HiltInjectionModule {

  @Provides
  @Singleton
  fun provideOkHttpClient(
    @ApplicationContext context: Context,
    offlineInterceptor: OfflineInterceptor,
    httpLoggingInterceptor: HttpLoggingInterceptor
  ): OkHttpClient {
    val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
    val cache = Cache(context.cacheDir, cacheSize)
    return OkHttpClient.Builder().run {
      connectTimeout(TIME_OUT, TimeUnit.SECONDS)
      readTimeout(TIME_OUT, TimeUnit.SECONDS)
      addInterceptor(httpLoggingInterceptor)
      addInterceptor(offlineInterceptor)
      addNetworkInterceptor(onlineInterceptor)
      cache(cache)
      build()
    }
  }

  @Provides
  @Singleton
  fun provideLoggingInterceptor() = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  @Provides
  @Singleton
  fun provideOfflineInterceptor(@ApplicationContext context: Context) = OfflineInterceptor(context)

  class OfflineInterceptor(
    @ApplicationContext private val context: Context
  ) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      var request: Request = chain.request()
      if (!isInternetAvailable(context)) {
        val maxStale = 60 * 60 * 24 * 30 // Offline cache available for 30 days
        request = request.newBuilder().run {
          header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
          removeHeader("Pragma")
          build()
        }
      }
      return chain.proceed(request)
    }

  }

  @Provides
  @Singleton
  fun createRetrofit(okHttpClient: OkHttpClient): Retrofit =
    Retrofit.Builder().run {
      baseUrl(BuildConfig.BASE_URL)
      client(okHttpClient)
      addConverterFactory(GsonConverterFactory.create())
      build()
    }


  @Provides
  @Singleton
  fun createService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)
  }

  @Provides
  @Singleton
  fun createSampleRepository(apiService: ApiService): ISampleRepository {
    return SampleRepository(apiService)
  }

  var onlineInterceptor: Interceptor = object : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val response: Response = chain.proceed(chain.request())
      val maxAge = 60 // read from cache for 60 seconds even if there is internet connection
      return response.newBuilder()
        .header("Cache-Control", "public, max-age=$maxAge")
        .removeHeader("Pragma")
        .build()
    }
  }

  fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
    return when {
      actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
      actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
      actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
      else -> false
    }
  }
}
