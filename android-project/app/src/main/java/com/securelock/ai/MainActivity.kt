package com.securelock.ai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.securelock.ai.ui.navigation.SecureLockNavHost
import com.securelock.ai.ui.theme.SecureLockTheme

/**
 * MainActivity extends FragmentActivity as required by androidx.biometric.BiometricPrompt
 * and uses enableEdgeToEdge() for Android 15/16 inset compliance.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as SecureLockApplication).storageRepository

        setContent {
            val settings by repository.settings.collectAsState()
            SecureLockTheme(themeMode = settings.themeMode) {
                SecureLockNavHost(
                    activity = this,
                    repository = repository
                )
            }
        }
    }
}
