package com.voca.mobile.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.voca.mobile.ui.theme.VocaTheme

enum class DuoButtonStyle { Primary, Secondary, Danger, Blue }

/**
 * The signature Duolingo button: a solid rounded top face sitting on a darker
 * "shadow" base. Pressing pushes the face down onto the base for a tactile 3D feel.
 */
@Composable
fun DuoButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DuoButtonStyle = DuoButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val brand = VocaTheme.brand
    val scheme = MaterialTheme.colorScheme

    val (face, base, content) = when (style) {
        DuoButtonStyle.Primary -> Triple(scheme.primary, brand.primaryShadow, Color.White)
        DuoButtonStyle.Blue -> Triple(brand.blue, brand.blueShadow, Color.White)
        DuoButtonStyle.Danger -> Triple(brand.danger, brand.dangerShadow, Color.White)
        DuoButtonStyle.Secondary -> Triple(scheme.surface, brand.border, scheme.onSurface)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 4.dp
    val offset by animateDpAsState(if (pressed || !enabled) 0.dp else depth, label = "duo-press")

    val faceColor = if (enabled) face else face.copy(alpha = 0.5f)
    val baseColor = if (enabled) base else base.copy(alpha = 0.5f)
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(baseColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = offset, bottom = depth - offset)
                .defaultMinSize(minHeight = 52.dp)
                .clip(shape)
                .background(faceColor),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
