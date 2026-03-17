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
            .setMinimumFetchIntervalInSeconds(3600)
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

    suspend fun fetchThemeColors(): ThemeColors {
        return try {
            remoteConfig.fetchAndActivate().await()

            ThemeColors(
                primaryColor = Color.parseColor(remoteConfig.getString("primary_color")),
                operatorColor = Color.parseColor(remoteConfig.getString("operator_color")),
                backgroundColor = Color.parseColor(remoteConfig.getString("background_color")),
                statusBarColor = Color.parseColor(remoteConfig.getString("status_bar_color"))
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
}