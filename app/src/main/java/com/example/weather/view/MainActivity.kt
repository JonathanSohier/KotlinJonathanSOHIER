package com.example.weather.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.lifecycle.ViewModelProvider
import com.example.weather.R
import com.example.weather.database.CityRoomDatabase
import com.example.weather.databinding.ActivityMainBinding
import com.example.weather.entity.WeatherCityEntity
import com.example.weather.repository.WeatherCityRepository
import com.example.weather.retrofit.RetrofitBuilder
import com.example.weather.viewModel.WeatherViewModel
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var weatherviewModel: WeatherViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)

        weatherviewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        // Tout le code en dessous, ne doit pas exister dans le MainActivity, il doit être présent dans le ViewModel
        // Database Room
        val applicationScope = CoroutineScope(SupervisorJob())
        val database = CityRoomDatabase.getDatabase(this, applicationScope)
        val repository = WeatherCityRepository(database.cityDao())
        applicationScope.launch(Dispatchers.IO) {
            repository.insert(WeatherCityEntity("Test"))
        }

        // Retrofit Create instance and Get Request

        // Il faut utiliser un ViewModel pour faire le call api
        val retrofitBuilder = RetrofitBuilder.apiService
        CoroutineScope(Dispatchers.IO).launch {
            val responseWeather = retrofitBuilder.getWeather((44.851954).toFloat(), ( -0.572471).toFloat())
            withContext(Dispatchers.Main) {
                println(responseWeather);
                try {
                    if (responseWeather.isSuccessful) {
                        println("CALL API : ${responseWeather.body()}")
                        val temp = findViewById<TextView>(R.id.temperature_celsius_text_view)
                        println("")
                        // Récupère la date et l'heure actuelles
                        val currentDateTime = LocalDateTime.now()

                        // Formate la date et l'heure
                        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH")
                        val DateActuelle = currentDateTime.format(formatter)

                        // Affiche la date et l'heure formattées
                        println("Date et heure actuelles : $DateActuelle")
                        val Temp = responseWeather.body()?.hourly?.time?.size
                        if(Temp != null){
                            for ( i in 0..Temp){
                                val hour = responseWeather.body()?.hourly?.time?.get(i)
                                if (hour != null){
                                    val hourDate = LocalDateTime.parse(hour)
                                    val formattedDateTime = hourDate.format(formatter)
                                    if (DateActuelle == formattedDateTime){
                                        println("Date actuel : $hour")
                                        val température = responseWeather.body()?.hourly?.temperature_2m?.get(i)
                                        println("Température actuel : $température")
                                        val myTextView = findViewById<TextView>(R.id.temperature_celsius_text_view)
                                        myTextView.setText("Température actuel : $température")
                                        val températureApparent = responseWeather.body()?.hourly?.apparent_temperature?.get(i)
                                        println("Température ressentis : $température")
                                        val myTextView2 = findViewById<TextView>(R.id.feels_like_temperature_text_view)
                                        myTextView2.setText("Température ressentis : $températureApparent")
                                        val weatherCode = responseWeather.body()?.hourly?.weathercode?.get(i)
                                        println("Température ressentis : $weatherCode")
                                        val imageResource = when (weatherCode) {
                                            in 0..24 -> R.drawable.i1
                                            in 25..49 -> R.drawable.i2
                                            in 50..74 -> R.drawable.i3
                                            else -> R.drawable.i4
                                        }
                                        val weatherImageView = findViewById<ImageView>(R.id.weather_image_view);
                                        weatherImageView.setImageResource(imageResource)
                                    }
                                }
                            }
                        }

                    } else {
                        Toast.makeText(baseContext, "Error: ${responseWeather.code()}", Toast.LENGTH_LONG)
                    }
                } catch (e: HttpException) {
                    Toast.makeText(baseContext, "Exception: ${e.message()}", Toast.LENGTH_LONG)
                } catch (e: Throwable) {
                    Toast.makeText(baseContext, "Ooops: Something else went wrong", Toast.LENGTH_LONG)
                }
            }
        }
    }
}
