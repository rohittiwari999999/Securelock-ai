package com.securelock.ai.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.security.VaultRepository
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.screens.AppLockScreen
import com.securelock.ai.ui.screens.CalendarLaunchScreen
import com.securelock.ai.ui.screens.CameraPrivacyMonitorScreen
import com.securelock.ai.ui.screens.DashboardScreen
import com.securelock.ai.ui.screens.FakeCrashConfigScreen
import com.securelock.ai.ui.screens.IntruderSelfieScreen
import com.securelock.ai.ui.screens.LockNotificationsScreen
import com.securelock.ai.ui.screens.PermissionsManagerScreen
import com.securelock.ai.ui.screens.PrivateBrowserScreen
import com.securelock.ai.ui.screens.SettingsScreen
import com.securelock.ai.ui.screens.StatisticsScreen
import com.securelock.ai.ui.screens.ThemeCustomizationScreen
import com.securelock.ai.ui.screens.UnlockScreen
import com.securelock.ai.ui.screens.VaultScreen

object Routes {
    const val CALENDAR_LAUNCH = "calendar_launch"
    const val UNLOCK = "unlock"
    const val DASHBOARD = "dashboard"
    const val APP_LOCK = "app_lock"
    const val VAULT = "vault"
    const val INTRUDER_SELFIE = "intruder_selfie"
    const val LOCK_NOTIFICATIONS = "lock_notifications"
    const val FAKE_CRASH = "fake_crash"
    const val CAMERA_MONITOR = "camera_monitor"
    const val PRIVATE_BROWSER = "private_browser"
    const val STATISTICS = "statistics"
    const val THEME_CUSTOMIZATION = "theme_customization"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
}

@Composable
fun SecureLockNavHost(
    activity: FragmentActivity,
    repository: SecureStorageRepository,
    vaultRepository: VaultRepository
) {
    val navController = rememberNavController()
    val settings by repository.settings.collectAsState()
    val vaultUnlocked by AppLockSessionManager.vaultUnlockedFlow.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val needsSetup = !repository.hasPinConfigured()
    val secretDateActive = settings.secretDateConfig.enabled

    val startRoute = when {
        !vaultUnlocked && secretDateActive -> Routes.CALENDAR_LAUNCH
        !vaultUnlocked -> Routes.UNLOCK
        else -> Routes.DASHBOARD
    }

    // Automatically return to Calendar (or Unlock) when vault relocks in background / screen-off
    LaunchedEffect(vaultUnlocked, secretDateActive, backStackEntry) {
        val currentRoute = backStackEntry?.destination?.route
        if (!vaultUnlocked && currentRoute != null &&
            currentRoute != Routes.CALENDAR_LAUNCH &&
            currentRoute != Routes.UNLOCK
        ) {
            val lockTarget = if (secretDateActive) {
                Routes.CALENDAR_LAUNCH
            } else {
                Routes.UNLOCK
            }
            navController.navigate(lockTarget) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable(Routes.CALENDAR_LAUNCH) {
            CalendarLaunchScreen(
                repository = repository,
                onSecretUnlocked = {
                    AppLockSessionManager.unlockVault()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.CALENDAR_LAUNCH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.UNLOCK) {
            UnlockScreen(
                activity = activity,
                repository = repository,
                targetPackageLabel = "SecureLock AI",
                targetPackageName = null,
                isSetupFlow = needsSetup,
                onUnlocked = {
                    AppLockSessionManager.unlockVault()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            if (!vaultUnlocked) return@composable

            BackHandler(enabled = secretDateActive) {
                AppLockSessionManager.lockVault()
                navController.navigate(Routes.CALENDAR_LAUNCH) {
                    popUpTo(0) { inclusive = true }
                }
            }

            DashboardScreen(
                repository = repository,
                vaultRepository = vaultRepository,
                onNavigate = { route -> navController.navigate(route) },
                onLockNow = {
                    AppLockSessionManager.lockVault()
                    val lockTarget = if (secretDateActive) Routes.CALENDAR_LAUNCH else Routes.UNLOCK
                    navController.navigate(lockTarget) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.APP_LOCK) {
            AppLockScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) }
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                activity = activity,
                repository = repository,
                vaultRepository = vaultRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.INTRUDER_SELFIE) {
            IntruderSelfieScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LOCK_NOTIFICATIONS) {
            LockNotificationsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FAKE_CRASH) {
            FakeCrashConfigScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onTestAuthReturn = {
                    AppLockSessionManager.lockVault()
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CAMERA_MONITOR) {
            CameraPrivacyMonitorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRIVATE_BROWSER) {
            PrivateBrowserScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.THEME_CUSTOMIZATION) {
            ThemeCustomizationScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenThemes = { navController.navigate(Routes.THEME_CUSTOMIZATION) }
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionsManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
