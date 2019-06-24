package com.nuvoton.cloudconnector

import android.view.ViewManager
import com.github.mikephil.charting.charts.LineChart
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.lineChart(init: LineChart.() -> Unit) = ankoView( {LineChart(it)}, R.style.AppTheme, init)