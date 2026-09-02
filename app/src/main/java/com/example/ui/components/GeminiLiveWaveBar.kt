package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.JarvisUiState
import com.example.ai.JarvisVoiceStatus

/**
 * Sleek, non-intrusive floating pill that sits right above the bottom navigation bar.
 * Gives user live feedback of what Gemini Live is hearing/speaking without obstructing the web view.
 */
@Composable
fun GeminiLiveFloatingBar(
    uiState: JarvisUiState,
    onToggleLang: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0C0E14).copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF61AFEF).copy(alpha = 0.6f),
                    Color(0xFFC678DD).copy(alpha = 0.6f),
                    Color(0xFFE5C07B).copy(alpha = 0.6f)
                )
            )
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Animated Dancing Wave Equalizer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                val isSpeaking = uiState.isTtsSpeaking || uiState.status == JarvisVoiceStatus.SPEAKING
                val isListening = uiState.status == JarvisVoiceStatus.LISTENING
                val sound = uiState.soundLevel

                LiveDancingBar(heightFactor = if (isSpeaking) 0.9f else if (isListening) sound else 0.25f, delay = 0, color = Color(0xFF61AFEF))
                LiveDancingBar(heightFactor = if (isSpeaking) 0.7f else if (isListening) (sound * 1.2f).coerceAtMost(1f) else 0.35f, delay = 150, color = Color(0xFFC678DD))
                LiveDancingBar(heightFactor = if (isSpeaking) 1.0f else if (isListening) (sound * 0.8f).coerceAtMost(1f) else 0.2f, delay = 300, color = Color(0xFFE5C07B))
                LiveDancingBar(heightFactor = if (isSpeaking) 0.6f else if (isListening) (sound * 1.1f).coerceAtMost(1f) else 0.4f, delay = 450, color = Color(0xFF98C379))
            }

            // Center: Live Voice Text / Status
            val displayText = when {
                uiState.isTtsSpeaking && uiState.aiResponseText.isNotBlank() -> uiState.aiResponseText
                uiState.recognizedSpeech.isNotBlank() -> "\"${uiState.recognizedSpeech}\""
                uiState.status == JarvisVoiceStatus.PROCESSING -> if (uiState.preferredLanguage.startsWith("bn")) "বিশ্লেষণ করা হচ্ছে..." else "Thinking..."
                uiState.status == JarvisVoiceStatus.LISTENING -> if (uiState.preferredLanguage.startsWith("bn")) "শুনছি, যেকোনো কিছু বলুন..." else "Listening live..."
                uiState.status == JarvisVoiceStatus.EXECUTING -> uiState.activeActionLabel.ifBlank { "Executing..." }
                else -> if (uiState.preferredLanguage.startsWith("bn")) "শুনছি..." else "Listening..."
            }

            Text(
                text = displayText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            // Right: Language Switch Pill + Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E222B),
                    modifier = Modifier.clickable { onToggleLang() }
                ) {
                    Text(
                        text = if (uiState.preferredLanguage.startsWith("bn")) "বাংলা 🇧🇩" else "EN 🇺🇸",
                        color = Color(0xFFE5C07B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Live Mode",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveDancingBar(
    heightFactor: Float,
    delay: Int,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "dancing_bar_$delay")
    val animatedScale by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400 + delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_scale_$delay"
    )

    val currentHeight = (18.dp * ((animatedScale * 0.4f) + (heightFactor * 0.6f)).coerceIn(0.2f, 1f))

    Box(
        modifier = Modifier
            .width(3.dp)
            .height(currentHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

/**
 * Concentric glowing wave rings around the central AI button in bottom navigation bar.
 * Activated smoothly during Gemini Live mode.
 */
@Composable
fun GeminiLiveAiButton(
    isLiveMode: Boolean,
    isHomeVisible: Boolean,
    soundLevel: Float,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ai_button_aura")
    val pulseScale1 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale1"
    )
    val pulseScale2 by transition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale2"
    )

    val auraScale = if (isLiveMode) {
        (pulseScale1 + (soundLevel * 0.35f)).coerceIn(1f, 1.8f)
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLiveMode) {
            // Outer Pulsating Wave 2
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(pulseScale2)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFC678DD).copy(alpha = 0.25f),
                                Color(0xFF61AFEF).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner Pulsating Wave 1
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(auraScale)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF61AFEF).copy(alpha = 0.45f),
                                Color(0xFFC678DD).copy(alpha = 0.45f),
                                Color(0xFFE5C07B).copy(alpha = 0.45f),
                                Color(0xFF98C379).copy(alpha = 0.45f),
                                Color(0xFF61AFEF).copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }

        // Center AI Core Button
        @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isLiveMode) Color(0xFF0F1218)
                    else if (isHomeVisible) Color(0xFFF7F6F2)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isLiveMode) 1.8.dp else 1.dp,
                    brush = if (isLiveMode) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF61AFEF),
                                Color(0xFFC678DD),
                                Color(0xFFE5C07B),
                                Color(0xFF98C379),
                                Color(0xFF61AFEF)
                            )
                        )
                    } else {
                        SolidColor(
                            if (isHomeVisible) Color(0xFFD6D1C6)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    },
                    shape = CircleShape
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.img_auren_logo_1788383105052),
                contentDescription = "AUREN AI",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
