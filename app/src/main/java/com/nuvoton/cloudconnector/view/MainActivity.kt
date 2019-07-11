package com.nuvoton.cloudconnector.view

import android.content.pm.ActivityInfo
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.jakewharton.rxbinding3.view.clicks
import com.nuvoton.cloudconnector.R
import com.nuvoton.cloudconnector.debug
import com.nuvoton.cloudconnector.lineChart
import com.nuvoton.cloudconnector.viewmodel.MainViewModel
import com.nuvoton.cloudconnector.viewmodel.RepoOption
import com.nuvoton.cloudconnector.viewmodel.RepoOption.*
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.sdk27.coroutines.onTouch
import java.nio.Buffer

import com.jakewharton.rxbinding3.view.touches
import com.uber.autodispose.autoDisposable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var mainViewModel: MainViewModel
    private var awsButton: Button? = null
    private var pelionButton: Button? = null
    private var aliyunButton: Button? = null
    private var test = 0
    private var lineChart : LineChart? = null
    private var lineDataSet: LineDataSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainViewModel = MainViewModel(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // TODO: update line chart
        createView()
        mainViewModel.mvRepoStatusSubject.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe {
                when (it.option) {
                    AWS -> changeButtonStatus(awsButton!!, it.isAlive)
                    PELION -> changeButtonStatus(pelionButton!!, it.isAlive)
                    ALIYUN -> changeButtonStatus(aliyunButton!!, it.isAlive)
                }
            }

        mainViewModel.mvDataUpdateSubject.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                // 暫時只取第一個VALUE用來更新圖表，並非最終版
                val value = if (it.isNotEmpty()) {
                    val list = ArrayList(it.values)
                    list[0].toString()
                } else
                    "0"
                updateChart(value.toFloat())
//                debug("mvDataUpdateSubject message received, ${test++} = $it")
            }, {
                it.printStackTrace()
            })
    }

    fun updateChart(value: Float) {
        if (lineChart != null) {
            val lineData = lineChart!!.data
            val dataset = lineData.dataSets[0]
            val x = dataset.entryCount
            dataset.addEntry(Entry(x.toFloat(), value))
            dataset.removeFirst()
            lineData.notifyDataChanged()
            lineChart!!.notifyDataSetChanged()
            lineChart!!.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainViewModel.destroy()
    }

    private fun changeButtonStatus(button: Button, isAlive: Boolean) {
        button.isEnabled = isAlive
        if (isAlive) {
            button.setBackgroundResource(R.drawable.button_up)
        }else {
            button.setBackgroundResource(R.drawable.button_gray_up)
        }
    }

    private fun createView() {
        constraintLayout {
            id = View.generateViewId()
            background = resources.getDrawable(R.drawable.background, theme)
            val tab = linearLayout {
                id = View.generateViewId()
                orientation = LinearLayout.HORIZONTAL

                button("AWS") {
                    this@MainActivity.awsButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f
                    touches { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_down)
                                this@MainActivity.mainViewModel.setupCloud(AWS)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_up)
                            }
                        }
                        false
                    }.debounce(2, TimeUnit.SECONDS)
                        .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this@MainActivity)))
                        .subscribe()
                }.lparams(width = 0, height = matchParent) {
                    weight = 1f
                    margin = 8
                }

                button("Pelion") {
                    this@MainActivity.pelionButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f
                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_down)
                                this@MainActivity.mainViewModel.setupCloud(PELION)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_up)
                            }
                        }
                    }
                }.lparams(width = 0, height = matchParent) {
                    weight = 1f
                    margin = 8
                }

                button("Aliyun") {
                    this@MainActivity.aliyunButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f

                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                this@MainActivity.mainViewModel.setupCloud(ALIYUN)
                                setBackgroundResource(R.drawable.button_down)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_up)
                            }
                        }
                    }
                }.lparams(width = 0, height = matchParent) {
                    weight = 1f
                    margin = 8
                }
            }.lparams(width = matchParent, height = 150) {
                topToTop = rootView.id
                startToStart = rootView.id
                endToEnd = rootView.id
            }

            val bottomGap = view {
                id = View.generateViewId()
                backgroundColor = Color.TRANSPARENT
            }.lparams(width = matchParent, height = 100) {
                bottomToBottom = rootView.id
                startToStart = rootView.id
                endToEnd = rootView.id
            }

            lineChart = lineChart {
                id = View.generateViewId()
                backgroundColor = Color.TRANSPARENT
                val arraylist = arrayListOf<Entry>()
                for (i in 0 until 10) {
                    val ran = Math.random()*100.0.toInt() % 30
                    arraylist.add(Entry(i.toFloat(), ran.toFloat()))
                }
                lineDataSet = LineDataSet(arraylist, "TEST Dataset")
                val dataset = arrayListOf<ILineDataSet>(lineDataSet!!)
                val tempdata = LineData(dataset)
                data = tempdata
            }.lparams(width = matchParent, height = 0) {
                topToBottom = tab.id
                bottomToTop = bottomGap.id
                startToStart = rootView.id
                endToEnd = rootView.id
            }
        }
    }
}
