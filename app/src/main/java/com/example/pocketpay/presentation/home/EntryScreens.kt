package com.example.pocketpay.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.theme.PocketTheme
import kotlinx.coroutines.delay

/** Dark splash with the lime brand mark; fades in, holds briefly, then hands over. */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit, modifier: Modifier = Modifier, holdMillis: Long = 1600L) {
    val c = PocketTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(700))
        delay(holdMillis)
        onSplashFinished()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
            .background(Brush.verticalGradient(listOf(c.hero, c.heroAlt)))
            .drawBehind {
                drawCircle(c.lime.copy(alpha = 0.10f), radius = 220.dp.toPx(), center = Offset(size.width, 0f))
                drawCircle(c.lime.copy(alpha = 0.07f), radius = 160.dp.toPx(), center = Offset(0f, size.height))
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.alpha(progress.value).scale(0.9f + 0.1f * progress.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(c.lime),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = c.onLime, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("PocketPay", color = c.onHero, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lock_next_gen_digital_banking_micro_savin), color = c.onHeroMuted, style = MaterialTheme.typography.bodyMedium)
        }
        CircularProgressIndicator(
            color = c.lime, trackColor = c.heroChip, strokeWidth = 3.dp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).size(28.dp).alpha(progress.value)
        )
    }
}

/** App lock: PIN keypad with an optional fingerprint shortcut. */
@Composable
fun LockScreen(
    biometricAvailable: Boolean,
    onBiometricRequest: () -> Unit,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { if (biometricAvailable) onBiometricRequest() }

    PinEntryScreen(
        title = stringResource(R.string.pin_enter_title),
        subtitle = stringResource(R.string.pin_lock_subtitle),
        modifier = modifier,
        error = error,
        resetKey = attempt,
        leading = if (biometricAvailable) {
            { CircleIcon(Icons.Rounded.Fingerprint, stringResource(R.string.pin_use_biometric), container = PocketTheme.colors.heroChip, tint = PocketTheme.colors.lime, size = 72.dp, onClick = onBiometricRequest) }
        } else null,
        onComplete = { pin ->
            val result = PinManager.verify(context, pin)
            if (result == PinResult.Success) onUnlocked()
            else { error = PinManager.describe(context, result); attempt++ }
        }
    )
}

/** First-launch PIN creation: choose, then repeat. */
@Composable
fun PinSetupScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    PinEntryScreen(
        title = stringResource(if (first == null) R.string.pin_create_title else R.string.pin_repeat_title),
        subtitle = stringResource(if (first == null) R.string.pin_create_subtitle else R.string.pin_repeat_subtitle),
        modifier = modifier,
        error = error,
        resetKey = attempt,
        onBack = if (first != null) ({ first = null; error = null; attempt++ }) else null,
        onComplete = { pin ->
            val firstPin = first
            if (firstPin == null) {
                val problem = PinManager.validateNewPin(context, pin)
                if (problem != null) { error = problem; attempt++ } else { first = pin; error = null; attempt++ }
            } else if (pin != firstPin) {
                error = context.getString(R.string.pin_konfirmasi_pin_tidak_cocok)
                first = null; attempt++
            } else {
                val failure = PinManager.setInitialPin(context, pin)
                if (failure == null) onDone() else { error = failure; first = null; attempt++ }
            }
        }
    )
}

/** Change PIN: current PIN, new PIN, repeat. */
@Composable
fun ChangePinScreen(onBack: () -> Unit, onChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) } // 0 old, 1 new, 2 repeat
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    PinEntryScreen(
        title = stringResource(when (step) { 0 -> R.string.pin_old_title; 1 -> R.string.pin_new_title; else -> R.string.pin_repeat_title }),
        subtitle = stringResource(if (step == 2) R.string.pin_repeat_subtitle else R.string.pin_create_subtitle),
        modifier = modifier,
        error = error,
        resetKey = attempt,
        onBack = onBack,
        onComplete = { pin ->
            error = null
            when (step) {
                0 -> {
                    val r = PinManager.verify(context, pin)
                    if (r == PinResult.Success) { oldPin = pin; step = 1 } else error = PinManager.describe(context, r)
                }
                1 -> {
                    val problem = PinManager.validateNewPin(context, pin)
                    if (problem != null) error = problem
                    else if (pin == oldPin) error = context.getString(R.string.pinmgr_err_same_as_old)
                    else { newPin = pin; step = 2 }
                }
                else -> {
                    if (pin != newPin) { error = context.getString(R.string.pin_konfirmasi_pin_baru_tidak_cocok); step = 1 }
                    else {
                        val failure = PinManager.changePin(context, oldPin, pin)
                        if (failure == null) onChanged() else { error = failure; step = 0 }
                    }
                }
            }
            attempt++
        }
    )
}
