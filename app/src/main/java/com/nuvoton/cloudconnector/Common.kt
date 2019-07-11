package com.nuvoton.cloudconnector

import android.app.Activity
import android.util.Log
import android.view.ViewManager
import android.widget.Button
import com.github.mikephil.charting.charts.LineChart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvoton.cloudconnector.model.AWSRepo
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.lineChart(init: LineChart.() -> Unit) = ankoView( {LineChart(it)}, R.style.AppTheme, init)
inline fun <reified T> Gson.fromJsonString(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

inline fun Activity.debug(message: String) = Log.d(this.javaClass.simpleName, message)
inline fun AWSRepo.debug(message: String) = Log.d(this.javaClass.simpleName, message)


