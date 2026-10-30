package de.kamilkirstein.repocast.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kamilkirstein.repocast.ui.theme.AppStyle
import de.kamilkirstein.repocast.ui.theme.Logo

private fun tagline(style: AppStyle) = when (style) {
    AppStyle.NACHTRADIO -> "Code zum Zuhören"
    AppStyle.LEINEN -> null
    AppStyle.DAEMMERUNG -> "Jeder Commit eine Geschichte"
}

/** The animated intro shown once per app start. */
@Composable
fun SplashScreen(style: AppStyle, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1400, easing = LinearEasing))
        kotlinx.coroutines.delay(350)
        onFinished()
    }
    val p = progress.value
    // The wordmark fades and rises in during the second half of the logo animation.
    val word = ((p - 0.55f) / 0.35f).coerceIn(0f, 1f)
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
        if (style != AppStyle.LEINEN) {
            val glow = colors.primary
            Box(
                Modifier.size(340.dp)
                    .graphicsLayer { alpha = (p * 1.6f).coerceAtMost(1f); scaleX = 0.6f + 0.4f * p; scaleY = scaleX }
                    .background(Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), glow.copy(alpha = 0f)))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Logo(style, Modifier.size(140.dp), progress = p)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { alpha = word; translationY = 24f * (1f - word) },
            ) {
                Text("RepoCast", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                tagline(style)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            }
        }
    }
}
