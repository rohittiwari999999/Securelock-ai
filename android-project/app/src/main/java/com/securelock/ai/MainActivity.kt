package com.securelock.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.securelock.ai.service.AppLockSessionManager
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

        val app = application as SecureLockApplication

        setContent {
            val settings by app.storageRepository.settings.collectAsState()
            SecureLockTheme(
                themeId = settings.themeId,
                customAccentHex = settings.customAccentHex,
                forceLightMode = settings.forceLightMode,
                cardStyleMode = settings.cardStyleMode
            ) {
                SecureLockNavHost(
                    activity = this,
                    repository = app.storageRepository,
                    vaultRepository = app.vaultRepository
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as SecureLockApplication
        if (app.storageRepository.isSecretDateUnlockActive() &&
            intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        ) {
            AppLockSessionManager.lockVault()
        }
    }
}
