package com.keuangan.app.data.api

import com.keuangan.app.data.model.ActionResponse
import com.keuangan.app.data.model.BillPostPayload
import com.keuangan.app.data.model.BillsResponse
import com.keuangan.app.data.model.SheetResponse
import com.keuangan.app.data.model.TransactionPostPayload
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface GoogleSheetApiService {

    @GET
    suspend fun getTransactions(
        @Url fullUrl: String
    ): SheetResponse

    @POST
    suspend fun postTransaction(
        @Url fullUrl: String,
        @Body payload: TransactionPostPayload
    ): ActionResponse

    @GET
    suspend fun getBills(
        @Url fullUrl: String,
        @Query("view") view: String = "bills"
    ): BillsResponse

    @POST
    suspend fun postBill(
        @Url fullUrl: String,
        @Body payload: BillPostPayload
    ): ActionResponse
}

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private const val PLACEHOLDER_BASE_URL = "https://script.google.com/"

    val apiService: GoogleSheetApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetApiService::class.java)
    }
}
