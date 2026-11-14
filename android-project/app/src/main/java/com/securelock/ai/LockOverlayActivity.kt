package com.securelock.ai

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
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.screens.AppProtectionGateScreen
import com.securelock.ai.ui.screens.UnlockScreen
import com.securelock.ai.ui.theme.SecureLockTheme

/**
 * Dedicated FLAG_SECURE Activity displayed when a user-selected protected app enters
 * the foreground (requires Usage Access / Accessibility + Display Over Other Apps).
 */
class LockOverlayActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: packageName
        val repository = (application as SecureLockApplication).storageRepository

        setContent {
            val settings by repository.settings.collectAsState()
            var showProtectionCover by remember {
                mutableStateOf(settings.protectionScreenConfig.enabled)
            }

            BackHandler {
                moveTaskToBack(true)
            }

            SecureLockTheme(themeMode = settings.themeMode) {
                if (showProtectionCover) {
                    AppProtectionGateScreen(
                        config = settings.protectionScreenConfig,
                        onProceedToUnlock = { showProtectionCover = false },
                        onDismissHome = { moveTaskToBack(true) }
                    )
                } else {
                    UnlockScreen(
                        activity = this,
                        repository = repository,
                        targetPackageLabel = targetPackage,
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

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }
}
