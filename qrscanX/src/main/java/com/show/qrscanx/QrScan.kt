package com.show.qrscanX

import android.content.Context
import android.graphics.Bitmap
import com.google.zxing.Result

/**
 *  com.show.qrscanX
 *  2021/1/4
 *  22:04
 *  ShowMeThe
 */

typealias  ScanningResultListener  = (resultOk:Boolean,result:Result?)->Unit

enum class DecodeType{
    SINGLE,CONTINUES
}

object QrScan {

    fun decode(context: Context) : QrCodeDecode {
        return QrCodeDecode(context, DecodeType.SINGLE)
    }

    fun decode(context: Context,type: DecodeType) : QrCodeDecode {
        return QrCodeDecode(context, type)
    }

    fun decodeBitmap(bitmap: Bitmap?) : QrCodeBitmap {
        return QrCodeBitmap(bitmap)
    }

}