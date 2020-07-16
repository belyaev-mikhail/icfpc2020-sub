package ru.spbstu

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection

fun main(args: Array<String>) {
    try {
        val (serverUrl, playerKey) = args

        println("ServerUrl: $serverUrl; PlayerKey: $playerKey")

        val client = OkHttpClient()

        val request = Request.Builder().url(serverUrl).post(playerKey.toRequestBody()).build()

        val response = client.newCall(request).execute()

        val status = response.code

        val body = response.body
        check(body != null)

        if (status != HttpURLConnection.HTTP_OK) {
            println("Unexpected server response:");
            println("HTTP code: " + status);

            println("Response body: " + body.string());
            System.exit(2);
        }

        println("Server response: " + body.string());
    } catch (e: Exception) {
        println("Unexpected server response:");
        e.printStackTrace(System.out);
        System.exit(1);
    }
}
