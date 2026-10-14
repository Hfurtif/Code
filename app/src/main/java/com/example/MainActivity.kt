package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.CodeViewModel
import com.example.ui.screens.CompanionNamingDialog
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.LanguageSelectionDialog
import com.example.ui.screens.LessonsScreen
import com.example.ui.screens.LevelTestDialog
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.screens.WebSearchDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

sealed class NavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Conversation : NavTab("conversation", "Conversation", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Lessons : NavTab("lessons", "Apprendre", Icons.Filled.School, Icons.Outlined.School)
    object Progress : NavTab("progress", "Progression", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    object Storage : NavTab("storage", "Stockage", Icons.Filled.Storage, Icons.Outlined.Storage)
    object Settings : NavTab("settings", "Réglages", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CodeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: CodeViewModel) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showLevelTestDialog by remember { mutableStateOf(false) }

    val infoBanner by viewModel.infoBannerMessage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLanguageDialogVisible by viewModel.isLanguageDialogVisible.collectAsState()
    val isWebSearchDialogVisible by viewModel.webSearchDialogVisible.collectAsState()
    val isCompanionNamingDialogVisible by viewModel.isCompanionNamingDialogVisible.collectAsState()
    val showInitialCompanionNamingDialog = !userProfile.hasNamedCompanion
    val showInitialLanguageDialog = userProfile.hasNamedCompanion && !userProfile.hasSelectedInitialLanguage

    // Runtime Permission for Microphone
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val tabs = listOf(
        NavTab.Conversation,
        NavTab.Lessons,
        NavTab.Progress,
        NavTab.Storage,
        NavTab.Settings
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                windowInsets = WindowInsets.navigationBars
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            selectedIconColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = NeonCyan,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Switcher
            when (selectedTabIndex) {
                0 -> ConversationScreen(viewModel = viewModel)
                1 -> LessonsScreen(
                    viewModel = viewModel,
                    onNavigateToConversation = { selectedTabIndex = 0 }
                )
                2 -> ProgressScreen(
                    viewModel = viewModel,
                    onLaunchLevelTest = { showLevelTestDialog = true }
                )
                3 -> StorageScreen(
                    viewModel = viewModel,
                    onNavigateToConversation = { selectedTabIndex = 0 }
                )
                4 -> SettingsScreen(viewModel = viewModel)
            }

            // Info Banner Notification Overlay (if any info/error message)
            infoBanner?.let { message ->
                Surface(
                    color = DarkSurface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissInfoBanner() }) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextSecondary)
                        }
                    }
                }
            }

            // Level Test Modal Dialog
            if (showLevelTestDialog) {
                LevelTestDialog(
                    viewModel = viewModel,
                    onDismiss = { showLevelTestDialog = false }
                )
            }

            // Companion Naming Modal Dialog (First launch or manual change from Settings)
            if (showInitialCompanionNamingDialog || isCompanionNamingDialogVisible) {
                CompanionNamingDialog(
                    initialName = userProfile.companionName,
                    initialGender = userProfile.companionGender,
                    initialVoiceGender = userProfile.voiceGender,
                    isFirstLaunch = showInitialCompanionNamingDialog,
                    onConfirm = { name, gender, voiceGender ->
                        viewModel.setCompanionName(name, gender, voiceGender)
                    },
                    onDismiss = {
                        viewModel.hideCompanionNamingDialog()
                    }
                )
            }

            // Language Selection Modal Dialog (First launch or manual change)
            if (showInitialLanguageDialog || isLanguageDialogVisible) {
                LanguageSelectionDialog(
                    currentLanguageCode = userProfile.preferredLanguageCode,
                    onSelectLanguage = { lang ->
                        viewModel.selectLanguage(lang)
                    },
                    onDismiss = {
                        viewModel.hideLanguageDialog()
                    },
                    isFirstLaunch = showInitialLanguageDialog
                )
            }

            // Autonomous Web Search Modal Dialog
            if (isWebSearchDialogVisible) {
                WebSearchDialog(
                    onDismiss = { viewModel.hideWebSearchDialog() },
                    onSearch = { query ->
                        viewModel.triggerWebSearch(query)
                    }
                )
            }
        }
    }
}
