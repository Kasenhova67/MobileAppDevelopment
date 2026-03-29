package com.example.calculator.Data

import android.graphics.Color
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class ThemeRepository {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    data class ThemeColors(
        val primaryColor: Int,
        val operatorColor: Int,
        val backgroundColor: Int,
        val statusBarColor: Int
    )

    suspend fun initRemoteConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        val defaults = mapOf(
            "primary_color" to "#673AB7",
            "operator_color" to "#FF9800",
            "background_color" to "#E0C8FF",
            "status_bar_color" to "#4A1D8C"
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    private fun cleanColorString(colorStr: String): String = colorStr.trim()

    suspend fun fetchThemeColors(): ThemeColors {
        return try {
            remoteConfig.fetch(0).await()
            remoteConfig.activate().await()

            val primaryStr = cleanColorString(remoteConfig.getString("primary_color"))
            val operatorStr = cleanColorString(remoteConfig.getString("operator_color"))
            val backgroundStr = cleanColorString(remoteConfig.getString("background_color"))
            val statusBarStr = cleanColorString(remoteConfig.getString("status_bar_color"))

            val finalPrimary = if (primaryStr.startsWith("#")) primaryStr else "#$primaryStr"
            val finalOperator = if (operatorStr.startsWith("#")) operatorStr else "#$operatorStr"
            val finalBackground = if (backgroundStr.startsWith("#")) backgroundStr else "#$backgroundStr"
            val finalStatusBar = if (statusBarStr.startsWith("#")) statusBarStr else "#$statusBarStr"

            ThemeColors(
                primaryColor = Color.parseColor(finalPrimary),
                operatorColor = Color.parseColor(finalOperator),
                backgroundColor = Color.parseColor(finalBackground),
                statusBarColor = Color.parseColor(finalStatusBar)
            )
        } catch (e: Exception) {
            ThemeColors(
                primaryColor = Color.parseColor("#673AB7"),
                operatorColor = Color.parseColor("#FF9800"),
                backgroundColor = Color.parseColor("#E0C8FF"),
                statusBarColor = Color.parseColor("#4A1D8C")
            )
        }
    }

    suspend fun forceFetchThemeColors(): ThemeColors {
        return try {
            remoteConfig.fetchAndActivate().await()
            fetchThemeColors()
        } catch (e: Exception) {
            fetchThemeColors()
        }
    }
}