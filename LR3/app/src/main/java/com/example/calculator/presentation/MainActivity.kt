package com.example.calculator.presentation

import android.content.Intent

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.R
import com.example.calculator.Data.CalculatorRepositoryImpl
import com.example.calculator.Data.LocalDataSource
import com.example.calculator.Domain.CalculateExpressionUseCase
import com.example.calculator.Domain.ShareResultUseCase
import com.example.calculator.Domain.ValidateExpressionUseCase

import com.example.calculator.utils.SoundManager

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var soundManager: SoundManager
    private lateinit var shareResultUseCase: ShareResultUseCase

    private lateinit var display: TextView
    private lateinit var expressionPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeDependencies()
        initializeViews()
        setupObservers()
        setupClickListeners()
    }

    private fun initializeDependencies() {
        val calculatorRepository = CalculatorRepositoryImpl(
            LocalDataSource(getSharedPreferences("calculator_prefs", MODE_PRIVATE))
        )

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
    }

    private fun setupClickListeners() {
        // Numbers 0-9
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
}