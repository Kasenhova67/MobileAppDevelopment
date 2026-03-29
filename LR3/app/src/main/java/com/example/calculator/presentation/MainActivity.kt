package com.example.calculator.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.R
import com.example.calculator.Data.CalculatorRepositoryImpl
import com.example.calculator.Data.LocalDataSource
import com.example.calculator.Data.ThemeRepository
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ShareResultUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase
import com.example.calculator.utils.SoundManager
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var soundManager: SoundManager
    private lateinit var shareResultUseCase: ShareResultUseCase
    private lateinit var display: TextView
    private lateinit var expressionPreview: TextView
    private lateinit var mainLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_NO_TITLE)
        }

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        setContentView(R.layout.activity_main)

        initializeDependencies()
        initializeViews()
        setupObservers()
        setupClickListeners()

        viewModel.loadTheme()
        viewModel.loadHistory()
        handleIntent(intent)
    }

    private fun initializeDependencies() {
        val sharedPrefs = getSharedPreferences("calculator_prefs", MODE_PRIVATE)
        val localDataSource = LocalDataSource(sharedPrefs)
        val calculatorRepository = CalculatorRepositoryImpl(localDataSource)

        val calculateExpressionUseCase = CalculateExpressionUseCase(calculatorRepository)
        val validateExpressionUseCase = ValidateExpressionUseCase(calculatorRepository)

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(calculateExpressionUseCase, validateExpressionUseCase)
        )[MainViewModel::class.java]

        soundManager = SoundManager(this)
        shareResultUseCase = ShareResultUseCase()
    }

    private fun initializeViews() {
        display = findViewById(R.id.displayText)
        expressionPreview = findViewById(R.id.expressionPreview)
        mainLayout = findViewById(R.id.mainLayout)
    }

    private fun applyTheme(colors: ThemeRepository.ThemeColors) {
        try {
            val numberButtons = listOf(
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnEquals
            )

            numberButtons.forEach { id ->
                findViewById<Button>(id)?.apply {
                    setBackgroundColor(colors.primaryColor)
                    setTextColor(Color.WHITE)
                }
            }

            val operatorButtons = listOf(
                R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide,
                R.id.btnClear, R.id.btnBackspace
            )

            operatorButtons.forEach { id ->
                findViewById<Button>(id)?.apply {
                    setBackgroundColor(colors.primaryColor)
                    setTextColor(colors.operatorColor)
                }
            }

            mainLayout.setBackgroundColor(colors.backgroundColor)

            removeCustomStatusBar()
            createCustomStatusBar(colors.statusBarColor)

        } catch (e: Exception) {
            Log.e("MAIN_ACT", "Ошибка применения темы", e)
        }
    }

    private fun applyDefaultTheme() {
        val defaultColors = ThemeRepository.ThemeColors(
            primaryColor = Color.parseColor("#673AB7"),
            operatorColor = Color.parseColor("#FF9800"),
            backgroundColor = Color.parseColor("#E0C8FF"),
            statusBarColor = Color.parseColor("#4A1D8C")
        )
        applyTheme(defaultColors)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            if (intent.getBooleanExtra("from_notification", false)) {
                Toast.makeText(this, "Открыто из уведомления", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка обработки intent", e)
        }
    }

    private fun testLocalNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "test_channel",
                "Test Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Тестовый канал"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "test_channel")
            .setContentTitle(" Тестовое уведомление")
            .setContentText("Это тест из приложения")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(1002, notification)
        Toast.makeText(this, "Уведомление отправлено!", Toast.LENGTH_SHORT).show()
    }

    private fun showTestMenu() {
        val options = arrayOf(
            "1. Разрешение",
            "2. Тест локального уведомления",
            "3. Показать историю"
        )

        AlertDialog.Builder(this)
            .setTitle("🔧 Тестовое меню")
            .setItems(options) { _, which ->
                when (which) {

                    0 -> checkNotificationPermission()
                    1 -> testLocalNotification()
                    2 -> showHistoryDialog()

                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Toast.makeText(this, "Разрешение уже есть", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Разрешение не требуется для Android < 13", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra("notification_type")) {
            "calculation" -> {
                Toast.makeText(this, "Новое вычисление в истории", Toast.LENGTH_SHORT).show()
                viewModel.loadHistory()
            }
            "theme_update" -> {
                viewModel.loadTheme()
                Toast.makeText(this, "Тема обновлена", Toast.LENGTH_SHORT).show()
            }
            "history_cleared" -> {
                viewModel.clearHistory()
                Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.result.observe(this) { result ->
            display.text = result
        }

        viewModel.expression.observe(this) { expression ->
            expressionPreview.text = expression
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.themeColors.observe(this) { colors ->
            if (colors != null) {
                applyTheme(colors)
            } else {
                applyDefaultTheme()
            }
        }
    }

    private fun setupClickListeners() {
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                soundManager.playClick()
                val value = (it as Button).text.toString()
                viewModel.addCharacter(value)
            }
        }

        val operatorButtons = mapOf(
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnMultiply to "*",
            R.id.btnDivide to "/"
        )

        operatorButtons.forEach { (id, operator) ->
            findViewById<Button>(id).setOnClickListener {
                soundManager.playClick()
                viewModel.addCharacter(operator)
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener {
            soundManager.playClick()
            viewModel.addCharacter(".")
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            soundManager.playClick()
            viewModel.calculate()
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            soundManager.playClick()
            viewModel.clear()
        }

        try {
            findViewById<Button>(R.id.btnBackspace).setOnClickListener {
                soundManager.playClick()
                viewModel.removeLastCharacter()
            }
        } catch (e: Exception) {
            // Кнопка может отсутствовать
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            soundManager.playClick()
            handleShareClick()
        }

        findViewById<Button>(R.id.btnShare).setOnLongClickListener {
            soundManager.playClick()
            showTestMenu()
            true
        }

        findViewById<Button>(R.id.btnEquals).setOnLongClickListener {
            val options = arrayOf(
                "1. Обновить тему из Firebase",
                "2. Применить дефолтную тему",

            )

            AlertDialog.Builder(this)
                .setTitle("🔄 Меню")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            Toast.makeText(this, "Обновляем тему...", Toast.LENGTH_SHORT).show()
                            viewModel.forceReloadTheme()
                        }
                        1  -> applyDefaultTheme()

                    }
                }
                .setNegativeButton("Закрыть", null)
                .show()
            true
        }
    }

    private fun handleShareClick() {
        val result = display.text.toString()
        if (result == "0" || result == "Error") {
            Toast.makeText(this, "Нечего шерить", Toast.LENGTH_SHORT).show()
            return
        }

        shareResultUseCase.createResultImage(this, result)?.let { file ->
            val uri = shareResultUseCase.getUriForFile(this, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Result"))
        } ?: run {
            Toast.makeText(this, "Failed to create image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    private fun showHistoryDialog() {
        val history = viewModel.history.value ?: emptyList()

        if (history.isEmpty()) {
            Toast.makeText(this, "История пуста", Toast.LENGTH_SHORT).show()
            return
        }

        val items = history.map { "${it.expression} = ${it.result}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("История вычислений")
            .setItems(items) { _, position ->
                val selectedItem = history[position]
                viewModel.setExpression(selectedItem.expression)
            }
            .setPositiveButton("Очистить всё") { _, _ ->
                viewModel.clearHistory()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showCurrentColors() {
        val colors = viewModel.themeColors.value
        if (colors != null) {
            val message = """
                primary: #${Integer.toHexString(colors.primaryColor)}
                operator: #${Integer.toHexString(colors.operatorColor)}
                background: #${Integer.toHexString(colors.backgroundColor)}
                statusBar: #${Integer.toHexString(colors.statusBarColor)}
            """.trimIndent()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Цвета не загружены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCustomStatusBar(color: Int) {
        try {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                (24 * resources.displayMetrics.density).toInt()
            }

            val decorView = window.decorView as ViewGroup
            val statusBarView = View(this)
            statusBarView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                statusBarHeight
            )
            statusBarView.setBackgroundColor(color)
            statusBarView.tag = "custom_status_bar"

            decorView.addView(statusBarView)
        } catch (e: Exception) {
            Log.e("CUSTOM_STATUS", "Ошибка создания статус-бара", e)
        }
    }

    private fun removeCustomStatusBar() {
        try {
            val decorView = window.decorView as ViewGroup
            for (i in 0 until decorView.childCount) {
                val child = decorView.getChildAt(i)
                if (child.tag == "custom_status_bar") {
                    decorView.removeView(child)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("CUSTOM_STATUS", "Ошибка удаления статус-бара", e)
        }
    }
}