package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import com.example.R
import com.example.actions.ActionExecutor
import com.example.data.models.ActionType
import com.example.data.models.BallStyle
import com.example.data.models.FloatingShape
import com.example.data.models.MenuStyle
import com.example.haptics.HapticIntensity
import com.example.haptics.HapticType
import com.example.receivers.AssistXDeviceAdminReceiver
import com.example.ui.components.ActionCustomizationDialog
import com.example.ui.components.AppPickerDialog
import com.example.ui.components.BallPreviewCard
import com.example.ui.components.PermissionCard
import com.example.ui.components.RadialMenu
import com.example.ui.components.RadialMenuItem
import com.example.ui.dialogs.IconCustomizationDialog
import com.example.data.models.ShortcutIconRegistry
import com.example.ui.viewmodel.AssistXViewModel
import com.example.utils.AppHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainSettingsScreen(
    viewModel: AssistXViewModel,
    onRequestNotificationPermission: () -> Unit,
    initialCustomizeActionId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var customizingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var customizingActionForIcon by remember {
        mutableStateOf(initialCustomizeActionId?.let { ActionType.fromId(it) })
    }

    val refinedBlue = Color(0xFF3B82F6)
    val graphiteCard = Color(0xFF14171E)
    val graphiteBorder = Color(0xFF2A3240)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(refinedBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = "System Navigation & Floating Controls",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. MASTER ACTIVATION CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("master_switch_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.isServiceEnabled)
                            Color(0xFF131A26)
                        else
                            graphiteCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (settings.isServiceEnabled) refinedBlue.copy(alpha = 0.6f) else graphiteBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assistive Floating Overlay",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = if (settings.isServiceEnabled)
                                    "Active on screen • Tap to expand System Navigation"
                                else
                                    "Enable to display floating assistive bar over other apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Switch(
                            checked = settings.isServiceEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !permissions.hasOverlay) {
                                    showPermissionDialog = true
                                } else {
                                    viewModel.toggleService(isChecked)
                                }
                            },
                            modifier = Modifier.testTag("master_service_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = refinedBlue,
                                checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            // 2. CRITICAL: ACCESSIBILITY SYSTEM NAVIGATION HERO BANNER
            item {
                if (permissions.hasAccessibility) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_accessibility_active"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF111822)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E3A8A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Accessibility,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Gestures Active",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "Real Back, Home, Recent Apps, Screenshot & Lock actions are ready",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_accessibility_required"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161922)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, refinedBlue)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF1E3A8A), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Accessibility,
                                        contentDescription = null,
                                        tint = refinedBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable System Navigation",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFF8FAFC)
                                    )
                                    Text(
                                        text = "Required for real Back, Home, Recents & Screenshot actions",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = refinedBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "To allow the on-screen Back button to navigate back in your apps, turn on AssistX in Android Accessibility Settings.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { openAccessibilitySettings(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_activate_accessibility"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB)
                                )
                            ) {
                                Text("Turn On Accessibility Service", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 3. LIVE INTERACTIVE BALL PREVIEW
            item {
                BallPreviewCard(settings = settings)
            }

            // 4. SHORTCUT MENU STYLE & LAYOUT
            item {
                SectionHeader(title = "SHORTCUT MENU STYLE & LAYOUT")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_style_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select how shortcuts appear when tapping the floating bar:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuStyle.entries.forEach { style ->
                                val isSelected = settings.menuStyle == style
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateMenuStyle(style) }
                                        .testTag("card_menu_style_${style.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF1A212E) else Color(0xFF181C25)
                                    ),
                                    border = if (isSelected)
                                        androidx.compose.foundation.BorderStroke(1.5.dp, refinedBlue)
                                    else
                                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26303E))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = style.displayName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFFCBD5E1)
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(refinedBlue, CircleShape)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = style.subtitle,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) refinedBlue else Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = style.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                            color = Color(0xFF94A3B8),
                                            minLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. CIRCULAR RADIAL MENU COMPONENT (TRIGONOMETRY)
            item {
                SectionHeader(title = "CIRCULAR RADIAL MENU (TRIGONOMETRY)")
            }

            item {
                RadialMenuShowcaseCard(
                    actions = settings.actions,
                    currentMenuStyle = settings.menuStyle,
                    currentCenterShape = settings.radialCenterShape,
                    currentAccentColor = settings.radialAccentColor,
                    customIcons = settings.customIcons,
                    onSelectMenuStyle = { viewModel.updateMenuStyle(it) },
                    onSelectCenterShape = { viewModel.updateRadialCenterShape(it) },
                    onSelectAccentColor = { viewModel.updateRadialAccentColor(it) },
                    onCustomizeIcon = { action -> customizingActionForIcon = action },
                    onExecuteAction = { action ->
                        ActionExecutor(context).execute(action) { /* on close */ }
                    }
                )
            }

            // 6. FLOATING FORM FACTOR & FREEDOM OF MOVEMENT
            item {
                SectionHeader(title = "SHAPE & SCREEN MOVEMENT")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Shape Selector
                        Text("Form Factor", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FloatingShape.entries.forEach { shape ->
                                val isSelected = settings.floatingShape == shape
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateFloatingShape(shape) },
                                    label = { Text(shape.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = refinedBlue.copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFFF8FAFC)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) refinedBlue else graphiteBorder
                                    ),
                                    modifier = Modifier.testTag("chip_shape_${shape.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Move Anywhere on Screen vs Edge Snapping
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Snap to Screen Edges", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                Text(
                                    text = if (settings.edgeSnapping)
                                        "Snaps to the closest edge when released"
                                    else
                                        "Free Movement: Drag and place anywhere across your entire screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Switch(
                                checked = settings.edgeSnapping,
                                onCheckedChange = { viewModel.updateEdgeSnapping(it) },
                                modifier = Modifier.testTag("switch_edge_snapping"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = refinedBlue,
                                    checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }

            // 5. QUICK LAUNCH APPS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "QUICK LAUNCH APPS")
                    OutlinedButton(
                        onClick = { showAppPickerDialog = true },
                        modifier = Modifier.testTag("btn_add_app"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, refinedBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = refinedBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add App", fontSize = 12.sp, color = refinedBlue)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (settings.quickApps.isEmpty()) {
                            Text(
                                text = "No quick apps pinned yet. Tap 'Add App' to pin apps for instant 1-tap launching in the overlay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            Text(
                                text = "Pinned apps appear in the top row of your floating system menu:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(settings.quickApps) { pkg ->
                                    val appItem = remember(pkg) { AppHelper.getAppItem(context, pkg) }
                                    if (appItem != null) {
                                        QuickAppChip(
                                            appName = appItem.appName,
                                            icon = appItem.icon,
                                            onRemove = { viewModel.removeQuickApp(pkg) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. SYSTEM STYLES (THEMES)
            item {
                SectionHeader(title = "GRAPHITE SYSTEM STYLES")
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(BallStyle.entries) { style ->
                        val isSelected = style == settings.ballStyle
                        Card(
                            modifier = Modifier
                                .width(128.dp)
                                .clickable { viewModel.updateBallStyle(style) }
                                .testTag("style_card_${style.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color(0xFF1A212E)
                                else
                                    graphiteCard
                            ),
                            border = if (isSelected)
                                androidx.compose.foundation.BorderStroke(1.5.dp, refinedBlue)
                            else
                                androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(style.innerCoreColor), CircleShape)
                                        .border(1.dp, Color(style.ringColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(style.primaryColor), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 7. APPEARANCE & PHYSICS
            item {
                SectionHeader(title = "APPEARANCE & PHYSICS")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Size Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bar Size", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                            Text("${settings.ballSizeDp} dp", style = MaterialTheme.typography.bodyMedium, color = refinedBlue)
                        }
                        Slider(
                            value = settings.ballSizeDp.toFloat(),
                            onValueChange = { viewModel.updateBallSize(it.toInt()) },
                            valueRange = 42f..72f,
                            steps = 5,
                            modifier = Modifier.testTag("slider_ball_size")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Opacity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Opacity", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                            Text("${(settings.ballOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = refinedBlue)
                        }
                        Slider(
                            value = settings.ballOpacity,
                            onValueChange = { viewModel.updateBallOpacity(it) },
                            valueRange = 0.25f..1.0f,
                            modifier = Modifier.testTag("slider_ball_opacity")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-Fade on Idle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Fade on Inactivity", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                Text("Dim to subtle transparency after 3 seconds idle", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = settings.autoFadeOnIdle,
                                onCheckedChange = { viewModel.updateAutoFade(it) },
                                modifier = Modifier.testTag("switch_auto_fade"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = refinedBlue,
                                    checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                )
                            )
                        }

                        AnimatedVisibility(visible = settings.autoFadeOnIdle) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Idle Dim Level", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Text("${(settings.idleFadeOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = refinedBlue)
                                }
                                Slider(
                                    value = settings.idleFadeOpacity,
                                    onValueChange = { viewModel.updateIdleOpacity(it) },
                                    valueRange = 0.15f..0.50f,
                                    modifier = Modifier.testTag("slider_idle_opacity")
                                )
                            }
                        }
                    }
                }
            }

            // 8. HAPTICS & MICRO-INTERACTIONS
            item {
                SectionHeader(title = "HAPTICS & MICRO-INTERACTIONS")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("haptics_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Haptic Feedback", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                Text("Tactile haptic impulses during touch, drag, and shortcut actions", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = settings.hapticFeedback,
                                onCheckedChange = { viewModel.updateHaptic(it) },
                                modifier = Modifier.testTag("switch_haptic_master"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = refinedBlue,
                                    checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                )
                            )
                        }

                        AnimatedVisibility(visible = settings.hapticFeedback) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Haptic Intensity", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HapticIntensity.entries.forEach { intensity ->
                                        val isSelected = settings.hapticIntensity == intensity
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.updateHapticIntensity(intensity) },
                                            label = { Text(intensity.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = refinedBlue.copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFF8FAFC)
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) refinedBlue else graphiteBorder
                                            ),
                                            modifier = Modifier.testTag("chip_intensity_${intensity.id}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Action Shortcut Haptics", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                        Text("Tactile click when triggering shortcuts", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                    Switch(
                                        checked = settings.actionHapticsEnabled,
                                        onCheckedChange = { viewModel.updateActionHaptics(it) },
                                        modifier = Modifier.testTag("switch_action_haptics"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = refinedBlue,
                                            checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Drag & Snap Haptics", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                        Text("Impulse on drag start and release snap", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                    Switch(
                                        checked = settings.dragHapticsEnabled,
                                        onCheckedChange = { viewModel.updateDragHaptics(it) },
                                        modifier = Modifier.testTag("switch_drag_haptics"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = refinedBlue,
                                            checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Test Haptic Patterns",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = refinedBlue
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.testHaptic(HapticType.TINY) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Tick", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.testHaptic(HapticType.LIGHT) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Tap", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.testHaptic(HapticType.MEDIUM) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Drop", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.testHaptic(HapticType.SUCCESS) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Success", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. SYSTEM SHORTCUT SLOTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "SYSTEM SHORTCUT SLOTS")
                    TextButton(
                        onClick = { viewModel.resetMenuDefaults() },
                        modifier = Modifier.testTag("btn_reset_menu")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = refinedBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 12.sp, color = refinedBlue)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4, 6, 8).forEach { count ->
                        val isSelected = settings.actions.size == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setActionCount(count) },
                            label = { Text("$count Slots") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = refinedBlue.copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFFF8FAFC)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) refinedBlue else graphiteBorder
                            ),
                            modifier = Modifier.testTag("chip_count_$count")
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tap any slot below to customize its system action:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val actions = settings.actions
                        val columns = if (actions.size <= 4) 2 else if (actions.size <= 6) 3 else 4
                        val rows = (actions.size + columns - 1) / columns

                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (c in 0 until columns) {
                                    val index = r * columns + c
                                    if (index < actions.size) {
                                        val action = actions[index]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF1C212B))
                                                .border(
                                                    1.dp,
                                                    Color(0xFF2A3342),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .combinedClickable(
                                                    onClick = { customizingSlotIndex = index },
                                                    onLongClick = { customizingActionForIcon = action }
                                                )
                                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                                .testTag("slot_item_$index"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val customIconId = settings.customIcons[action.id]
                                            val displayIconRes = ShortcutIconRegistry.getIconRes(customIconId, action.iconRes)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(id = displayIconRes),
                                                    contentDescription = null,
                                                    tint = if (customIconId != null) refinedBlue else Color(0xFFF8FAFC),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(id = action.titleRes),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color(0xFFCBD5E1),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 10. PERMISSIONS & CAPABILITIES SECTION
            item {
                SectionHeader(title = "PERMISSIONS & CAPABILITIES")
            }

            item {
                PermissionCard(
                    title = stringResource(id = R.string.permission_overlay_title),
                    description = stringResource(id = R.string.permission_overlay_desc),
                    isGranted = permissions.hasOverlay,
                    isRequired = true,
                    icon = Icons.Default.Layers,
                    onGrantClick = {
                        openOverlaySettings(context)
                    }
                )
            }

            item {
                PermissionCard(
                    title = stringResource(id = R.string.permission_accessibility_title),
                    description = "Required to execute Back, Home, Recents, Screenshot, and Lock actions system-wide.",
                    isGranted = permissions.hasAccessibility,
                    isRequired = true,
                    icon = Icons.Default.Accessibility,
                    onGrantClick = {
                        openAccessibilitySettings(context)
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionCard(
                        title = stringResource(id = R.string.permission_notification_title),
                        description = stringResource(id = R.string.permission_notification_desc),
                        isGranted = permissions.hasNotification,
                        isRequired = false,
                        icon = Icons.Default.Notifications,
                        onGrantClick = {
                            onRequestNotificationPermission()
                        }
                    )
                }
            }

            item {
                PermissionCard(
                    title = "Modify System Settings",
                    description = "Allows adjusting screen brightness directly from the floating slider.",
                    isGranted = permissions.hasWriteSettings,
                    isRequired = false,
                    icon = Icons.Default.Settings,
                    onGrantClick = {
                        openWriteSettings(context)
                    }
                )
            }

            item {
                PermissionCard(
                    title = "Device Administrator",
                    description = "Alternative system permission used to instantly lock the screen.",
                    isGranted = permissions.hasDeviceAdmin,
                    isRequired = false,
                    icon = Icons.Default.Lock,
                    onGrantClick = {
                        openDeviceAdminEnrollment(context)
                    }
                )
            }

            // 11. START ON BOOT
            item {
                SectionHeader(title = "SYSTEM BEHAVIOR")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start on Device Boot", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFFF8FAFC))
                                Text("Resume floating bar automatically after phone reboot", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = settings.startOnBoot,
                                onCheckedChange = { viewModel.updateStartOnBoot(it) },
                                modifier = Modifier.testTag("switch_start_on_boot"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = refinedBlue,
                                    checkedTrackColor = refinedBlue.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }

            // 12. ABOUT & VERSION
            item {
                SectionHeader(title = "ABOUT ASSISTX")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_about_app"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = graphiteCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, graphiteBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AssistX System Utility",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "Version 1.5.0 • Build 8",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = refinedBlue
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x223B82F6),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "System navigation controls, Glass UI, Dual Floating Form Factors (Floating Bar & Circular Orb), and Trigonometric Circular Radial Menu with customizable center anchors.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }

    // Permission Prompt Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = graphiteCard,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, graphiteBorder, RoundedCornerShape(24.dp)),
            title = {
                Text("Display Over Other Apps", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF8FAFC))
            },
            text = {
                Text(
                    "To display the floating assistive bar on your screen, Android requires the 'Display over other apps' permission. Tap Continue to enable it in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        openOverlaySettings(context)
                    },
                    modifier = Modifier.testTag("dialog_grant_overlay_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // App Picker Dialog
    if (showAppPickerDialog) {
        AppPickerDialog(
            onDismiss = { showAppPickerDialog = false },
            onAppSelected = { pkg ->
                viewModel.addQuickApp(pkg)
            }
        )
    }

    // Action Customization Dialog
    customizingSlotIndex?.let { slotIdx ->
        val currentAction = settings.actions.getOrNull(slotIdx) ?: ActionType.HOME
        ActionCustomizationDialog(
            slotIndex = slotIdx,
            currentAction = currentAction,
            onActionSelected = { selected ->
                viewModel.updateActionSlot(slotIdx, selected)
                customizingSlotIndex = null
            },
            onDismiss = {
                customizingSlotIndex = null
            }
        )
    }

    // Icon Customization Dialog (from radial long-press or direct edit)
    customizingActionForIcon?.let { action ->
        IconCustomizationDialog(
            action = action,
            currentCustomIconId = settings.customIcons[action.id],
            accentColor = Color(settings.radialAccentColor.toInt()),
            onIconSelected = { iconId ->
                viewModel.updateCustomIconForAction(action.id, iconId)
                customizingActionForIcon = null
            },
            onDismiss = {
                customizingActionForIcon = null
            }
        )
    }
}

@Composable
private fun QuickAppChip(
    appName: String,
    icon: android.graphics.drawable.Drawable?,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F29)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3342))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = remember(icon) {
                try {
                    icon?.toBitmap(28, 28)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appName,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = appName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFF1F5F9),
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun RadialMenuShowcaseCard(
    actions: List<ActionType>,
    currentMenuStyle: MenuStyle,
    currentCenterShape: FloatingShape,
    currentAccentColor: Long,
    customIcons: Map<String, String>,
    onSelectMenuStyle: (MenuStyle) -> Unit,
    onSelectCenterShape: (FloatingShape) -> Unit,
    onSelectAccentColor: (Long) -> Unit,
    onCustomizeIcon: (ActionType) -> Unit,
    onExecuteAction: (ActionType) -> Unit
) {
    val context = LocalContext.current
    val isRadialActiveOnOverlay = currentMenuStyle == MenuStyle.RADIAL_ORBIT
    var isRadialOpen by remember { mutableStateOf(false) }
    var radialRadius by remember { mutableFloatStateOf(92f) }
    var radialSweep by remember { mutableFloatStateOf(360f) }

    val radialAccentColors = remember {
        listOf(
            Triple("Refined Blue", 0xFF3B82F6L, Color(0xFF3B82F6)),
            Triple("Cyber Cyan", 0xFF06B6D4L, Color(0xFF06B6D4)),
            Triple("Electric Violet", 0xFF8B5CF6L, Color(0xFF8B5CF6)),
            Triple("Emerald Green", 0xFF10B981L, Color(0xFF10B981)),
            Triple("Amber Gold", 0xFFF59E0BL, Color(0xFFF59E0B)),
            Triple("Crimson Rose", 0xFFF43F5EL, Color(0xFFF43F5E)),
            Triple("Pure White", 0xFFF8FAFCL, Color(0xFFF8FAFC))
        )
    }

    val radialItems = remember(actions, currentAccentColor, customIcons) {
        actions.map { action ->
            val isFrequent = action == ActionType.FREQUENT_APP
            val frequentApp = if (isFrequent) com.example.utils.AppUsageTracker.getMostFrequentlyUsedApp(context) else null
            val labelText = if (isFrequent && frequentApp != null) "★ ${frequentApp.appName}" else context.getString(action.titleRes)
            val customIconId = customIcons[action.id]
            val iconRes = ShortcutIconRegistry.getIconRes(customIconId, action.iconRes)

            RadialMenuItem(
                id = action.id,
                label = labelText,
                iconRes = iconRes,
                contentDescription = labelText,
                backgroundColor = if (isFrequent) Color(0xFF1E2235) else Color(0xFF1C212B),
                borderColor = if (isFrequent || customIconId != null) Color(currentAccentColor.toInt()) else Color(0xFF2A3342),
                iconTint = if (isFrequent || customIconId != null) Color(currentAccentColor.toInt()) else Color(0xFFF8FAFC),
                onLongClick = {
                    onCustomizeIcon(action)
                },
                onClick = {
                    if (isFrequent && frequentApp != null) {
                        com.example.utils.AppUsageTracker.recordAppLaunch(context, frequentApp.packageName)
                        Toast.makeText(context, "Frequently Used: ${frequentApp.appName}", Toast.LENGTH_SHORT).show()
                        com.example.utils.AppHelper.launchApp(context, frequentApp.packageName)
                    } else {
                        Toast.makeText(context, "Radial Action: ${context.getString(action.titleRes)}", Toast.LENGTH_SHORT).show()
                        onExecuteAction(action)
                    }
                }
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radial_menu_showcase_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14171E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3240))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trigonometric Orbital Layout",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Positions icons using X = r · cos(θ), Y = r · sin(θ)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRadialOpen) Color(currentAccentColor.toInt()).copy(alpha = 0.2f) else Color(0xFF1E232E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRadialOpen) Color(currentAccentColor.toInt()) else Color(0xFF2A3342)
                    )
                ) {
                    Text(
                        text = if (isRadialOpen) "OPEN" else "CLOSED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isRadialOpen) Color(currentAccentColor.toInt()) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Anchor Form Factor Option Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Center Anchor Face",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFF8FAFC)
                )
                Text(
                    text = "Choose whether the radial orbit expands from the Floating Bar or Circular Orb",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FloatingShape.entries.forEach { shape ->
                        val isSelected = currentCenterShape == shape
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCenterShape(shape) },
                            label = {
                                Text(
                                    if (shape == FloatingShape.PILL_BAR) "Floating Bar (Pill)" else "Circular Orb (FAB)"
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(currentAccentColor.toInt()).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFFF8FAFC)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color(currentAccentColor.toInt()) else Color(0xFF2A3240)
                            ),
                            modifier = Modifier.testTag("chip_radial_center_${shape.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Accent Color Option Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Primary Accent Color",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFF8FAFC)
                    )
                    val activeColorName = radialAccentColors.find { it.second == currentAccentColor }?.first ?: "Custom"
                    Text(
                        text = activeColorName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(currentAccentColor.toInt())
                    )
                }
                Text(
                    text = "Dynamically updates the central trigger glow, border, and active status",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(radialAccentColors) { (name, colorVal, colorObj) ->
                        val isSelected = currentAccentColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorObj)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { onSelectAccentColor(colorVal) }
                                .testTag("color_picker_${name.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (colorVal == 0xFFF8FAFCL) Color.Black else Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Floating Menu Banner or Activation Button
            if (isRadialActiveOnOverlay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF122238), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(currentAccentColor.toInt()).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(currentAccentColor.toInt()), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active overlay style on screen: Tapping the floating bar expands this radial orbit",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFBAE6FD)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectMenuStyle(MenuStyle.RADIAL_ORBIT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_set_radial_overlay"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentAccentColor.toInt()))
                ) {
                    Text(
                        text = "Set Radial Orbit as Floating Bar Menu",
                        color = Color(currentAccentColor.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive RadialMenu Stage Canvas showcasing Modern Minimalist Glassmorphism Blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F1217))
                    .border(1.dp, Color(0xFF222834), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background ambient geometric glow spots that adaptively blur through the Glassmorphism container
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-45).dp, y = (-35).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(currentAccentColor.toInt()).copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .offset(x = 55.dp, y = 40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                RadialMenu(
                    items = radialItems,
                    isOpen = isRadialOpen,
                    onToggle = { isRadialOpen = !isRadialOpen },
                    radius = radialRadius.dp,
                    sweepAngleDegrees = radialSweep,
                    startAngleDegrees = -90f,
                    centerShape = currentCenterShape,
                    accentColor = Color(currentAccentColor.toInt()),
                    centralFabColor = Color(currentAccentColor.toInt()),
                    centralFabIconColor = Color.White,
                    showLabels = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hint for long-press icon customization
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B24), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF283142), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Long-press any shortcut icon above to customize its symbol",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = Color(currentAccentColor.toInt())
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Controls for Radius and Sweep Angle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Orbital Radius: ${radialRadius.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
            Slider(
                value = radialRadius,
                onValueChange = { radialRadius = it },
                valueRange = 70f..115f,
                modifier = Modifier.testTag("slider_radial_radius")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Angular Spread: ${radialSweep.toInt()}° (${if (radialSweep >= 360f) "Full Orbit" else "Arc"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
            Slider(
                value = radialSweep,
                onValueChange = { radialSweep = it },
                valueRange = 180f..360f,
                steps = 3,
                modifier = Modifier.testTag("slider_radial_sweep")
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        ),
        color = Color(0xFF3B82F6),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val generalIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(generalIntent)
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
    }
}

private fun openWriteSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val generalIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(generalIntent)
        }
    }
}

private fun openDeviceAdminEnrollment(context: Context) {
    val comp = AssistXDeviceAdminReceiver.getComponentName(context)
    val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
        putExtra(
            android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            context.getString(R.string.device_admin_description)
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
    }
}
