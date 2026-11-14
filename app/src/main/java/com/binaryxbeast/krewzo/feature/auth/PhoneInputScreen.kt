package com.binaryxbeast.krewzo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun PhoneInputScreen(
    viewModel: AuthViewModel,
    onOtpSent: (String) -> Unit
) {
    val phone by viewModel.phoneState.collectAsState()
    val sendResult by viewModel.otpSendResult.collectAsState()
    val isLoading = sendResult is Resource.Loading
    val errorMessage = (sendResult as? Resource.Error)?.message
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    Scaffold(
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
            Spacer(modifier = Modifier.height(48.dp))

            StaggeredAnimatedItem(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Forward-moving geometric 'K' Brand Symbol
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(KrewzoDimens.RadiusXl),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(KrewzoDimens.RadiusXl))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFF7582FF)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Krewzo",
                        style = MaterialTheme.typography.headlineLarge,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "On-demand blue-collar crew booking in Pune",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            StaggeredAnimatedItem(index = 1) {
                KrewzoCard(
                    padding = 20.dp
                ) {
                    Text(
                        text = "Sign in or Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter your mobile number to receive a 6-digit verification code",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Chip
                        Box(
                            modifier = Modifier
                                .height(KrewzoDimens.InputHeight)
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .background(KrewzoTheme.colors.surfaceSecondary)
                                .border(KrewzoDimens.BorderWidth, KrewzoTheme.colors.border, RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🇮🇳 +91",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = KrewzoTheme.colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        KrewzoTextField(
                            value = phone,
                            onValueChange = { viewModel.updatePhone(it) },
                            placeholder = "98765 43210",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = KrewzoTheme.colors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            isError = errorMessage != null,
                            errorMessage = errorMessage,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    KrewzoButton(
                        text = "Get Verification Code",
                        onClick = {
                            viewModel.sendOtp(activity = activity, onSuccess = onOtpSent)
                        },
                        isLoading = isLoading,
                        enabled = phone.length == 10
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StaggeredAnimatedItem(index = 2) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Secure verification",
                            tint = KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Secure OTP Verification",
                            style = MaterialTheme.typography.labelMedium,
                            color = KrewzoTheme.colors.textTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "By continuing, you agree to Krewzo's Terms of Service and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textTertiary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
