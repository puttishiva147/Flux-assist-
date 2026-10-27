package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ActionType

@Composable
fun ActionCustomizationDialog(
    slotIndex: Int,
    currentAction: ActionType,
    onActionSelected: (ActionType) -> Unit,
    onDismiss: () -> Unit
) {
    val graphiteSurface = Color(0xFF14171E)
    val graphiteBorder = Color(0xFF2A3240)
    val refinedBlue = Color(0xFF3B82F6)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = graphiteSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, graphiteBorder, RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Configure Slot #${slotIndex + 1}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFF8FAFC)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a system gesture or utility action for this shortcut:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .testTag("action_selection_list")
                ) {
                    items(ActionType.entries) { action ->
                        val isSelected = action == currentAction
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) refinedBlue.copy(alpha = 0.18f)
                                    else Color(0xFF1A1F28)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) refinedBlue else Color(0xFF252D3A),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onActionSelected(action)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("action_item_${action.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (isSelected) refinedBlue.copy(alpha = 0.25f) else Color(0xFF222834),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = action.iconRes),
                                    contentDescription = null,
                                    tint = if (isSelected) refinedBlue else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = action.titleRes),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = action.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = refinedBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_btn")
            ) {
                Text("Cancel", color = refinedBlue)
            }
        }
    )
}
