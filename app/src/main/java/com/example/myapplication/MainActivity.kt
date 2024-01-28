package com.example.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.accompanist.glide.rememberGlidePainter

class MainActivity : ComponentActivity() {
    val TAG = "weatherAPP"
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // 位置情報許可をリクエスト
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }


        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable(
                        //  天気予報表示画面
                        route = "forecast/{location}",
                        arguments = listOf(navArgument("location") { type = NavType.StringType })
                    ) { entry ->
                        ForecastScreen(navController, entry.arguments?.getString("location"))
                    }
                    composable(
                        //  リトライ画面
                        route = "retry/{location}",
                        arguments = listOf(navArgument("location") { type = NavType.StringType })
                    ) { entry ->
                        RetryScreen(navController, entry.arguments?.getString("location"))
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(navController: NavHostController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SearchButton("東京") {
                getWeatherDataAndNavigate(navController, "Tokyo")
            }

            SearchButton("兵庫") {
                getWeatherDataAndNavigate(navController, "Hyogo")
            }

            SearchButton("大分") {
                getWeatherDataAndNavigate(navController, "Oita")
            }

            SearchButton("北海道") {
                getWeatherDataAndNavigate(navController, "Hokkaido")
            }
            SearchButton("現在地") {
                getWeatherDataAndNavigate(navController, "現在地")
            }
        }
    }

    @Composable
    fun SearchButton(location: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = location)
        }
    }

    @Composable
    fun ForecastScreen(navController: NavController, location: String?) {
        //  天気予報表示画面
        val result = viewModel.getWeatherDataResult()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            location?.let { Text(text = it + "の天気予報") }
            if (result != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(result) { weatherItem ->
                        WeatherListItem(weatherItem)
                    }
                }
            }
        }
    }

    @Composable
    fun RetryScreen(navController: NavController, location: String?) {
        //  リトライ画面
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "通信に失敗しました。")
            if (location != null) {
                Button(
                    onClick = {
                        getWeatherDataAndNavigate(navController, location)
                    },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = "リトライ")
                }
            }
        }
    }


    private fun getWeatherDataAndNavigate(navController: NavController, location: String) {
        //  予報データを取得して画面遷移
        viewModel.getWeatherData(
            context = this,
            location = location,
            callback = object : MainViewModel.VolleyCallback {
                override fun onSuccess(result: List<WeatherItem>) {
                    navController.navigate("forecast/$location")
                }

                override fun onError(error: String?) {
                    // エラーの場合リトライ画面へ
                    navController.navigate("retry/$location")

                }
            }
        )
    }

    @Composable
    fun WeatherListItem(weatherItem: WeatherItem) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val iconUrl =
                "https://openweathermap.org/img/wn/${weatherItem.weather.firstOrNull()?.icon}@2x.png"
            val painter = rememberGlidePainter(request = iconUrl, fadeIn = true)

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "気温: ${weatherItem.main.temp} °C",
                modifier = Modifier.padding(8.dp)
            )


            Text(
                text = "日時: ${weatherItem.dt_txt}",
                modifier = Modifier.padding(8.dp)
            )
        }
    }


}

