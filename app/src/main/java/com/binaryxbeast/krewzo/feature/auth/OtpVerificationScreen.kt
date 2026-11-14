package com.binaryxbeast.krewzo.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.UserSession
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.components.rememberShakeController
import com.binaryxbeast.krewzo.ui.components.shakeEffect
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun OtpVerificationScreen(
    phone: String,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onRouteResolved: (UserSession) -> Unit
) {
    val otp by viewModel.otpState.collectAsState()
    val authResult by viewModel.authResult.collectAsState()
    val isLoading = authResult is Resource.Loading
    val errorMessage = (authResult as? Resource.Error)?.message
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val shakeController = rememberShakeController()

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeController.shake()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Blinking cursor transition for focused digit slot
    val cursorTransition = rememberInfiniteTransition(label = "otpCursorTransition")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "otpCursorBlink"
    )

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Verification",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            StaggeredAnimatedItem(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Security Brand Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Verification",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Enter 6-digit Code",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sent via SMS to +91 $phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KrewzoTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.bouncyClickable { onBack() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            StaggeredAnimatedItem(index = 1) {
                KrewzoCard(padding = 18.dp) {
                    // 6-digit Interactive Responsive PIN Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable {
                                focusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Invisible BasicTextField capturing real user typing
                        BasicTextField(
                            value = otp,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(6)
                                if (digits != otp) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.updateOtp(digits)
                                if (digits.length == 6) {
                                    focusManager.clearFocus()
                                    viewModel.verifyOtp(phone = phone, onSuccess = onRouteResolved)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (otp.length == 6) {
                                        viewModel.verifyOtp(phone = phone, onSuccess = onRouteResolved)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .size(1.dp)
                        )

                        // 6 Visual Individual Digit Boxes - responsive weight(1f) to ensure all 6 always fit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shakeEffect(shakeController.shakeTrigger),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 6) {
                                val char = otp.getOrNull(i)
                                val isFocused = otp.length == i || (i == 5 && otp.length == 6)

                                val boxBorderColor by animateColorAsState(
                                    targetValue = when {
                                        errorMessage != null -> MaterialTheme.colorScheme.error
                                        isFocused -> MaterialTheme.colorScheme.primary
                                        char != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else -> KrewzoTheme.colors.border
                                    },
                                    animationSpec = tween(150),
                                    label = "otpBoxBorder"
                                )

                                val boxBgColor by animateColorAsState(
                                    targetValue = when {
                                        errorMessage != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        char != null -> MaterialTheme.colorScheme.surface
                                        else -> KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.5f)
                                    },
                                    animationSpec = tween(150),
                                    label = "otpBoxBg"
                                )

                                val charScale by animateFloatAsState(
                                    targetValue = if (char != null) 1f else 0.5f,
                                    animationSpec = spring(
                                        dampingRatio = 0.55f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "otpDigitScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                                        .background(boxBgColor)
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = boxBorderColor,
                                            shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (char != null) {
                                        Text(
                                            text = char.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = KrewzoTheme.colors.textPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = charScale
                                                scaleY = charScale
                                            }
                                        )
                                    } else if (isFocused) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(24.dp)
                                                .graphicsLayer { alpha = cursorAlpha }
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    KrewzoButton(
                        text = "Verify & Continue",
                        onClick = {
                            viewModel.verifyOtp(phone = phone, onSuccess = onRouteResolved)
                        },
                        isLoading = isLoading,
                        enabled = otp.length == 6
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Didn't receive the SMS? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textTertiary
                        )
                        Text(
                            text = "Resend Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.bouncyClickable {
                                viewModel.sendOtp(activity = activity) {}
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
