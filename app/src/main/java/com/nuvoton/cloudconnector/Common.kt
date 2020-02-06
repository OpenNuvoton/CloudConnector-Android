package com.nuvoton.cloudconnector

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewManager
import android.widget.Button
import androidx.preference.PreferenceManager
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.github.mikephil.charting.charts.LineChart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuvoton.cloudconnector.model.AWSRepo
import com.nuvoton.cloudconnector.model.RepositoryCommon
import com.nuvoton.cloudconnector.viewmodel.MainViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.Subject
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.lineChart(init: LineChart.() -> Unit) = ankoView( {LineChart(it)}, R.style.AppTheme, init)
inline fun <reified T> Gson.fromJsonString(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

fun Activity.debug(message: String) = Log.d(this.javaClass.simpleName, message)
fun AWSRepo.debug(message: String) = Log.d(this.javaClass.simpleName, message)
fun MainViewModel.debug(message: String) = Log.d(this.javaClass.simpleName, message)
fun RepositoryCommon.debug(message: String) = Log.d(this.javaClass.simpleName, message)

fun Disposable.disposeBy(trashCan: CompositeDisposable) = trashCan.add(this)

fun Activity.addPref(key: String, value: Any) {
    val defaultPref = PreferenceManager.getDefaultSharedPreferences(this)
    when(value) {
        is Boolean -> defaultPref.edit().putBoolean(key, value).apply()
        is String -> defaultPref.edit().putString(key, value).apply()
        is Int -> defaultPref.edit().putInt(key, value).apply()
        is Float -> defaultPref.edit().putFloat(key, value).apply()
        is Long -> defaultPref.edit().putLong(key, value).apply()
        else -> throw Exception("not support value type $value")
    }
}

fun Context.getPrefString(key: String) : String? {
    val defaultPref = PreferenceManager.getDefaultSharedPreferences(this)
    return defaultPref.getString(key, null)
}