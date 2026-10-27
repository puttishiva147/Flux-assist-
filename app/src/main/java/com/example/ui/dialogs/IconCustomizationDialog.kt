package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ActionType
import com.example.data.models.ShortcutIconRegistry
import com.example.haptics.HapticManager
import com.example.haptics.HapticType

@Composable
fun IconCustomizationDialog(
    action: ActionType,
    currentCustomIconId: String?,
    accentColor: Color = Color(0xFF3B82F6),
    onIconSelected: (iconId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    var selectedIconId by remember { mutableStateOf(currentCustomIconId ?: "default") }

    val activeIconRes = remember(selectedIconId) {
        ShortcutIconRegistry.getIconRes(selectedIconId, action.iconRes)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14171E),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .border(1.dp, Color(0xFF2A3240), RoundedCornerShape(24.dp))
            .testTag("dialog_customize_icon"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live preview avatar of the satellite shortcut
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C212B))
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = activeIconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Customize Icon",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = stringResource(id = action.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select an icon for this shortcut in the radial menu:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    items(ShortcutIconRegistry.AVAILABLE_ICONS) { iconOption ->
                        val isSelected = selectedIconId == iconOption.id ||
                                (selectedIconId == "default" && iconOption.id == "default")
                        val displayRes = if (iconOption.id == "default") action.iconRes else iconOption.iconRes

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x223B82F6) else Color(0xFF1C212B))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color(0xFF2A3342),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    hapticManager.performHaptic(HapticType.TINY)
                                    selectedIconId = iconOption.id
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("icon_option_${iconOption.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = displayRes),
                                    contentDescription = iconOption.name,
                                    tint = if (isSelected) accentColor else Color(0xFFE2E8F0),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = iconOption.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hapticManager.performHaptic(HapticType.LIGHT)
                    onIconSelected(selectedIconId)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_custom_icon")
            ) {
                Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    hapticManager.performHaptic(HapticType.TINY)
                    selectedIconId = "default"
                    onIconSelected("default")
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_reset_custom_icon")
            ) {
                Text("Reset", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    )
}
