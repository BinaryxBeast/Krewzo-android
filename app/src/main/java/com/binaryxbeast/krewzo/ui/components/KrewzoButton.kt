package com.binaryxbeast.krewzo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

enum class KrewzoButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    DANGER
}

/**
 * Modern fluid button with spring press physics, smooth state transitions,
 * and elegant Material 3 styling.
 */
@Composable
fun KrewzoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KrewzoButtonVariant = KrewzoButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillMaxWidth: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 400f
        ),
        label = "btnScale"
    )

    val buttonModifier = (if (fillMaxWidth) {
        modifier.fillMaxWidth().height(KrewzoDimens.ButtonHeightLg)
    } else {
        modifier.height(KrewzoDimens.ButtonHeightLg)
    }).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    val shape = RoundedCornerShape(KrewzoDimens.RadiusXl)

    when (variant) {
        KrewzoButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.onPrimary)
            }
        }
        KrewzoButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.primary)
            }
        }
        KrewzoButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                border = BorderStroke(KrewzoDimens.BorderWidth, KrewzoTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.primary)
            }
        }
        KrewzoButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    contentColor: Color
) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "btnContentAnim"
    ) { loading ->
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                ExpressLoadingIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = contentColor
                )
            }
        }
    }
}
