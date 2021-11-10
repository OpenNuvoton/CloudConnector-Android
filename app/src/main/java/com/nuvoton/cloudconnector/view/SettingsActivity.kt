package com.nuvoton.cloudconnector.view

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import com.nuvoton.cloudconnector.R
import com.nuvoton.cloudconnector.getPrefString
import org.jetbrains.anko.support.v4.alert

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val product_key: EditTextPreference? = this.findPreference("pref_aliyun_product_key") as EditTextPreference?
            val device_name: EditTextPreference? = this.findPreference("pref_aliyun_device_name") as EditTextPreference?
            val device_secret: EditTextPreference? = this.findPreference("pref_aliyun_device_secret") as EditTextPreference?

            val prefListThemes: ListPreference? = this.findPreference("pref_aliyun_region") as ListPreference?
            prefListThemes!!.setOnPreferenceChangeListener { preference, newValue ->
                Log.i("pref_aliyun_region","setOnPreferenceChangeListener:   "+newValue)
//                val regionString = context.getPrefString("pref_aliyun_region")
                if (newValue != null) {
//                    val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
//                    editor.putString("pref_aliyun_product_key", "a1n3OH5qaph") // You can pass you username value here
//                    editor.commit()
                    when (newValue){
                        "HQ_APP1"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "HQ_APP_1"
                            device_secret!!.text = "e8db2434944834a231f4eeca26d9b07a"
                        }
                        "HQ_APP2"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "HQ_APP_2"
                            device_secret!!.text = "f71d0618bd2dd4db269820e8ee6eaf67"
                        }
                        "SZ_APP1"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "SZ_APP_1"
                            device_secret!!.text = "29e2132ce9ca534c5ec7809259276633"
                        }
                        "SZ_APP2"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "SZ_APP_2"
                            device_secret!!.text = "0d57399458cde375805a312cf6003b17"
                        }
                        "SH_APP1"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "SH_APP_1"
                            device_secret!!.text = "35d74697fa8f6f3e26a162fccbc5abb5"
                        }
                        "SH_APP2"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "SH_APP_2"
                            device_secret!!.text = "e54aba7dcd2d2c01803db7bc4c04274f"
                        }
                        "HQ_DEV"->{
                            product_key!!.text = "a1n3OH5qaph"
                            device_name!!.text = "HQ_DEV"
                            device_secret!!.text = "34f09a2a8880b1e0bad52333ee44ad4f"
                        }
                    }

                }
                return@setOnPreferenceChangeListener true
            }

        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref, rootKey)   //加入ＩＴＥＭ
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            return super.onPreferenceTreeClick(preference)
        }

    }
}