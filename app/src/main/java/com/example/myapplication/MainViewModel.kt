package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class MainViewModel : ViewModel() {

    // 結果を保持
    private var weatherData: List<WeatherItem>? = null

    fun getWeatherData(
        context: Context,
        location: String,
        callback: VolleyCallback
    ) {

        // キャッシュを確認
        val currentDate = getCurrentDate()
        val cacheKey = "weather_$location _ $currentDate"
        val cachedData = getFromCache(context = context, key = cacheKey)

        if (cachedData != null && cachedData.isNotBlank() && location != "現在地") {
            // キャッシュが存在する場合は返す
            weatherData = parseJsonToWeatherData(cachedData)

            // 成功時のコールバックにデータモデルを渡す
            weatherData?.let { callback.onSuccess(it) }
            return
        }

        val apiUrl = context.getString(R.string.five_day_weather_url)

        val fullUrl: String = if (location == "現在地") {
            // 現在地ボタンの場合、緯度と経度を取得
            val locationMap = requestLocation(context)
            val latitude = locationMap?.get("latitude")
            val longitude = locationMap?.get("longitude")

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
                // 結果をセット キャッシュに保存
                weatherData = parseJsonToWeatherData(response)
                saveToCache(context = context, key = cacheKey, data = response)

                // 成功時のコールバックにデータモデルを渡す
                weatherData?.let { callback.onSuccess(it) }

            } catch (e: Exception) {
                // エラー時のコールバック
                callback.onError(e.message)
            }
        }
    }

    // 通信結果を取得
    fun getWeatherDataResult(): List<WeatherItem>? {
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

    private fun parseJsonToWeatherData(jsonData: String): List<WeatherItem>? {
        val weatherData = Gson().fromJson(jsonData, WeatherData::class.java)
        return weatherData.list
    }

    // 位置情報を取得
    private fun requestLocation(context: Context): Map<String, Double>? {
        val locationMap = mutableMapOf<String, Double>()

        // パーミッションの確認
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastKnownLocation: Location? =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (lastKnownLocation != null) {
                locationMap["latitude"] = lastKnownLocation.latitude
                locationMap["longitude"] = lastKnownLocation.longitude
            } else {
                Log.e("weatherAPP", "Last known location is null")
            }
        } else {
            return null
        }
        return locationMap
    }

    private fun getCurrentDate(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")  // 任意のフォーマットを指定

        return currentDate.format(formatter)
    }

    // キャッシュ操作メソッド
    private fun saveToCache(context: Context, key: String, data: String) {
        val preferences = context.getSharedPreferences("WeatherCache", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(key, data)
        editor.apply()
    }

    private fun getFromCache(context: Context, key: String): String? {
        val preferences = context.getSharedPreferences("WeatherCache", Context.MODE_PRIVATE)
        return preferences.getString(key, null)
    }


    interface VolleyCallback {
        fun onSuccess(result: List<WeatherItem>)
        fun onError(error: String?)
    }
}
