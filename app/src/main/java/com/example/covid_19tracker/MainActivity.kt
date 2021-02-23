package com.example.covid_19tracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.covid_19tracker.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private lateinit var binding: ActivityMainBinding
private const val BASE_URL = "https://api.covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<Unit, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getNationalData().enqueue(object: Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val nationalData = response.body()
                if (nationalData == null){
                    Log.w(TAG,"Did not receive a valid response body")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG,"Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }
        })

        covidService.getNationalData().enqueue(object: Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val stateData = response.body()
                if (stateData == null){
                    Log.w(TAG,"Did not receive a valid response body")
                    return
                }
                perStateDailyData = stateData.reversed().groupBy { it.state }
                Log.i(TAG,"Update spinner with state data")
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }
        })

    }

    private fun setupEventListeners() {
        // Add a Listener for user scrubbing on the chart
        binding.sparkView
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData ->
            if (itemData is CovidData){
                updateInfoForDate(itemData)
            }
        }
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // Create a SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter
        // Update radio buttons to select the positive cases and max time by default
        binding.radioButtonPositive.isChecked = true
        binding.radioButtonMax.isChecked = true
        // Display metric for the most recent data
        updateInfoForDate(dailyData.last())
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adapter.metric){
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outPutDateFormat = SimpleDateFormat("MM dd, yyyy", Locale.UK)
        binding.tvDateLabel.text = outPutDateFormat.format(covidData.dateChecked)
    }
}