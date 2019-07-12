package com.nuvoton.cloudconnector.view

import android.content.pm.ActivityInfo
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.nuvoton.cloudconnector.R
import com.nuvoton.cloudconnector.lineChart
import com.nuvoton.cloudconnector.viewmodel.MainViewModel
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.sdk27.coroutines.onTouch

import com.jakewharton.rxbinding3.view.touches
import com.nuvoton.cloudconnector.debug
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.AppCenter

class MainActivity : AppCompatActivity() {
    lateinit var mainViewModel: MainViewModel
    private var awsButton: Button? = null
    private var pelionButton: Button? = null
    private var aliyunButton: Button? = null
    private var test = 0
    private var lineChart: LineChart? = null
    private lateinit var awsDataSet: LineDataSet
    private lateinit var aliyunDataSet: LineDataSet
    private lateinit var pelionDataSet: LineDataSet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectAppCenter()

        mainViewModel = MainViewModel(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        createView()
        initDataSets()

        subscribeSubjects(mainViewModel.mvAWSSubject, awsDataSet)
        subscribeSubjects(mainViewModel.mvPelionSubject, pelionDataSet)
        subscribeSubjects(mainViewModel.mvAliyunSubject, aliyunDataSet)

        subscribeStatusSubjects(mainViewModel.mvAWSStatusSubject, awsButton, R.drawable.button_red_up)
        subscribeStatusSubjects(mainViewModel.mvPelionStatusSubject, pelionButton, R.drawable.button_green_up)
        subscribeStatusSubjects(mainViewModel.mvAliyunStatusSubject, aliyunButton, R.drawable.button_blue_up)
    }

    private fun switchRepo(dataSet: LineDataSet) {
        if (lineChart?.data?.contains(dataSet) == true) {
            lineChart?.data?.removeDataSet(dataSet)
        } else {
            lineChart?.data?.addDataSet(dataSet)
        }
        lineChart?.data?.notifyDataChanged()
        lineChart?.notifyDataSetChanged()
        lineChart?.invalidate()
    }

    fun testUpdateChart(value: Float) {
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

    override fun onStart() {
        super.onStart()
        mainViewModel.start()
    }

    override fun onPause() {
        super.onPause()
        mainViewModel.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainViewModel.destroy()
    }

    private fun subscribeStatusSubjects(subject: PublishSubject<Boolean>, button: Button?, resUpId: Int) {
        subject.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                changeButtonStatus(button, it, resUpId)
            }, {
                it.printStackTrace()
            })
    }

    private fun changeButtonStatus(button: Button?, isAlive: Boolean, resUpId: Int) {
        button?.isEnabled = isAlive
        if (isAlive) {
            button?.setBackgroundResource(resUpId)
        }else {
            button?.setBackgroundResource(R.drawable.button_gray_up)
        }
    }

    private fun initDataSets() {
        awsDataSet = LineDataSet(arrayListOf(Entry(0f, 0f)), "AWS")
        awsDataSet.color = Color.RED
        awsDataSet.setCircleColor(Color.RED)
        switchRepo(awsDataSet)
        pelionDataSet = LineDataSet(arrayListOf(Entry(0f, 0f)), "Pelion")
        pelionDataSet.color = Color.GREEN
        pelionDataSet.setCircleColor(Color.GREEN)
        switchRepo(pelionDataSet)
        aliyunDataSet = LineDataSet(arrayListOf(Entry(0f, 0f)), "ALiYun")
        aliyunDataSet.color = Color.BLUE
        aliyunDataSet.setCircleColor(Color.BLUE)
        switchRepo(aliyunDataSet)
    }
    private fun subscribeSubjects(subject: PublishSubject<Map<String, Any?>>, lineDataSet: LineDataSet) {
        subject.subscribeOn(Schedulers.io())
            .map {
                val value = it["temperature"]?.toString()
                val position = it["timestamp"]?.toString()
                if (value != null) {
                    lineDataSet.addEntry(Entry(position!!.toFloat(), value.toFloat()))

                    val limit = position.toFloat() - 30f
                    while (lineDataSet.getEntryForIndex(0).x < limit)
                        lineDataSet.removeFirst()

                    val positionZero = lineDataSet.getEntryForIndex(0)
                    if (positionZero.y == 0f)
                        lineDataSet.removeFirst()
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                lineChart?.data?.notifyDataChanged()
                lineChart?.notifyDataSetChanged()
                lineChart?.invalidate()
            }, {
                it.printStackTrace()
            })
    }

    private fun connectAppCenter() {
        AppCenter.start(
            application, "a73adaa7-a2d1-4c01-a202-8cbb90a7f241",
            Analytics::class.java, Crashes::class.java
        )
    }

    // UI
    private fun createView() {
        constraintLayout {
            id = View.generateViewId()
            background = resources.getDrawable(R.drawable.background, theme)
            val tab = linearLayout {
                id = View.generateViewId()
                orientation = LinearLayout.HORIZONTAL

                button {
                    this@MainActivity.awsButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f
                    val text = resources.getString(R.string.toggle_cloud_button, "AWS")
                    setText(text)
                    touches { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_red_down)
                                this@MainActivity.switchRepo(awsDataSet)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_red_up)
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

                button {
                    this@MainActivity.pelionButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f
                    val text = resources.getString(R.string.toggle_cloud_button, "Pelion")
                    setText(text)
                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_green_down)
                                this@MainActivity.switchRepo(pelionDataSet)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_green_up)
                            }
                        }
                    }
                }.lparams(width = 0, height = matchParent) {
                    weight = 1f
                    margin = 8
                }

                button {
                    this@MainActivity.aliyunButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textColor = Color.WHITE
                    textSize = 20f
                    val text = resources.getString(R.string.toggle_cloud_button, "Aliyun")
                    setText(text)
                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_blue_down)
                                this@MainActivity.switchRepo(aliyunDataSet)
                            }
                            MotionEvent.ACTION_UP -> {
                                setBackgroundResource(R.drawable.button_blue_up)
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
                data = LineData()
                val des = Description()
                des.text = "\u2103"
                des.textSize = 20f
                description = des
            }.lparams(width = matchParent, height = 0) {
                topToBottom = tab.id
                bottomToTop = bottomGap.id
                startToStart = rootView.id
                endToEnd = rootView.id
            }
        }
    }
}
