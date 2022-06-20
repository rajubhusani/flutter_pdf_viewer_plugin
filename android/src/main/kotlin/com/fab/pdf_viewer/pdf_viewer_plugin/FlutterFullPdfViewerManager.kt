package com.fab.pdf_viewer.pdf_viewer_plugin

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

import com.github.barteksc.pdfviewer.PDFView

import java.io.File

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * FlutterFullPdfViewerManager
 */
internal class FlutterFullPdfViewerManager(activity: Activity?) {

    var closed = false
    var pdfView: PDFView? = null

    init {
        this.pdfView = PDFView(activity, null)
    }

    fun openPDF(path: String, pass: String?, mode: String?) {
        when (mode) {
            "fromFile" -> {
                pdfView!!.fromFile(File(path))
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .password(pass)
                        .load()
            }
            "fromAsset" -> {
                pdfView!!.fromAsset(path)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .password(pass)
                        .load()
            }
            else -> throw IllegalArgumentException("invalid mode: $mode.")
        }
    }

    fun resize(params: FrameLayout.LayoutParams) {
        pdfView!!.layoutParams = params
    }

    @JvmOverloads
    fun close(call: MethodCall? = null, result: MethodChannel.Result? = null) {
        if (pdfView != null) {
            val vg = pdfView!!.parent as ViewGroup
            vg.removeView(pdfView)
        }
        pdfView = null
        result?.success(null)

        closed = true
    }
}