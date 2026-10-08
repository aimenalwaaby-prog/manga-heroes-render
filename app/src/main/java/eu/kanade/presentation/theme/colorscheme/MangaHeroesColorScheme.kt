package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Default Manga Heroes palette: vivid blue, violet, pink and warm amber with accessible text colors. */
internal object MangaHeroesColorScheme : BaseColorScheme() {
    override val lightScheme = lightColorScheme(
        primary = Color(0xFF4C4DFF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE4E3FF),
        onPrimaryContainer = Color(0xFF17156A),
        secondary = Color(0xFFE94F9B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFD9E9),
        onSecondaryContainer = Color(0xFF6B123F),
        tertiary = Color(0xFFFFA62B),
        onTertiary = Color(0xFF3B2100),
        tertiaryContainer = Color(0xFFFFDDB0),
        onTertiaryContainer = Color(0xFF2D1800),
        background = Color(0xFFFCF9FF),
        onBackground = Color(0xFF1C1A22),
        surface = Color(0xFFFCF9FF),
        onSurface = Color(0xFF1C1A22),
        surfaceVariant = Color(0xFFEAE5F0),
        onSurfaceVariant = Color(0xFF4A4650),
        outline = Color(0xFF7B747F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainer = Color(0xFFF1ECF5),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0EA),
    )

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFB9B7FF),
        onPrimary = Color(0xFF26256F),
        primaryContainer = Color(0xFF3D3E98),
        onPrimaryContainer = Color(0xFFE4E3FF),
        secondary = Color(0xFFFFA9CF),
        onSecondary = Color(0xFF68143E),
        secondaryContainer = Color(0xFF8D245A),
        onSecondaryContainer = Color(0xFFFFD9E9),
        tertiary = Color(0xFFFFBA5A),
        onTertiary = Color(0xFF492900),
        tertiaryContainer = Color(0xFF704A12),
        onTertiaryContainer = Color(0xFFFFDDB0),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE9E0EA),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE9E0EA),
        surfaceVariant = Color(0xFF48434C),
        onSurfaceVariant = Color(0xFFCBC2CC),
        outline = Color(0xFF958E98),
        surfaceContainerLowest = Color(0xFF0F0D12),
        surfaceContainerLow = Color(0xFF1C191F),
        surfaceContainer = Color(0xFF211E24),
        surfaceContainerHigh = Color(0xFF2B272F),
        surfaceContainerHighest = Color(0xFF353038),
    )
}
