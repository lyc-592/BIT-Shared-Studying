package com.example.bitshared.di

import com.example.bitshared.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        // 1. 创建日志拦截器
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. 创建 OkHttpClient 并加入拦截器
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // 3. 构建 Retrofit
        return Retrofit.Builder()
            // ！！重点：请在浏览器访问一下，确认路径是否真的包含 api/ ！！
            .baseUrl("http://47.94.122.20:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}