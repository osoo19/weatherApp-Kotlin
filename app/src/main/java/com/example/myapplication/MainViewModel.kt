package com.example.myapplication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainViewModel : ViewModel() {

    // 結果を保持
    private var weatherData: String? = null

    fun getWeatherData(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        location: String,
        callback: VolleyCallback
    ) {
        val apiUrl = context.getString(R.string.five_day_weather_url)

        // パラメーターをクエリ文字列に追加
        val fullUrl: String = if (location == "現在地") {
            "$apiUrl?APPID=${context.getString(R.string.api_key)}&lat=$latitude&lon=$longitude&units=metric&lang=ja"
        } else {
            "$apiUrl?APPID=${context.getString(R.string.api_key)}&q=$location&units=metric&lang=ja"
        }

        // Volley リクエストを非同期で実行
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Volley リクエスト
                    makeVolleyRequest(context, fullUrl)
                }
                // 結果をセット
                weatherData = response
                // 成功時のコールバック
                callback.onSuccess(response)
            } catch (e: Exception) {
                // エラー時のコールバック
                callback.onError(e.message)
            }
        }
    }

    // 通信結果を取得するメソッド
    fun getWeatherDataResult(): String? {
        return weatherData
    }

    private suspend fun makeVolleyRequest(context: Context, url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    // 成功
                    continuation.resume(response)
                },
                { error ->
                    // エラー
                    continuation.resumeWithException(error)
                })

            // リクエストをキューに追加
            queue.add(stringRequest)

            // キャンセル時にリクエストをキャンセル
            continuation.invokeOnCancellation {
                stringRequest.cancel()
            }
        }
    }

    interface VolleyCallback {
        fun onSuccess(result: String)
        fun onError(error: String?)
    }
}
