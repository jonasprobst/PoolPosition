package ch.poolposition.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF0B6E99)
private val BrandDark = Color(0xFF57C5F0)

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = Color(0xFF4A6572),
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    secondary = Color(0xFF9FB4C0),
)

@Composable
fun PoolPositionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
