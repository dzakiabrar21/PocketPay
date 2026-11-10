package com.example.pocketpay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.ui.theme.PocketTheme

/** Page background: calm tint with a soft lime glow in the top corner, like the reference. */
@Composable
fun PocketBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glow = PocketTheme.colors.lime.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .fillMaxSize()
            // Swallow touches so nothing underneath a full-screen overlay can be tapped through it.
            .pointerInput(Unit) {}
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = Offset(900f, 0f),
                    radius = 900f
                )
            ),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground
            ) { content() }
        }
    )
}

enum class PillStyle { LIME, DARK, SOFT_ON_HERO, OUTLINE }

/** Fully rounded action button. [LIME] is the primary call to action. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillStyle = PillStyle.LIME,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val c = PocketTheme.colors
    val container = when (style) {
        PillStyle.LIME -> c.lime
        PillStyle.DARK -> c.hero
        PillStyle.SOFT_ON_HERO -> c.heroChip
        PillStyle.OUTLINE -> Color.Transparent
    }
    val content = when (style) {
        PillStyle.LIME -> c.onLime
        PillStyle.DARK, PillStyle.SOFT_ON_HERO -> c.onHero
        PillStyle.OUTLINE -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) container else container.copy(alpha = 0.4f),
        contentColor = content,
        border = if (style == PillStyle.OUTLINE) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier.height(height)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Round icon button used in headers and on the hero card. */
@Composable
fun CircleIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    container: Color = PocketTheme.colors.soft,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (actionText != null) {
            Text(
                actionText,
                style = MaterialTheme.typography.labelLarge,
                color = PocketTheme.colors.accentText,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(PocketTheme.colors.lime, PocketTheme.colors.accent))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial.take(1).uppercase(),
            color = PocketTheme.colors.onLime,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
fun StatusChip(text: String, container: Color, content: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

data class NavItem(val label: String, val icon: ImageVector)

/** Floating dark pill navigation, as in the reference. The selected tab is highlighted in lime. */
@Composable
fun FloatingBottomBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = PocketTheme.colors
    Surface(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(34.dp),
        color = c.hero,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint = if (selected) c.lime else c.onHeroMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(role = Role.Tab) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Row of dots showing how many PIN digits were entered. */
@Composable
fun PinDots(length: Int, filled: Int, modifier: Modifier = Modifier, isError: Boolean = false, onDark: Boolean = false) {
    val c = PocketTheme.colors
    val active = if (isError) c.danger else if (onDark) c.lime else MaterialTheme.colorScheme.onSurface
    val idle = if (onDark) c.onHero.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(length) { i ->
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) active else Color.Transparent)
                    .then(
                        if (i < filled) Modifier else Modifier.background(Color.Transparent)
                    )
                    .padding(1.dp)
                    .background(if (i < filled) active else idle, CircleShape)
            )
        }
    }
}

/** Numeric keypad for PIN entry. [leading] fills the bottom-left cell (e.g. fingerprint). */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    val c = PocketTheme.colors
    val keyBg = if (onDark) c.heroChip else MaterialTheme.colorScheme.surface
    val keyFg = if (onDark) c.onHero else MaterialTheme.colorScheme.onSurface
    @Composable
    fun Key(content: @Composable () -> Unit, onClick: (() -> Unit)?) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.background(keyBg).clickable(role = Role.Button, onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) { content() }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d ->
                    Key({ Text(d.toString(), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = keyFg) }) { onDigit(d) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Key({ if (leading != null) leading() }, null)
            Key({ Text("0", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = keyFg) }) { onDigit('0') }
            Key(
                { Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "⌫", tint = keyFg, modifier = Modifier.size(26.dp)) },
                onDelete
            )
        }
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            cap = StrokeCap.Round
        )
    }
}

/** Animated lime check inside a soft halo: shown on payment success. */
@Composable
fun SuccessBadge(modifier: Modifier = Modifier, size: Dp = 104.dp) {
    val c = PocketTheme.colors
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Box(
        modifier = modifier.size(size).scale(scale.value).clip(CircleShape).background(c.lime.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(size * 0.68f).clip(CircleShape).background(c.lime), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onLime, modifier = Modifier.size(size * 0.38f))
        }
    }
}

/**
 * Simple rounded bar chart. [values] are relative heights; the bar at [highlight] is lime, the rest
 * a muted tone. Labels sit under each bar.
 */
@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    chartHeight: Dp = 170.dp
) {
    val c = PocketTheme.colors
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        values.forEachIndexed { i, v ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .clip(RoundedCornerShape(50))
                        .background(c.soft),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fraction = (v.toFloat() / max).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.08f))
                                .clip(RoundedCornerShape(50))
                                .background(if (highlight == null || highlight == i) c.lime else c.lime.copy(alpha = 0.55f))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Card with the standard 24dp radius and a hairline border instead of heavy shadows. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val base = Modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.then(base),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, PocketTheme.colors.border)
        ) { Box(Modifier.padding(contentPadding)) { content() } }
    } else {
        Surface(
            modifier = modifier.then(base),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, PocketTheme.colors.border)
        ) { Box(Modifier.padding(contentPadding)) { content() } }
    }
}

/** Rounded text field with a lime focus ring, used by every form in the app. */
@Composable
fun PocketTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    isError: Boolean = false,
    prefix: String? = null,
    singleLine: Boolean = true
) {
    val c = PocketTheme.colors
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = keyboardOptions,
        prefix = prefix?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, null) } },
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = c.lime,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = c.accentText,
            cursorColor = c.accent
        )
    )
}

/** Filter chip that turns lime when selected. */
@Composable
fun PocketChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = PocketTheme.colors
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) c.lime else MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) c.onLime else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** Hides a covered screen from accessibility services (TalkBack) while something is drawn over it. */
fun Modifier.hideIf(covered: Boolean): Modifier = if (covered) this.clearAndSetSemantics { } else this
