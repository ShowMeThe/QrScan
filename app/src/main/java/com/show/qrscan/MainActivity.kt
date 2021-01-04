package com.show.qrscan

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.show.qrscanx.DecodeType
import com.show.qrscanx.QrScan
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        QrScan.decode(this, DecodeType.CONTINUES())
            .bind(preView,this)
            .addListener {
                Log.e("2222222", it.text)
            }

    }



}