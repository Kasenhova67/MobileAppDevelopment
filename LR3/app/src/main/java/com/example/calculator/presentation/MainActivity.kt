package com.example.calculator.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.R
import com.example.calculator.presentation.MainViewModel
import com.example.calculator.Data.CalculatorRepositoryImpl
import com.example.calculator.Data.HistoryRepository
import com.example.calculator.Data.LocalDataSource
import com.example.calculator.Data.ThemeRepository
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ShareResultUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase
import com.example.calculator.utils.NotificationManager
import com.example.calculator.utils.SoundManager
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var soundManager: SoundManager
    private lateinit var shareResultUseCase: ShareResultUseCase
    private lateinit var notificationManager: NotificationManager

    private lateinit var display: TextView
    private lateinit var expressionPreview: TextView
    private lateinit var mainLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeDependencies()
        initializeViews()
        setupObservers()
        setupClickListeners()

        handleIntent(intent)

        viewModel.loadTheme()
        viewModel.loadHistory()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra("notification_type")) {
            "calculation" -> {
                Toast.makeText(this, "New calculation added to history", Toast.LENGTH_SHORT).show()
                viewModel.loadHistory()
            }
            "theme_update" -> {
                viewModel.loadTheme()
                Toast.makeText(this, "Theme has been updated", Toast.LENGTH_SHORT).show()
            }
            "history_cleared" -> {
                viewModel.clearHistory()
                Toast.makeText(this, "History has been cleared", Toast.LENGTH_SHORT).show()
            }
        }
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
        notificationManager = NotificationManager(this)
    }

    private fun initializeViews() {
        display = findViewById(R.id.displayText)
        expressionPreview = findViewById(R.id.expressionPreview)
        mainLayout = findViewById(R.id.mainLayout)
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
            colors?.let {
                applyTheme(it)
            }
        }

        viewModel.history.observe(this) { historyItems ->

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

        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            soundManager.playClick()
            handleShareClick()
        }

        try {
            findViewById<Button>(R.id.btnHistory).setOnClickListener {
                soundManager.playClick()
                showHistoryDialog()
            }
        } catch (e: Exception) {

            findViewById<Button>(R.id.btnShare).setOnLongClickListener {
                soundManager.playClick()
                showHistoryDialog()
                true
            }
        }
    }

    private fun handleShareClick() {
        val result = display.text.toString()
        if (result == "0" || result == "Error") {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
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

    private fun applyTheme(colors: ThemeRepository.ThemeColors) {
        try {
            val numberButtons = listOf(
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnEquals
            )

            numberButtons.forEach { id ->
                findViewById<Button>(id)?.setBackgroundColor(colors.primaryColor)
            }

            val operatorButtons = listOf(
                R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide,
                R.id.btnClear, R.id.btnBackspace
            )

            operatorButtons.forEach { id ->
                findViewById<Button>(id)?.setBackgroundColor(colors.operatorColor)
            }

            mainLayout.setBackgroundColor(colors.backgroundColor)

            window.statusBarColor = colors.statusBarColor

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showHistoryDialog() {
        val history = viewModel.history.value ?: emptyList()

        if (history.isEmpty()) {
            Toast.makeText(this, "No history yet", Toast.LENGTH_SHORT).show()
            return
        }

        val items = history.map { "${it.expression} = ${it.result}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Calculation History")
            .setItems(items) { _, position ->
                // Optional: Handle item click - maybe paste the expression?
                val selectedItem = history[position]
                viewModel.setExpression(selectedItem.expression)
            }
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.clearHistory()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}