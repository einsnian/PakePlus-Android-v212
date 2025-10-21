package com.app.pakeplus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetectorCompat

    // 权限请求码
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_STORAGE_PERMISSION = 101

    // 注册相机和相册结果回调
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                // 可将图片传递给WebView（示例：转为Base64）
                val base64Image = bitmapToBase64(it)
                webView.evaluateJavascript("window.onCameraImage('$base64Image')", null)
                Toast.makeText(this, "相机拍摄成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                // 可将图片Uri传递给WebView
                val uriString = it.toString()
                webView.evaluateJavascript("window.onGalleryImage('$uriString')", null)
                Toast.makeText(this, "相册选择成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.single_main)

        // 处理系统栏Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ConstraintLayout)) { view, insets ->
            val systemBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBar.left, systemBar.top, systemBar.right, systemBar.bottom)
            insets
        }

        // 初始化WebView
        webView = findViewById(R.id.webview)
        initWebViewSettings()

        // 初始化手势检测器
        initGestureDetector()

        // 检查并请求必要权限
        checkAndRequestPermissions()

        // 加载网页
        webView.loadUrl("https://juejin.cn/")
        // 本地测试可使用：webView.loadUrl("file:///android_asset/index.html")
    }

    // 初始化WebView设置
    private fun initWebViewSettings() {
        webView.settings.apply {
            javaScriptEnabled = true       // 启用JS（与前端交互必要）
            domStorageEnabled = true       // 启用DOM存储
            allowFileAccess = true         // 允许文件访问
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            setSupportZoom(false)
        }

        webView.clearCache(true)
        webView.webViewClient = MyWebViewClient()
        webView.webChromeClient = MyChromeClient()

        // 注入JS接口，供前端调用原生功能
        webView.addJavascriptInterface(WebAppInterface(), "NativeInterface")
    }

    // 初始化手势检测器（左右滑动控制网页前进后退）
    private fun initGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                // 处理水平滑动
                if (Math.abs(diffX) > Math.abs(diffY) && 
                    Math.abs(diffX) > 100 && 
                    Math.abs(velocityX) > 100
                ) {
                    if (diffX > 0 && webView.canGoBack()) {
                        webView.goBack()
                        return true
                    } else if (diffX < 0 && webView.canGoForward()) {
                        webView.goForward()
                        return true
                    }
                }
                return false
            }
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    // 检查并请求权限
    private fun checkAndRequestPermissions() {
        // 相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // 存储权限（根据Android版本适配）
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, storagePermission) 
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(storagePermission),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "相机权限被拒绝，无法使用相机功能", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "存储权限被拒绝，无法访问相册", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 处理返回键
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // WebView客户端
    inner class MyWebViewClient : WebViewClient() {
        private var debug = false  // 控制是否启用vConsole调试

        @Deprecated("Deprecated in Java", ReplaceWith("false"))
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return false  // 允许WebView加载所有链接
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            println("WebView错误: ${error?.description}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // 注入调试工具和自定义JS
            if (debug) {
                val vConsole = assets.open("vConsole.js").bufferedReader().use { it.readText() }
                view?.evaluateJavascript(vConsole + "var vConsole = new window.VConsole()", null)
            }
            val injectJs = assets.open("custom.js").bufferedReader().use { it.readText() }
            view?.evaluateJavascript(injectJs, null)
        }
    }

    // WebChrome客户端（处理进度和弹窗）
    inner class MyChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            println("网页加载进度: $newProgress%，URL: ${view?.url}")
        }
    }

    // JS与原生交互接口
    inner class WebAppInterface {
        // 供JS调用：打开相机
        @android.webkit.JavascriptInterface
        fun openCamera() {
            runOnUiThread {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) 
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    takePictureLauncher.launch(intent)
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
            }
        }

        // 供JS调用：打开相册
        @android.webkit.JavascriptInterface
        fun openGallery() {
            runOnUiThread {
                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, storagePermission) 
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImageLauncher.launch(intent)
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(storagePermission),
                        REQUEST_STORAGE_PERMISSION
                    )
                }
            }
        }
    }

    // 辅助方法：Bitmap转Base64（供WebView显示）
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }
}
