package com.example.covid_19tracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {

    @GET("us/daily.json")
    fun getNationalData(): Call<List<CovidData>>

    @GET("us/daily.json")
    fun getStateData(): Call<List<CovidData>>
}