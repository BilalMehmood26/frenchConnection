package com.usaclean.frenchconnectionuser.stripe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Controller {
    const val BASE_URL = "https://us-central1-frenchconnection-41ec9.cloudfunctions.net/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }

}