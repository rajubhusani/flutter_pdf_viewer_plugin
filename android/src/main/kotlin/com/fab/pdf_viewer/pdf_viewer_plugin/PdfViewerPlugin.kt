package com.fab.pdf_viewer.pdf_viewer_plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.system.Os.close
import android.widget.FrameLayout
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.File


/** PdfViewerPlugin */
public class PdfViewerPlugin: FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
//   lateinit var channel : MethodChannel
  private var flutterFullPdfViewerManager: FlutterFullPdfViewerManager? = null
  private var flutterBinding: FlutterPlugin.FlutterPluginBinding? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    flutterBinding = flutterPluginBinding
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "pdf_viewer_plugin")
    channel.setMethodCallHandler(this)
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    lateinit var channel : MethodChannel
    var activity: Activity? = null
    var asset: String? = null
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "pdf_viewer_plugin")
      activity = registrar.activity()
      channel.setMethodCallHandler(PdfViewerPlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "launch" -> openPDF(call, result)
      "resize" -> resize(call, result)
      "close" -> close(call, result)
      "share" -> sharePDFFile(call, result)
      else -> result.notImplemented()
    }
  }

  private fun openPDF(call: MethodCall, result: Result) {
    val mode = call.argument<String>("mode")
    val pass = call.argument<String>("pass")
    var src = call.argument<String>("path")
    src = when (mode) {
      "fromFile" -> Uri.parse(src).path!!
      "fromAsset" -> flutterBinding?.flutterAssets?.getAssetFilePathByName(src!!)
      else -> throw IllegalArgumentException("invalid mode: $mode.")
    }
    if (flutterFullPdfViewerManager == null) {
      flutterFullPdfViewerManager = FlutterFullPdfViewerManager(activity)
    }
    val params = buildLayoutParams(call)
    activity?.addContentView(flutterFullPdfViewerManager?.pdfView, params)
    flutterFullPdfViewerManager?.openPDF(src!!, pass, mode)
    result.success(null)
  }

  private fun resize(call: MethodCall, result: Result) {
    if (flutterFullPdfViewerManager != null) {
      val params = buildLayoutParams(call)
      flutterFullPdfViewerManager?.resize(params)
    }
    result.success(null)
  }

  private fun close(call: MethodCall, result: Result) {
    if (flutterFullPdfViewerManager != null) {
      flutterFullPdfViewerManager?.close(call, result)
      flutterFullPdfViewerManager = null
    }
  }

  private fun buildLayoutParams(call: MethodCall): FrameLayout.LayoutParams {
    val rc = call.argument<Map<String, Number>>("rect")
    val params: FrameLayout.LayoutParams
    if (rc != null) {
      params = FrameLayout.LayoutParams(dp2px(activity!!, rc["width"]?.toInt()?.toFloat()!!), dp2px(activity!!, rc["height"]?.toInt()?.toFloat()!!))
      params.setMargins(dp2px(activity!!, rc["left"]?.toInt()?.toFloat()!!), dp2px(activity!!, rc["top"]?.toInt()?.toFloat()!!), 0, 0)
    } else {
      val display = activity?.windowManager?.defaultDisplay
      val size = Point()
      display?.getSize(size)
      val width = size.x
      val height = size.y
      params = FrameLayout.LayoutParams(width, height)
    }
    return params
  }

  private fun dp2px(context: Context, dp: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
  }

  override fun onActivityResult(i: Int, i1: Int, intent: Intent): Boolean {
    return flutterFullPdfViewerManager != null
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivity() {
    channel.setMethodCallHandler(null)
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    Log.d("====", "onReattachedToActivityForConfigChanges")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {

    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    Log.d("====", "onDetachedFromActivityForConfigChanges")
  }

  private fun sharePDFFile(call: MethodCall, result: Result){
    val filePath = call.argument<String>("path")

    val file = File(filePath!!)

    val fileUri = FileProvider.getUriForFile(activity!!.applicationContext, activity!!.applicationContext.packageName + ".provider", file)

    val intent = Intent()
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.action = Intent.ACTION_SEND
    intent.type = "application/pdf"
    intent.putExtra(Intent.EXTRA_SUBJECT, "Share File")
    intent.putExtra(Intent.EXTRA_STREAM, fileUri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val chooserIntent = Intent.createChooser(intent, "Share Statement PDF")
    chooserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    activity!!.startActivity(chooserIntent)


    result.success(true)

  }
}
