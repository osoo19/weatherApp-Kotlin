package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    val TAG = "weatherAPP"
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable(
                        route = "forecast/{location}",
                        arguments = listOf(navArgument("location") { type = NavType.StringType })
                    ) { entry ->
                        ForecastScreen(navController)
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
    fun ForecastScreen(navController: NavController) {
        val result = viewModel.getWeatherDataResult()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            if (result != null) {
                Text(text = result)
            }
        }

    }

    private fun getWeatherDataAndNavigate(navController: NavController, location: String) {
        viewModel.getWeatherData(
            context = this,
            latitude = null,
            longitude = null,
            location = location,
            callback = object : MainViewModel.VolleyCallback {
                override fun onSuccess(result: String) {
                    Log.d(TAG, "API Response: $result")
                    navController.navigate("forecast/$location")
                }

                override fun onError(error: String?) {
                    Log.e(TAG, "API Error: $error")
                    // エラー処理
                }
            }
        )
    }


}

