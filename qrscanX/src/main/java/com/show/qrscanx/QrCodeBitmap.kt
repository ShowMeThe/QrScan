package com.show.qrscanX

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer

class QrCodeBitmap constructor(val bitmap: Bitmap?) {


    fun getResult(defaultBarcodeFormat :ArrayList<BarcodeFormat> =
                      arrayListOf(BarcodeFormat.QR_CODE),canRecycler:Boolean = true):Result?{
        if(bitmap == null || bitmap.isRecycled){
            return null
        }
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
        val reader = MultiFormatReader()
        val map = mapOf<DecodeHintType, Collection<BarcodeFormat>>(
            Pair(DecodeHintType.POSSIBLE_FORMATS, defaultBarcodeFormat)
        )
        reader.setHints(map)
       return kotlin.runCatching {
            val result = reader.decode(binaryBitmap)
            result
        }.onSuccess {
           if(!bitmap.isRecycled && canRecycler){
               bitmap.recycle()
           }
       }.getOrNull()
    }

}