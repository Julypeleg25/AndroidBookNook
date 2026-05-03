package com.booknook.app.model.cloudinary

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudinaryClient {
    private val client = OkHttpClient.Builder().build()

    fun createApi(cloudName: String): CloudinaryApi {
        return Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/$cloudName/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(CloudinaryApi::class.java)
    }
}
