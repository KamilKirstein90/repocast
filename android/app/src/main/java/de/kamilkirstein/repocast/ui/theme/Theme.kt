package de.kamilkirstein.repocast.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The three looks the user can pick in the settings. */
enum class AppStyle(val label: String, val tagline: String, val dark: Boolean) {
    NACHTRADIO("Nachtradio", "Tinte & Bernstein", dark = true),
    LEINEN("Leinen", "Salbei & Tinte", dark = false),
    DAEMMERUNG("Dämmerung", "Pflaume & Koralle", dark = true),
}

/** A cover tile color with a readable text color on top. */
data class CoverColor(val background: Color, val content: Color)

/** Style details beyond the Material color scheme. */
data class StyleExtras(val covers: List<CoverColor>, val heroGradient: List<Color>)

val LocalAppStyle = staticCompositionLocalOf { AppStyle.NACHTRADIO }
val LocalStyleExtras = staticCompositionLocalOf { extras(AppStyle.NACHTRADIO) }

private val nachtradio = darkColorScheme(
    primary = Color(0xFFF2B544),
    onPrimary = Color(0xFF0E131C),
    primaryContainer = Color(0xFF3A2E12),
    onPrimaryContainer = Color(0xFFFFE2A8),
    secondary = Color(0xFF6FA3D8),
    onSecondary = Color(0xFF0E131C),
    tertiaryContainer = Color(0xFF243048),
    onTertiaryContainer = Color(0xFFE8ECF2),
    background = Color(0xFF0E131C),
    onBackground = Color(0xFFE8ECF2),
    surface = Color(0xFF0E131C),
    onSurface = Color(0xFFE8ECF2),
    surfaceVariant = Color(0xFF1A2130),
    onSurfaceVariant = Color(0xFF9AA4B5),
    surfaceContainerLowest = Color(0xFF0A0E15),
    surfaceContainerLow = Color(0xFF141A25),
    surfaceContainer = Color(0xFF1A2130),
    surfaceContainerHigh = Color(0xFF1D2738),
    surfaceContainerHighest = Color(0xFF243048),
    outline = Color(0xFF4A5468),
    outlineVariant = Color(0xFF2A3447),
    error = Color(0xFFE9776B),
    onError = Color(0xFF0E131C),
    errorContainer = Color(0xFF4A1F1B),
    onErrorContainer = Color(0xFFFFDAD5),
)

private val leinen = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE3D6),
    onPrimaryContainer = Color(0xFF0F2A1D),
    secondary = Color(0xFF3B5BA9),
    onSecondary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE6F2),
    onTertiaryContainer = Color(0xFF18221D),
    background = Color(0xFFF3F4F0),
    onBackground = Color(0xFF18221D),
    surface = Color(0xFFF3F4F0),
    onSurface = Color(0xFF18221D),
    surfaceVariant = Color(0xFFE3E8E0),
    onSurfaceVariant = Color(0xFF56635C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9F6),
    surfaceContainer = Color(0xFFEBEEE8),
    surfaceContainerHigh = Color(0xFFE3E8E0),
    surfaceContainerHighest = Color(0xFFDCE2D9),
    inverseSurface = Color(0xFF18221D),
    inverseOnSurface = Color(0xFFF3F4F0),
    outline = Color(0xFF8A968F),
    outlineVariant = Color(0xFFD5DBD3),
    error = Color(0xFFB3402A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE9E4),
    onErrorContainer = Color(0xFF7A2E1E),
)

private val daemmerung = darkColorScheme(
    primary = Color(0xFFFF8C73),
    onPrimary = Color(0xFF1B1726),
    primaryContainer = Color(0xFF3A2B4F),
    onPrimaryContainer = Color(0xFFFFD9CF),
    secondary = Color(0xFFB9A8F0),
    onSecondary = Color(0xFF1B1726),
    tertiaryContainer = Color(0xFF3A2B4F),
    onTertiaryContainer = Color(0xFFF1ECF7),
    background = Color(0xFF1B1726),
    onBackground = Color(0xFFF1ECF7),
    surface = Color(0xFF1B1726),
    onSurface = Color(0xFFF1ECF7),
    surfaceVariant = Color(0xFF262036),
    onSurfaceVariant = Color(0xFFB3A9C6),
    surfaceContainerLowest = Color(0xFF15111F),
    surfaceContainerLow = Color(0xFF211B2E),
    surfaceContainer = Color(0xFF262036),
    surfaceContainerHigh = Color(0xFF2F2742),
    surfaceContainerHighest = Color(0xFF3A3150),
    outline = Color(0xFF6B5A8A),
    outlineVariant = Color(0xFF4A3F63),
    error = Color(0xFFFF8C9A),
    onError = Color(0xFF1B1726),
    errorContainer = Color(0xFF4A1F2B),
    onErrorContainer = Color(0xFFFFD9DE),
)

fun colorScheme(style: AppStyle): ColorScheme = when (style) {
    AppStyle.NACHTRADIO -> nachtradio
    AppStyle.LEINEN -> leinen
    AppStyle.DAEMMERUNG -> daemmerung
}

fun extras(style: AppStyle): StyleExtras = when (style) {
    AppStyle.NACHTRADIO -> StyleExtras(
        covers = listOf(
            CoverColor(Color(0xFFF2B544), Color(0xFF0E131C)),
            CoverColor(Color(0xFF6FA3D8), Color(0xFF0E131C)),
            CoverColor(Color(0xFF7CC4A4), Color(0xFF0E131C)),
            CoverColor(Color(0xFFE9776B), Color(0xFF0E131C)),
        ),
        heroGradient = listOf(Color(0xFF1D2738), Color(0xFF151C29)),
    )
    AppStyle.LEINEN -> StyleExtras(
        covers = listOf(
            CoverColor(Color(0xFF2F6B4F), Color(0xFFF3F4F0)),
            CoverColor(Color(0xFFC9D7E8), Color(0xFF18221D)),
            CoverColor(Color(0xFFE4C79A), Color(0xFF18221D)),
            CoverColor(Color(0xFF3B5BA9), Color(0xFFFFFFFF)),
        ),
        heroGradient = listOf(Color(0xFF18221D), Color(0xFF18221D)),
    )
    AppStyle.DAEMMERUNG -> StyleExtras(
        covers = listOf(
            CoverColor(Color(0xFFFF8C73), Color(0xFF1B1726)),
            CoverColor(Color(0xFFB9A8F0), Color(0xFF1B1726)),
            CoverColor(Color(0xFFD4A0D0), Color(0xFF1B1726)),
            CoverColor(Color(0xFF7FD1C1), Color(0xFF1B1726)),
        ),
        heroGradient = listOf(Color(0xFF3A2B4F), Color(0xFF2A2238)),
    )
}

@Composable
fun RepoCastTheme(style: AppStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppStyle provides style, LocalStyleExtras provides extras(style)) {
        MaterialTheme(colorScheme = colorScheme(style), content = content)
    }
}
