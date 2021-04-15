import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

enum PDFViewState { shouldStart, startLoad, finishLoad }

class PdfViewerPlugin {
  final _channel = const MethodChannel("pdf_viewer_plugin");
  static PdfViewerPlugin? _instance;

  factory PdfViewerPlugin() => _instance ??= new PdfViewerPlugin._();
  PdfViewerPlugin._() {
    _channel.setMethodCallHandler(_handleMessages);
  }

  final _onDestroy = new StreamController<Null>.broadcast();
  Stream<Null> get onDestroy => _onDestroy.stream;
  Future<Null> _handleMessages(MethodCall call) async {
    switch (call.method) {
      case 'onDestroy':
        _onDestroy.add(null);
        break;
    }
  }

  Future<Null> launch(String? path, String? pass, String? mode,
      {Rect? rect}) async {
    final args = <String, dynamic>{'path': path, 'pass': pass, 'mode': mode};
    if (rect != null) {
      args['rect'] = {
        'left': rect.left,
        'top': rect.top,
        'width': rect.width,
        'height': rect.height
      };
    }
    await _channel.invokeMethod('launch', args);
  }

  /// Close the PDFViewer
  /// Will trigger the [onDestroy] event
  Future close() => _channel.invokeMethod('close');

  //Share PDF File
  Future share(String path) async {
    final args = <String, dynamic>{'path': path};
    _channel.invokeMethod('share', args);
  }

  /// adds the plugin as ActivityResultListener
  /// Only needed and used on Android
  Future registerAcitivityResultListener() =>
      _channel.invokeMethod('registerAcitivityResultListener');

  /// removes the plugin as ActivityResultListener
  /// Only needed and used on Android
  Future removeAcitivityResultListener() =>
      _channel.invokeMethod('removeAcitivityResultListener');

  /// Close all Streams
  void dispose() {
    _onDestroy.close();
    _instance = null;
  }

  /// resize PDFViewer
  Future<Null> resize(Rect? rect) async {
    final args = {};
    args['rect'] = {
      'left': rect?.left,
      'top': rect?.top,
      'width': rect?.width,
      'height': rect?.height
    };
    await _channel.invokeMethod('resize', args);
  }
}
