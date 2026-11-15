package com.securelock.ai

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.screens.AppProtectionGateScreen
import com.securelock.ai.ui.screens.UnlockScreen
import com.securelock.ai.ui.theme.SecureLockTheme

/**
 * Dedicated FLAG_SECURE Activity displayed when a user-selected protected app enters
 * the foreground (via AccessibilityService or UsageStatsManager monitor).
 * Supports per-app lock methods and simulated decoy protection screens.
 */
class LockOverlayActivity : FragmentActivity() {

    private var activeTargetPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        activeTargetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        if (activeTargetPackage.isBlank() || activeTargetPackage == packageName) {
            finish()
            return
        }

        val repository = (application as SecureLockApplication).storageRepository

        setContent {
            val settings by repository.settings.collectAsState()
            val targetPackage = activeTargetPackage
            val targetLabel = remember(targetPackage) {
                AppLockServiceController.resolveAppLabel(this, targetPackage)
            }
            var showProtectionCover by remember(targetPackage, settings.protectionScreenConfig.enabled) {
                mutableStateOf(settings.protectionScreenConfig.enabled)
            }

            BackHandler {
                exitToHomeScreen()
            }

            SecureLockTheme(
                themeId = settings.themeId,
                customAccentHex = settings.customAccentHex,
                forceLightMode = settings.forceLightMode,
                cardStyleMode = settings.cardStyleMode
            ) {
                if (showProtectionCover) {
                    AppProtectionGateScreen(
                        config = settings.protectionScreenConfig,
                        onProceedToUnlock = { showProtectionCover = false },
                        onDismissHome = { exitToHomeScreen() }
                    )
                } else {
                    UnlockScreen(
                        activity = this,
                        repository = repository,
                        targetPackageLabel = targetLabel,
                        targetPackageName = targetPackage,
                        isSetupFlow = false,
                        onUnlocked = {
                            AppLockSessionManager.markAppUnlocked(
                                packageName = targetPackage,
                                timeoutSeconds = settings.relockTimeoutSeconds
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val nextPkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (!nextPkg.isNullOrBlank() && nextPkg != packageName) {
            activeTargetPackage = nextPkg
        }
    }

    private fun exitToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(homeIntent) }
        finish()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }
}
