package com.binaryxbeast.krewzo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

/**
 * Modern status badge with an animated pulsing radar dot for live active states
 * (SEARCHING and IN_PROGRESS) and refined semantic color pills.
 */
@Composable
fun KrewzoStatusBadge(
    status: BookingStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        BookingStatus.SEARCHING -> Triple(
            KrewzoTheme.colors.infoContainer,
            KrewzoTheme.colors.info,
            KrewzoTheme.colors.info
        )
        BookingStatus.PARTIALLY_FILLED -> Triple(
            KrewzoTheme.colors.warningContainer,
            KrewzoTheme.colors.warning,
            KrewzoTheme.colors.warning
        )
        BookingStatus.CONFIRMED -> Triple(
            KrewzoTheme.colors.successContainer,
            KrewzoTheme.colors.success,
            KrewzoTheme.colors.success
        )
        BookingStatus.IN_PROGRESS -> Triple(
            KrewzoTheme.colors.infoContainer,
            KrewzoTheme.colors.info,
            KrewzoTheme.colors.info
        )
        BookingStatus.COMPLETED -> Triple(
            KrewzoTheme.colors.surfaceTertiary,
            KrewzoTheme.colors.textPrimary,
            KrewzoTheme.colors.textSecondary
        )
        BookingStatus.CANCELLED, BookingStatus.EXPIRED -> Triple(
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            Color(0xFFDC2626)
        )
    }

    val isLive = status == BookingStatus.SEARCHING || status == BookingStatus.IN_PROGRESS
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.25f), RoundedCornerShape(KrewzoDimens.RadiusSm))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
