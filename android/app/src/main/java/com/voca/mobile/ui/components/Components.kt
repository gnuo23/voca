package com.voca.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.voca.mobile.ui.theme.VocaTheme

@Composable
fun DuoCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(2.dp, VocaTheme.brand.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = VocaTheme.brand.muted,
        modifier = modifier,
    )
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(2.dp, VocaTheme.brand.border, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = accent)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = VocaTheme.brand.muted,
        )
    }
}

@Composable
fun ProgressBarRounded(
    progress: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 14.dp,
    fill: Color = MaterialTheme.colorScheme.primary,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(clamped, label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(VocaTheme.brand.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

enum class PillKind { New, Learning, Familiar, Mastered, Difficult, Review, Close }

/** Maps a backend progress status string to a display label + pill color. */
fun statusPill(status: String?): Pair<String, PillKind> = when (status?.uppercase()) {
    "MASTERED" -> "Đã thuộc" to PillKind.Mastered
    "REVIEW" -> "Cần ôn" to PillKind.Review
    "LEARNING" -> "Đang học" to PillKind.Learning
    "FAMILIAR" -> "Quen mặt" to PillKind.Familiar
    "DIFFICULT" -> "Từ khó" to PillKind.Difficult
    else -> "Mới" to PillKind.New
}

@Composable
fun PillTag(text: String, kind: PillKind, modifier: Modifier = Modifier) {
    val brand = VocaTheme.brand
    val color = when (kind) {
        PillKind.New -> brand.muted
        PillKind.Learning -> brand.blue
        PillKind.Familiar -> brand.blue
        PillKind.Mastered -> brand.success
        PillKind.Difficult -> brand.danger
        PillKind.Review -> brand.yellow
        PillKind.Close -> brand.close
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun AudioButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = VocaTheme.brand
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(brand.blue.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Nghe",
            tint = brand.blue,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        MutedText(message)
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = VocaTheme.brand.danger)
        if (onRetry != null) {
            DuoButton("Thử lại", onRetry, style = DuoButtonStyle.Secondary)
        }
    }
}
