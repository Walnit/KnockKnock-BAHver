package com.walnit.knockknock.networking

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface SendMessagesToClient {
    @GET("knock/sendMessages/{receiver}/{sender}/{content}")
    fun sendMessage(@Path("receiver") receiverName: String, @Path("sender") senderName: String, @Path("content") content: String) : Call<ResponseBody>
}