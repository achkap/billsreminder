package gr.logariasmoi.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.logariasmoi.data.Bill
import gr.logariasmoi.data.Category
import gr.logariasmoi.data.Providers

val OverdueRed = Color(0xFFD32F2F)
val PaidGreen = Color(0xFF2E7D32)

@Composable
fun isAppInDarkTheme(themeMode: String): Boolean = when (themeMode) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun AppTheme(themeMode: String, pureBlack: Boolean, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(themeMode)
    val context = LocalContext.current
    var scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF9ECAFF), secondary = Color(0xFFFFB74D))
        else -> lightColorScheme(primary = Color(0xFF1565C0), secondary = Color(0xFFF57C00))
    }
    if (dark && pureBlack) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0B0B0B),
            surfaceContainer = Color(0xFF111111),
            surfaceContainerHigh = Color(0xFF191919),
            surfaceContainerHighest = Color(0xFF222222),
            surfaceBright = Color(0xFF262626),
            surfaceVariant = Color(0xFF1C1C1C),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun Category.icon(): ImageVector = when (this) {
    Category.ELECTRICITY -> Icons.Outlined.Bolt
    Category.GAS -> Icons.Outlined.LocalFireDepartment
    Category.WATER -> Icons.Outlined.WaterDrop
    Category.TELECOM -> Icons.Outlined.Wifi
    Category.BANK -> Icons.Outlined.AccountBalance
    Category.INSURANCE -> Icons.Outlined.Verified
    Category.PUBLIC -> Icons.Outlined.Policy
    Category.SUBSCRIPTION -> Icons.Outlined.Subscriptions
    Category.HOME -> Icons.Outlined.Home
    Category.OTHER -> Icons.Outlined.Category
}

fun Category.color(): Color = when (this) {
    Category.ELECTRICITY -> Color(0xFFF9A825)
    Category.GAS -> Color(0xFFEF6C00)
    Category.WATER -> Color(0xFF0288D1)
    Category.TELECOM -> Color(0xFF7B1FA2)
    Category.BANK -> Color(0xFF1565C0)
    Category.INSURANCE -> Color(0xFF00897B)
    Category.PUBLIC -> Color(0xFF455A64)
    Category.SUBSCRIPTION -> Color(0xFFD81B60)
    Category.HOME -> Color(0xFF6D4C41)
    Category.OTHER -> Color(0xFF757575)
}

/** Up to two characters that stand in for a company: initials of the first two words, or the first two letters. */
fun badgeText(name: String): String {
    val words = name.split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2)
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** A coloured tile with the company initials (no brand logos are bundled) or the category icon. */
@Composable
fun ProviderIcon(providerId: String?, name: String, category: Category, size: Dp = 44.dp) {
    val provider = Providers.get(providerId)
    val shape = RoundedCornerShape(size * 0.28f)
    val bg = provider?.let { Color(it.color) } ?: category.color()
    val fg = if (bg.luminance() > 0.55f) Color(0xFF1B1B1F) else Color.White
    val generic = provider?.category == Category.HOME || provider?.category == Category.OTHER
    val badge = when {
        provider != null && !generic -> badgeText(provider.name)
        provider == null && category == Category.OTHER -> badgeText(name)
        else -> ""
    }
    Box(
        Modifier.size(size).clip(shape).background(bg).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (badge.isNotEmpty()) {
            Text(badge, color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp, maxLines = 1)
        } else {
            Icon(category.icon(), contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
fun BillIcon(bill: Bill, size: Dp = 44.dp) = ProviderIcon(bill.providerId, bill.name, bill.category, size)
