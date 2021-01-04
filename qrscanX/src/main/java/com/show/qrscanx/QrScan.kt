package com.show.qrscanx

import android.content.Context
import com.google.zxing.Result
import kotlinx.coroutines.flow.debounce

/**
 *  com.show.qrscanx
 *  2021/1/4
 *  22:04
 *  ShowMeThe
 */

typealias  ScanningResultListener  = (result:Result)->Unit

sealed class DecodeType(val debounce:Long){
    class SINGLE : DecodeType(-1)
    class CONTINUES : DecodeType(300)
}

object QrScan {

    fun decode(context: Context) : QrCodeDecode{
        return QrCodeDecode(context, DecodeType.SINGLE())
    }

    fun decode(context: Context,type:DecodeType) : QrCodeDecode{
        return QrCodeDecode(context, type)
    }

}