package com.walnit.knockknock.networking

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface SendBundle {
    @GET("knock/sendBundle/{name}/{bundle}")
    fun sendBundle(@Path("name") name: String, @Path("bundle") bundle: String) : Call<ResponseBody>
}