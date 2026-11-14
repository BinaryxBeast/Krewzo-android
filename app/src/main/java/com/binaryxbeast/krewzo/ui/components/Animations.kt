package com.binaryxbeast.krewzo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.delay

/**
 * Shimmer effect modifier for skeleton/placeholder loading screens.
 * Uses a diagonal linear gradient sweep to reduce cognitive friction and perceived wait times.
 */
fun Modifier.shimmerEffect(
    cornerRadius: Dp = 8.dp
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.8f),
        KrewzoTheme.colors.surfaceTertiary.copy(alpha = 0.35f),
        KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.8f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(brush)
}

/**
 * Fluid Dual-Ring Express Loading Circle.
 * Features an outer subtle background orbit track and an inner high-speed accelerating sweep arc
 * with rounded end caps and smooth harmonic easing.
 */
@Composable
fun ExpressLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.18f),
    strokeWidth: Dp = 2.5.dp
) {
    val transition = rememberInfiniteTransition(label = "expressLoader")

    // Rotation angle: 0 to 360
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loaderRotation"
    )

    // Sweep angle expands and contracts (harmonic pulse)
    val sweepAngle by transition.animateFloat(
        initialValue = 50f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loaderSweep"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.toPx() - stroke, size.toPx() - stroke)
        val offset = Offset(stroke / 2f, stroke / 2f)

        // Outer static track ring
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = offset,
            size = arcSize,
            style = Stroke(width = stroke)
        )

        // Inner dynamic spinning arc
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = offset,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Physics-based spring press modifier for cards, list items, and interactive surfaces.
 * Uses high-fidelity tactile spring response and instant haptic feedback.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 400f
        ),
        label = "bouncyPressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

/**
 * Controller to trigger a tactile horizontal shake animation (e.g. for input error, invalid code).
 */
class ShakeController {
    var shakeTrigger by mutableIntStateOf(0)
        private set

    fun shake() {
        shakeTrigger++
    }
}

@Composable
fun rememberShakeController(): ShakeController {
    return remember { ShakeController() }
}

/**
 * Tactile horizontal shake effect modifier for error feedback.
 */
fun Modifier.shakeEffect(
    trigger: Int
): Modifier = composed {
    val animOffset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            animOffset.snapTo(0f)
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    0f at 0
                    (-14f) at 45
                    14f at 90
                    (-10f) at 135
                    10f at 180
                    (-6f) at 225
                    6f at 270
                    (-2f) at 315
                    0f at 360
                }
            )
        }
    }

    this.graphicsLayer {
        translationX = animOffset.value
    }
}

/**
 * Cascading staggered entrance container for list items.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 45L).coerceAtMost(350L))
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "staggeredAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggeredOffsetY"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        }
    ) {
        content()
    }
}

/**
 * Exact geometric skeleton card for bookings.
 */
@Composable
fun BookingCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).shimmerEffect(12.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Box(modifier = Modifier.width(110.dp).height(16.dp).shimmerEffect(6.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.width(70.dp).height(12.dp).shimmerEffect(6.dp))
                    }
                }
                Box(modifier = Modifier.width(74.dp).height(24.dp).shimmerEffect(12.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp).shimmerEffect(6.dp))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(90.dp).height(18.dp).shimmerEffect(6.dp))
                Box(modifier = Modifier.width(70.dp).height(32.dp).shimmerEffect(10.dp))
            }
        }
    }
}

/**
 * Geometric skeleton card for workers.
 */
@Composable
fun WorkerCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).shimmerEffect(23.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(120.dp).height(16.dp).shimmerEffect(6.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(80.dp).height(12.dp).shimmerEffect(6.dp))
                }
            }
            Box(modifier = Modifier.size(38.dp).shimmerEffect(19.dp))
        }
    }
}

/**
 * Concentric expanding radar pulse rings for live broadcasts, dispatch waves, and matching workers.
 */
@Composable
fun RadarPulseAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    pulseColor: Color = tint,
    centerContent: @Composable () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Wave 3
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = 0.7f + (pulse3 * 0.9f)
                    scaleY = 0.7f + (pulse3 * 0.9f)
                    alpha = (1f - pulse3).coerceIn(0f, 0.45f)
                }
                .clip(CircleShape)
                .background(pulseColor)
        )

        // Wave 2
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = 0.6f + (pulse2 * 0.8f)
                    scaleY = 0.6f + (pulse2 * 0.8f)
                    alpha = (1f - pulse2).coerceIn(0f, 0.55f)
                }
                .clip(CircleShape)
                .background(pulseColor)
        )

        // Wave 1
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = 0.5f + (pulse1 * 0.7f)
                    scaleY = 0.5f + (pulse1 * 0.7f)
                    alpha = (1f - pulse1).coerceIn(0f, 0.65f)
                }
                .clip(CircleShape)
                .background(pulseColor)
        )

        // Center content
        centerContent()
    }
}

/**
 * Pulsing live status dot with halo breathing ring.
 */
@Composable
fun PulsingStatusDot(
    modifier: Modifier = Modifier,
    color: Color = KrewzoTheme.colors.success,
    size: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    val haloAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing halo
        Box(
            modifier = Modifier
                .size(size * 2)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = haloAlpha
                }
                .clip(CircleShape)
                .background(color)
        )

        // Inner solid dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Animated Search Bar Ticker rotating through popular query strings with vertical slide.
 */
@Composable
fun SearchTicker(
    hints: List<String>,
    modifier: Modifier = Modifier,
    intervalMillis: Long = 3200L
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(hints) {
        while (true) {
            delay(intervalMillis)
            if (hints.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % hints.size
            }
        }
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = {
            (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn())
                .togetherWith(slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut())
        },
        label = "searchTickerAnim",
        modifier = modifier
    ) { index ->
        val hint = hints.getOrNull(index) ?: ""
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = KrewzoTheme.colors.textTertiary,
            maxLines = 1
        )
    }
}

/**
 * High-visibility star rating pill badge.
 */
@Composable
fun RatingBadge(
    rating: Double,
    reviewCount: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = KrewzoTheme.colors.ratingGreen
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%.1f", rating),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            if (reviewCount != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "($reviewCount)",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Quick category circular pill card with spring tap feedback.
 */
@Composable
fun QuickCategoryCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "categoryCardScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else containerColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.textPrimary,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/**
 * Stepper step definition.
 */
data class StepperStep(
    val title: String,
    val subtitle: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

/**
 * Express Delivery-style multi-step timeline tracker for active live orders.
 */
@Composable
fun LiveOrderTimeline(
    steps: List<StepperStep>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Indicator column (circle + connecting line)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    if (step.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(KrewzoTheme.colors.ratingGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else if (step.isCurrent) {
                        RadarPulseAnimation(
                            modifier = Modifier.size(28.dp),
                            pulseColor = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(KrewzoTheme.colors.surfaceSecondary)
                                .border(1.dp, KrewzoTheme.colors.border, CircleShape)
                        )
                    }

                    // Connecting vertical line if not last
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(32.dp)
                                .background(
                                    if (step.isCompleted) KrewzoTheme.colors.ratingGreen
                                    else KrewzoTheme.colors.border
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Step content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (index < steps.size - 1) 16.dp else 0.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (step.isCurrent || step.isCompleted) FontWeight.Bold else FontWeight.Normal,
                        color = if (step.isCurrent || step.isCompleted) KrewzoTheme.colors.textPrimary else KrewzoTheme.colors.textTertiary
                    )
                    if (step.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (step.isCurrent) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (step.isCurrent) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status-driven convenience overload for LiveOrderTimeline.
 */
@Composable
fun LiveOrderTimeline(
    currentStatus: com.binaryxbeast.krewzo.core.model.BookingStatus,
    modifier: Modifier = Modifier,
    currentWave: Int = 1,
    workersAccepted: Int = 0,
    workersNeeded: Int = 1
) {
    val isSearching = currentStatus == com.binaryxbeast.krewzo.core.model.BookingStatus.SEARCHING
    val isPartially = currentStatus == com.binaryxbeast.krewzo.core.model.BookingStatus.PARTIALLY_FILLED
    val isConfirmed = currentStatus == com.binaryxbeast.krewzo.core.model.BookingStatus.CONFIRMED
    val isInProgress = currentStatus == com.binaryxbeast.krewzo.core.model.BookingStatus.IN_PROGRESS
    val isCompleted = currentStatus == com.binaryxbeast.krewzo.core.model.BookingStatus.COMPLETED

    val steps = listOf(
        StepperStep(
            title = "Order Created & Dispatched",
            subtitle = "Wave $currentWave broadcast to active crew in Pune",
            isCompleted = true,
            isCurrent = false
        ),
        StepperStep(
            title = "Worker Matching",
            subtitle = if (isSearching) "Searching available crew nearby..." else "Workers matched",
            isCompleted = isPartially || isConfirmed || isInProgress || isCompleted,
            isCurrent = isSearching
        ),
        StepperStep(
            title = "Crew Confirmed",
            subtitle = if (workersAccepted > 0) "$workersAccepted of $workersNeeded workers confirmed" else "Awaiting confirmations",
            isCompleted = isConfirmed || isInProgress || isCompleted,
            isCurrent = isPartially
        ),
        StepperStep(
            title = "Shift in Progress",
            subtitle = if (isInProgress) "Crew is active on-site" else "Awaiting check-in",
            isCompleted = isCompleted,
            isCurrent = isInProgress
        ),
        StepperStep(
            title = "Completed",
            subtitle = if (isCompleted) "Shift completed & settled" else "Pending completion",
            isCompleted = isCompleted,
            isCurrent = false
        )
    )

    LiveOrderTimeline(steps = steps, modifier = modifier)
}
