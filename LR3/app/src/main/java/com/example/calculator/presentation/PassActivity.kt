package com.example.calculator.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.calculator.R
import com.example.calculator.utils.PassManager
import java.util.concurrent.Executor

class PassActivity : AppCompatActivity() {

    private lateinit var passKeyManager: PassManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var etPin: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvReset: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pass)

        passKeyManager = PassManager(this)
        executor = ContextCompat.getMainExecutor(this)

        initViews()
        setupBiometric()

        if (!passKeyManager.isPassKeySetup()) {
            showSetupDialog()
        }
    }

    private fun initViews() {
        etPin = findViewById(R.id.etPin)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvReset = findViewById(R.id.tvReset)

        btnSubmit.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin.isEmpty()) {
                Toast.makeText(this, "Введите пин-код", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passKeyManager.validatePassKey(pin)) {
                Toast.makeText(this, "Доступ разрешен", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Неверный пин-код", Toast.LENGTH_SHORT).show()
                etPin.text.clear()
            }
        }

        tvReset.setOnClickListener {
            showBiometricResetDialog()
        }
    }

    private fun setupBiometric() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    showSetupNewPinAfterBiometric()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(applicationContext, "Биометрия не распознана", Toast.LENGTH_SHORT).show()
                }
            })

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            findViewById<TextView>(R.id.tvBiometricHint).visibility = android.view.View.VISIBLE
        }
    }

    private fun showSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.setup, null)
        val etNewPin = dialogView.findViewById<EditText>(R.id.etNewPin)
        val etConfirmPin = dialogView.findViewById<EditText>(R.id.etConfirmPin)

        AlertDialog.Builder(this)
            .setTitle("Установка пин-кода")
            .setView(dialogView)
            .setPositiveButton("Установить") { _, _ ->
                val pin = etNewPin.text.toString()
                val confirm = etConfirmPin.text.toString()

                if (pin.isNotEmpty() && pin == confirm && pin.length >= 4) {
                    if (passKeyManager.initPassKey(pin)) {
                        Toast.makeText(this, "Пин-код установлен", Toast.LENGTH_SHORT).show()
                        etPin.requestFocus()
                    } else {
                        Toast.makeText(this, "Ошибка установки", Toast.LENGTH_SHORT).show()
                        showSetupDialog()
                    }
                } else {
                    Toast.makeText(this, "Пин-коды не совпадают или слишком короткий", Toast.LENGTH_SHORT).show()
                    showSetupDialog()
                }
            }
            .setNegativeButton("Закрыть") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showBiometricResetDialog() {
        // Проверяем доступность биометрии
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            AlertDialog.Builder(this)
                .setTitle("Биометрия недоступна")
                .setMessage("Для сброса пароля требуется биометрическая аутентификация.\n\n" +
                        "Пожалуйста, настройте отпечаток пальца или Face ID в настройках телефона.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Запускаем биометрию
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Подтверждение сброса пароля")
            .setSubtitle("Подтвердите личность для сброса пин-кода")
            .setNegativeButtonText("Отмена")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showSetupNewPinAfterBiometric() {
        val dialogView = layoutInflater.inflate(R.layout.setup, null)
        val etNewPin = dialogView.findViewById<EditText>(R.id.etNewPin)
        val etConfirmPin = dialogView.findViewById<EditText>(R.id.etConfirmPin)

        AlertDialog.Builder(this)
            .setTitle("Установка нового пин-кода")
            .setMessage("После подтверждения биометрии установите новый пин-код")
            .setView(dialogView)
            .setPositiveButton("Установить") { _, _ ->
                val newPin = etNewPin.text.toString()
                val confirmPin = etConfirmPin.text.toString()

                if (newPin.isNotEmpty() && newPin == confirmPin && newPin.length >= 4) {
                    // Очищаем старые данные и устанавливаем новый PIN
                    passKeyManager.clearPassKey()
                    if (passKeyManager.initPassKey(newPin)) {
                        Toast.makeText(this, "Пин-код успешно изменен", Toast.LENGTH_SHORT).show()
                        // Очищаем поле ввода и показываем экран входа
                        etPin.text.clear()
                        etPin.requestFocus()
                    } else {
                        Toast.makeText(this, "Ошибка установки пин-кода", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Пин-коды не совпадают или слишком короткий", Toast.LENGTH_SHORT).show()
                    showSetupNewPinAfterBiometric()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}