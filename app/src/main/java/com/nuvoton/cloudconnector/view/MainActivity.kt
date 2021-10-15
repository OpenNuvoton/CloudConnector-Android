package com.nuvoton.cloudconnector.view

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.jakewharton.rxbinding3.view.touches
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.nuvoton.cloudconnector.R
import com.nuvoton.cloudconnector.debug
import com.nuvoton.cloudconnector.disposeBy
import com.nuvoton.cloudconnector.lineChart
import com.nuvoton.cloudconnector.viewmodel.MainViewModel
import com.nuvoton.cloudconnector.viewmodel.RepoOption
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.sdk27.coroutines.onTouch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    lateinit var mainViewModel: MainViewModel
    private var awsButton: Button? = null
    private var pelionButton: Button? = null
    private var aliyunButton: Button? = null
    private var test = 0
    private var lineChart: LineChart? = null //畫畫
    private lateinit var awsDataSet: LineDataSet
    private lateinit var aliyunDataSet: LineDataSet
    private lateinit var pelionDataSet: LineDataSet
    private var timestamp = "0"
    private var zoomRatio = 1f
    private var trashCan = CompositeDisposable()
    private var ready = false
    private var isAllMode = true
    private var version = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectAppCenter() //更新

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        try {
            val pInfo: PackageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0)
            version = "version: " + pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        createView()
        initDataSets()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    //點擊右上角ＢＵＴＴＯＢ
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> if (ready) startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        ready = false
        thread {
            Thread.sleep(3000)
            ready = true
        }
        mainViewModel = MainViewModel(applicationContext)

        this.subscribeSubjects(mainViewModel.mvAWSSubject, awsDataSet)  //註冊ＡＷＳ收到訊息時 執行送出
        this.subscribeSubjects(mainViewModel.mvPelionSubject, pelionDataSet)
        this.subscribeSubjects(mainViewModel.mvAliyunSubject, aliyunDataSet)

        subscribeStatusSubjects(
            mainViewModel.mvAWSStatusSubject,
            awsButton,R.drawable.button_red_up
        )
        subscribeStatusSubjects(
            mainViewModel.mvPelionStatusSubject,
            pelionButton,R.drawable.button_blue_up
        )
        subscribeStatusSubjects(
            mainViewModel.mvAliyunStatusSubject,
            aliyunButton,R.drawable.button_green_up
        )
//        startSweepingDatasets()
        mainViewModel.updateSettings(applicationContext)
        mainViewModel.start()

//        lineChart?.moveViewToX(0f)
//        zoomRatio = 1f
    }

    override fun onPause() {
        super.onPause()
//        awsDataSet.clear()
//        pelionDataSet.clear()
//        aliyunDataSet.clear()
        mainViewModel.pause()
        trashCan.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        trashCan.dispose()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainViewModel.destroy()
    }

    private fun openDialog(option: RepoOption) {
        val list = mainViewModel.getCloudSetting(option)
        MaterialDialog(this).show {
            listItems(items = list)
        }
    }

    private fun startSweepingDatasets() {
        Observable.interval(3, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
            .map {
                val limit = timestamp.toFloat() - 20f

                while (awsDataSet.entryCount > 0 && awsDataSet.getEntryForIndex(0).x < limit)
                    awsDataSet.removeFirst()
                while (pelionDataSet.entryCount > 0 && pelionDataSet.getEntryForIndex(0).x < limit)
                    pelionDataSet.removeFirst()
                while (aliyunDataSet.entryCount > 0 && aliyunDataSet.getEntryForIndex(0).x < limit)
                    aliyunDataSet.removeFirst()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                lineChart?.data?.notifyDataChanged()
                lineChart?.notifyDataSetChanged()
                lineChart?.invalidate()
            }, {
                debug("Sweep Error")
                it.printStackTrace()
            })
    }

    //切換
//    private fun switchRepo(dataSet: LineDataSet) {
//        if (lineChart?.data?.contains(dataSet) == true) {
//            lineChart?.data?.removeDataSet(dataSet)
//        } else {
//            lineChart?.data?.addDataSet(dataSet)
//        }
//        lineChart?.data?.notifyDataChanged()
//        lineChart?.notifyDataSetChanged()
//        lineChart?.invalidate()
//    }

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

    private fun subscribeStatusSubjects(
        subject: PublishSubject<Boolean>,
        button: Button?,
        resUpId: Int
    ) {
        subject.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                changeButtonStatus(button, it, resUpId)
            }, {
                it.printStackTrace()
            }).disposeBy(trashCan)
    }

    private fun changeButtonStatus(button: Button?, isAlive: Boolean, resUpId: Int) {
        button?.isEnabled = isAlive
        if (isAlive) {
            button?.setBackgroundResource(resUpId)
        }else {
            button?.setBackgroundResource(R.drawable.button_gray_up_1)
        }
    }

    private fun initDataSets() {
        awsDataSet = LineDataSet(arrayListOf<Entry>(), "AWS")
        awsDataSet.color = Color.RED
        awsDataSet.setCircleColor(Color.RED)
        awsDataSet.lineWidth = 3f
//        switchRepo(awsDataSet)
        lineChart?.data?.addDataSet(awsDataSet)

        pelionDataSet = LineDataSet(arrayListOf<Entry>(), "Pelion")
        pelionDataSet.color = Color.BLUE
        pelionDataSet.setCircleColor(Color.BLUE)
        pelionDataSet.lineWidth = 3f
//        switchRepo(pelionDataSet)
        lineChart?.data?.addDataSet(pelionDataSet)

        aliyunDataSet = LineDataSet(arrayListOf<Entry>(), "ALiYun")
        aliyunDataSet.color = Color.GREEN
        aliyunDataSet.setCircleColor(Color.GREEN)
        aliyunDataSet.lineWidth = 3f
//        switchRepo(aliyunDataSet)
        lineChart?.data?.addDataSet(aliyunDataSet)
    }

    private fun subscribeSubjects(subject: PublishSubject<Map<String, Any?>>,lineDataSet: LineDataSet) {
        subject.subscribeOn(Schedulers.io())
            .map {
                val value = it["temperature"]?.toString()
                timestamp = it["timestamp"]?.toString() ?: "0"
                if (value != null) {

//                    lineDataSet.addEntry(Entry(timestamp.toFloat(), value.toFloat()))
//                    lineDataSet.addEntry(Entry(lineDataSet.entryCount.toFloat()+1, value.toFloat()))
                    this.addEntry(lineDataSet, value.toFloat())
                    Log.i(
                        "A",
                        "lineDataSet.entryCount:" + lineDataSet.entryCount + "   timestamp.toFloat():" + timestamp.toFloat()
                    )
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
            .subscribe({
                Log.i("B", "lineDataSet.entryCount:" + lineDataSet.entryCount)
//                if (lineDataSet.entryCount >= 60) {
//                    zoomRatio = lineDataSet.entryCount.toFloat() / 60.toFloat()
//                    lineChart?.moveViewToX(lineDataSet.entryCount.toFloat() - 1)
//                    lineChart?.zoom(0f, 1f, 0f, 0f)
//                    lineChart?.zoom(zoomRatio, 1f, 0f, 0f)
//                }

                lineDataSet.notifyDataSetChanged()
                lineChart?.data?.notifyDataChanged()
                lineChart?.notifyDataSetChanged()
                lineChart?.invalidate()
            }, {
                it.printStackTrace()
            }).disposeBy(trashCan)
    }

    //檢查有無更新
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

                //TODO 按鈕事件
                button {
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up)
                    textSize = 15f
                    setPadding(0, 0, 0, 10)
                    val text = resources.getString(R.string.toggle_cloud_button, "ALL")
                    setText(text)
                    touches { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                Log.i("button", "ALL ACTION_DOWN")
                                setBackgroundResource(R.drawable.button_gray_up_1)
                                switchChart("ALL")
                            }
                            MotionEvent.ACTION_UP -> {
                                Log.i("button", "ALL ACTION_UP")
                                setBackgroundResource(R.drawable.button_gray_up)
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

                //TODO 按鈕事件
                button {
                    this@MainActivity.awsButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up_1)
                    textColor = Color.WHITE
                    textSize = 15f
                    setPadding(0, 0, 0, 10)
                    val text = resources.getString(R.string.toggle_cloud_button, "AWS")
                    setText(text)
                    touches { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_red_down)
//                                this@MainActivity.switchRepo(awsDataSet)
                                switchChart("AWS")
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
//                    pelionButton!!.visibility = View.GONE
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up_1)
                    textColor = Color.WHITE
                    textSize = 15f
                    setPadding(0, 0, 0, 10)
                    val text = resources.getString(R.string.toggle_cloud_button, "Pelion")
                    setText(text)
                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_blue_down)
//                                this@MainActivity.switchRepo(pelionDataSet)
                                switchChart("Pelion")
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

                button {
                    this@MainActivity.aliyunButton = this
                    id = View.generateViewId()
                    setBackgroundResource(R.drawable.button_gray_up_1)
                    textColor = Color.WHITE
                    textSize = 15f

                    val text = resources.getString(R.string.toggle_cloud_button, "Aliyun")
                    setText(text)
                    onTouch { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                setBackgroundResource(R.drawable.button_green_down)
                                switchChart("ALiYun")
//                                this@MainActivity.switchRepo(aliyunDataSet)
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
            }.lparams(width = matchParent, height = 150) {
                topToTop = rootView.id
                startToStart = rootView.id
                endToEnd = rootView.id
                setMargins(0, 8, 0, 8)
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
                des.text = version +"         " + "\u2103"
                des.textSize = 15f
                description = des
            }.lparams(width = matchParent, height = 0) {
                topToBottom = tab.id
                bottomToTop = bottomGap.id
                startToStart = rootView.id
                endToEnd = rootView.id
            }

            // x坐标轴
            // x坐标轴
            val xl: XAxis = lineChart!!.getXAxis()
            xl.textColor = Color.WHITE

            //设置为true，则绘制网格线
            xl.setDrawGridLines(true)

            xl.setAvoidFirstLastClipping(true)
            xl.setSpaceMax(1.0f);
            // 如果false，那么x坐标轴将不可见
            xl.isEnabled = true
            // 将X坐标轴放置在底部，默认是在顶部。
            xl.position = XAxisPosition.BOTTOM
        }
    }

    // 添加进去一个坐标点
    private fun addEntry(lds: LineDataSet, value: Float) {

        val data: LineData = lineChart!!.getData()

        // 先添加一个x坐标轴的值
        // 因为是从0开始，data.getXValCount()每次返回的总是全部x坐标轴上总数量，所以不必多此一举的加1
//        data.addXValue(data.getXValCount().toString() + "")

        // 生成随机测试数
//        val f = (Math.random() * 20 + 50).toFloat()

        // set.getEntryCount()获得的是所有统计图表上的数据点总量，
        // 如从0开始一样的数组下标，那么不必多次一举的加1
        val entry = Entry(data.entryCount.toFloat(), value)

        // 往linedata里面添加点。注意：addentry的第二个参数即代表折线的下标索引。
        // 因为本例只有一个统计折线，那么就是第一个，其下标为0.
        // 如果同一张统计图表中存在若干条统计折线，那么必须分清是针对哪一条（依据下标索引）统计折线添加。
        var index = 0
        when (lds.label) {
            "AWS" -> index = 0
            "Pelion" -> index = 1
            "ALiYun" -> index = 2
        }
        data.addEntry(entry, index)

        // 像ListView那样的通知数据更新
        lineChart!!.notifyDataSetChanged()

        // y坐标轴线最大值
        // mChart.setVisibleYRange(30, AxisDependency.LEFT);

        if(isAllMode == true){
            // 当前统计图表中最多在x轴坐标线上显示的总量
            lineChart!!.setVisibleXRangeMaximum(20f)
            // 将坐标移动到最新
            // 此代码将刷新图表的绘图
            lineChart!!.moveViewToX((data.entryCount - 20).toFloat())

        }else{
            lineChart!!.setVisibleXRangeMaximum(data.entryCount.toFloat())
//            zoomRatio = lds.entryCount.toFloat() / 60.toFloat()
            lineChart?.moveViewToX(lds.entryCount.toFloat() - 1)
            lineChart?.zoom(0f, 1f, 0f, 0f)
//            lineChart?.zoom(zoomRatio, 1f, 0f, 0f)
        }

        // mChart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
    }

    private fun switchChart(label:String) {

        when (label) {
            "AWS" -> {
                awsDataSet.isVisible = true
                pelionDataSet.isVisible = false
                aliyunDataSet.isVisible = false
                awsDataSet.setDrawValues(false);//在点上显示数值 默认true
                awsDataSet.setDrawCircles(false);//在点上画圆 默认true
                lineChart!!.notifyDataSetChanged()
                isAllMode = false

            }
            "Pelion" -> {
                awsDataSet.isVisible = false
                pelionDataSet.isVisible = true
                aliyunDataSet.isVisible = false
                pelionDataSet.setDrawValues(false);//在点上显示数值 默认true
                pelionDataSet.setDrawCircles(false);//在点上画圆 默认true
                lineChart!!.notifyDataSetChanged()
                isAllMode = false
            }
            "ALiYun" -> {
                awsDataSet.isVisible = false
                pelionDataSet.isVisible = false
                aliyunDataSet.isVisible = true
                aliyunDataSet.setDrawValues(false);//在点上显示数值 默认true
                aliyunDataSet.setDrawCircles(false);//在点上画圆 默认true
                lineChart!!.notifyDataSetChanged()
                isAllMode = false
            }
            else -> {
                awsDataSet.isVisible = true
                pelionDataSet.isVisible = true
                aliyunDataSet.isVisible = true
                awsDataSet.setDrawValues(true);//在点上显示数值 默认true
                awsDataSet.setDrawCircles(true);//在点上画圆 默认true
                pelionDataSet.setDrawValues(true);//在点上显示数值 默认true
                pelionDataSet.setDrawCircles(true);//在点上画圆 默认true
                aliyunDataSet.setDrawValues(true);//在点上显示数值 默认true
                aliyunDataSet.setDrawCircles(true);//在点上画圆 默认true
                lineChart!!.notifyDataSetChanged()
                isAllMode = true
            }
        }

    }
}
