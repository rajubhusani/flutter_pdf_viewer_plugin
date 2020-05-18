import Flutter
import UIKit
//import WebKit
import PDFKit

@available(iOS 11.0, *)
public class SwiftPdfViewerPlugin: NSObject, FlutterPlugin {
    
    enum Actions: String {
        case launch
        case resize
        case close
        case share
    }
    var _result: FlutterResult?
    var _viewController: UIViewController!
    var _pdfView: PDFView?
    
    init(viewController: UIViewController) {
        _viewController = viewController;
        super.init()
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "pdf_viewer_plugin", binaryMessenger: registrar.messenger())
        
        let viewController: UIViewController = (UIApplication.shared.delegate?.window??.rootViewController)!;
        
        let instance = SwiftPdfViewerPlugin(viewController: viewController)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    func parseRect(rect: [String: Double]) -> CGRect {
        return CGRect.init(x: rect["left"]!, y: rect["top"]!, width: rect["width"]!, height: rect["height"]!)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        if (_result != nil) {
            
            _result!(FlutterError.init(code: "multiple_request", message: "Cancelled by a second request", details: nil))
            
            _result = nil;
        }
        
        switch call.method {
        case Actions.launch.rawValue:
            guard let arguments = call.arguments as? [String: Any] else {
                _result!(FlutterError.init(code: "arguments_not_found", message: "Please pass arguments", details: nil))
                return
            }
            
            performLaunchActionWithArguments(arguments)
            
        case Actions.resize.rawValue:
            guard let arguments = call.arguments as? [String: Any] else {
                _result!(FlutterError.init(code: "arguments_not_found", message: "Please pass arguments", details: nil))
                return
            }
            
            performResizeActionWithArguments(arguments)
        case Actions.close.rawValue:
            
            performCloseAction()
            result(nil)
            
        case Actions.share.rawValue:
            
            guard let arguments = call.arguments as? [String: Any] else {
                _result!(FlutterError.init(code: "arguments_not_found", message: "Please pass arguments", details: nil))
                return
            }
            
            performShareActionWithArguments(arguments)
        default:
            result(FlutterMethodNotImplemented);
        }
    }
    
    
    private func performLaunchActionWithArguments(_ arguments: [String: Any]) {
        
        if _pdfView == nil {
            
            guard  let rectValues: Dictionary<String, Double> = arguments["rect"] as? [String: Double] else {
                _result?(FlutterError.init(code: "rect_not_found", message: "Please value for key rect", details: nil))
                return
            }
            
            guard let path = arguments["path"] as? String else {
                _result?(FlutterError.init(code: "path_not_found", message: "Please value for key path", details: nil))
                return
            }
            
            let rect = self.parseRect(rect: rectValues)
            
            let targetURL: URL = URL.init(fileURLWithPath: path)
            
            _pdfView = PDFView(frame: rect)
            _pdfView!.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            _viewController.view.addSubview(_pdfView!)
            
            _pdfView!.autoScales = true
            
            setDocumentOfPDFView(_pdfView!, withUrl: targetURL, password: arguments["pass"] as? String)
        }
    }
    
    private func setDocumentOfPDFView(_ pdfView: PDFView, withUrl url: URL, password: String?) {
        
        guard let doc = PDFDocument(url: url) else {
            _result?(FlutterError.init(code: "document_not_found", message: "Please pass correct path", details: nil))
            return
        }
        pdfView.document = doc
        if doc.isLocked == true {
            
            guard (password != nil) else {
                
                _result?(FlutterError.init(code: "password_not_found", message: "Please value for key pass", details: nil))
                return
            }
            
            if !doc.unlock(withPassword: password!) {
                _result?(FlutterError.init(code: "incorrect password", message: "Password passed is incorrect", details: nil))
            }
        }
    }
    
    
    private func performResizeActionWithArguments(_ arguments: [String: Any]) {
        
        
        if _pdfView != nil {
            
            let rect: Dictionary<String, Double> = arguments["rect"] as! [String: Double]
            let rc: CGRect = self.parseRect(rect: rect)
            _pdfView!.frame = rc
        }
    }
    
    
    func performCloseAction() {
        
        if _pdfView != nil {
            _pdfView!.removeFromSuperview()
            _pdfView = nil
        }
    }
    
    private func performShareActionWithArguments(_ arguments: [String: Any]) {
        
        
        guard let path = arguments["path"] as? String else {
            
            _result?(FlutterError.init(code: "path_not_found", message: "Please pass value for key path", details: nil))
            return
        }
        self.shareFileWithPath(path: path)
    }
    
    private func shareFileWithPath(path: String) {
        
        let fileURL = NSURL(fileURLWithPath: path)
        
        let objectsToShare = [fileURL]
        let activityController = UIActivityViewController(activityItems: objectsToShare, applicationActivities: nil)
        
        
        if UI_USER_INTERFACE_IDIOM() == .phone {
            _viewController.present(activityController, animated: true, completion: nil)
        }else {
            let popup: UIPopoverController = UIPopoverController.init(contentViewController: activityController)
            popup.present(from: CGRect.init(x: _viewController.view.frame.size.width/2, y: _viewController.view.frame.size.height/4, width: 0, height: 0), in: _viewController.view, permittedArrowDirections: .any, animated: true)
        }
        
    }
}
