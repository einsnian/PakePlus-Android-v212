package com.app.pakeplus

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
// import android.view.Menu
// import android.view.WindowInsets
// import com.google.android.material.snackbar.Snackbar
// import com.google.android.material.navigation.NavigationView
// import androidx.navigation.findNavController
// import androidx.navigation.ui.AppBarConfiguration
// import androidx.navigation.ui.navigateUp
// import androidx.navigation.ui.setupActionBarWithNavController
// import androidx.navigation.ui.setupWithNavController
// import androidx.drawerlayout.widget.DrawerLayout
// import com.app.pakeplus.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
     private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.web_view) // 假设布局中有id为web_view的WebView

        // 配置WebView支持JS和文件访问
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // 必须启用JS，否则网页上传按钮可能无法触发
        webSettings.allowFileAccess = true // 允许访问文件
        webSettings.allowContentAccess = true // 允许通过内容提供者访问文件
    }
    
     // 权限请求码
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_STORAGE_PERMISSION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 检查并请求相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // 检查并请求存储权限（根据系统版本适配）
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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

     private lateinit var imageView: ImageView  // 用于显示图片的控件

    // 注册相机结果回调
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)  // 显示相机拍摄的图片
        }
    }

    // 注册相册结果回调
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            imageView.setImageURI(imageUri)  // 显示相册选择的图片
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // 假设布局文件为activity_main.xml
        imageView = findViewById(R.id.image_view)  // 绑定布局中的ImageView

        // 绑定相机按钮点击事件（假设布局中有id为btn_camera的按钮）
        findViewById<ImageView>(R.id.btn_camera).setOnClickListener {
            // 检查相机权限后启动相机
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED
            ) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureLauncher.launch(takePictureIntent)
            }
        }

        // 绑定相册按钮点击事件（假设布局中有id为btn_gallery的按钮）
        findViewById<ImageView>(R.id.btn_gallery).setOnClickListener {
            // 检查存储权限后启动相册
            val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(this, storagePermission) 
                == PackageManager.PERMISSION_GRANTED
            ) {
                val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickImageLauncher.launch(pickImageIntent)
            }
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 相机权限已授予，可执行相机操作
                } else {
                    // 权限被拒绝，提示用户
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 存储权限已授予，可执行相册操作
                } else {
                    // 权限被拒绝，提示用户
                }
            }
        }
    }
//    private lateinit var appBarConfiguration: AppBarConfiguration
//    private lateinit var binding: ActivityMainBinding

    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetectorCompat

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.single_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ConstraintLayout))
        { view, insets ->
            val systemBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBar.left, systemBar.top, systemBar.right, systemBar.bottom)
            insets
        }

        webView = findViewById<WebView>(R.id.webview)

        webView.settings.apply {
            javaScriptEnabled = true       // 启用JS
            domStorageEnabled = true       // 启用DOM存储（Vue 需要）
            allowFileAccess = true         // 允许文件访问
            setSupportMultipleWindows(true)
        }

        // webView.settings.userAgentString = ""

        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(false)

        // clear cache
        webView.clearCache(true)

        // inject js
        webView.webViewClient = MyWebViewClient()

        // get web load progress
        webView.webChromeClient = MyChromeClient()

        // Setup gesture detector
        gestureDetector =
            GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    // Only handle horizontal swipes
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                            if (diffX > 0) {
                                // Swipe right - go back
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                    return true
                                }
                            } else {
                                // Swipe left - go forward
                                if (webView.canGoForward()) {
                                    webView.goForward()
                                    return true
                                }
                            }
                        }
                    }
                    return false
                }
            })

        // Set touch listener for WebView
        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        webView.loadUrl("https://juejin.cn/")
        // webView.loadUrl("file:///android_asset/index.html")

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(R.layout.single_main)

//        setSupportActionBar(binding.appBarMain.toolbar)

//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.fab).show()
//        }

//        val drawerLayout: DrawerLayout = binding.drawerLayout
//        val navView: NavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
//            ), drawerLayout
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }

    inner class MyWebViewClient : WebViewClient() {

        // vConsole debug
        private var debug = false

        @Deprecated("Deprecated in Java", ReplaceWith("false"))
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return false
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            println("webView onReceivedError: ${error?.description}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (debug) {
                // vConsole
                val vConsole = assets.open("vConsole.js").bufferedReader().use { it.readText() }
                val openDebug = """var vConsole = new window.VConsole()"""
                view?.evaluateJavascript(vConsole + openDebug, null)
            }
            // inject js
            val injectJs = assets.open("custom.js").bufferedReader().use { it.readText() }
            view?.evaluateJavascript(injectJs, null)
        }
    }

    inner class MyChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            val url = view?.url
            println("wev view url:$url")
        }
    }
}
