package com.jackson4rocks.cipherlauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jackson4rocks.cipherlauncher.data.LauncherRepository
import com.jackson4rocks.cipherlauncher.data.LaunchableApp
import com.jackson4rocks.cipherlauncher.data.SettingsStore
import com.jackson4rocks.cipherlauncher.security.SecurityStore
import com.jackson4rocks.cipherlauncher.sensors.StepCounter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppMode {
    ENTRY,
    WATCH,
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var security: SecurityStore
    private lateinit var settings: SettingsStore
    private lateinit var stepCounter: StepCounter
    private lateinit var launcherRepository: LauncherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        security = SecurityStore(this)
        settings = SettingsStore(this)
        stepCounter = StepCounter(this)
        launcherRepository = LauncherRepository(this)

        configureWindow(window)

        setContent {
            var amoled by remember { mutableStateOf(settings.amoled) }

            CipherTheme(amoled = amoled) {
                CipherLauncherApp(
                    security = security,
                    settings = settings,
                    launcherRepository = launcherRepository,
                    stepCounter = stepCounter,
                    battery = readBatteryPercent(),
                    amoled = amoled,
                    onAmoledChanged = {
                        amoled = it
                        settings.amoled = it
                    },
                    openHomeSettings = ::openHomeSettings
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        stepCounter.start()
    }

    override fun onStop() {
        stepCounter.stop()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) configureWindow(window)
    }

    private fun readBatteryPercent(): Int {
        val manager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .coerceIn(0, 100)
    }

    private fun openHomeSettings() {
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private companion object {
        fun configureWindow(window: Window) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

@Composable
private fun CipherLauncherApp(
    security: SecurityStore,
    settings: SettingsStore,
    launcherRepository: LauncherRepository,
    stepCounter: StepCounter,
    battery: Int,
    amoled: Boolean,
    onAmoledChanged: (Boolean) -> Unit,
    openHomeSettings: () -> Unit
) {
    val context = LocalContext.current
    var configured by remember { mutableStateOf(security.isConfigured()) }
    var mode by remember { mutableStateOf(AppMode.ENTRY) }
    var showSeconds by remember { mutableStateOf(settings.showSeconds) }
    var setupError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    when {
        !configured -> SetupScreen(
            error = setupError,
            onSetup = { watch, home ->
                when {
                    !security.validPin(watch) || !security.validPin(home) ->
                        setupError = "Use two different 4–12 digit PINs."

                    watch == home ->
                        setupError = "Watch and launcher PINs must be different."

                    else -> {
                        security.setup(watch, home)
                        configured = true
                        setupError = null
                    }
                }
            }
        )

        mode == AppMode.ENTRY -> EntryScreen(
            onUnlock = { pin ->
                when {
                    security.verifyWatchPin(pin) -> {
                        mode = AppMode.WATCH
                        true
                    }
                    security.verifyHomePin(pin) -> {
                        mode = AppMode.HOME
                        true
                    }
                    else -> false
                }
            }
        )

        mode == AppMode.WATCH -> WatchScreen(
            steps = stepCounter.steps,
            battery = battery,
            showSeconds = showSeconds,
            onTap = { mode = AppMode.ENTRY }
        )

        mode == AppMode.HOME -> HomeScreen(
            repository = launcherRepository,
            steps = stepCounter.steps,
            battery = battery,
            onSettings = { mode = AppMode.SETTINGS },
            onLock = { mode = AppMode.ENTRY }
        )

        mode == AppMode.SETTINGS -> SettingsScreen(
            security = security,
            amoled = amoled,
            showSeconds = showSeconds,
            onAmoledChanged = onAmoledChanged,
            onSecondsChanged = {
                showSeconds = it
                settings.showSeconds = it
            },
            openHomeSettings = openHomeSettings,
            onBack = { mode = AppMode.HOME },
            onLock = { mode = AppMode.ENTRY }
        )
    }
}

@Composable
private fun SetupScreen(
    error: String?,
    onSetup: (String, String) -> Unit
) {
    var watchPin by remember { mutableStateOf("") }
    var homePin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Watch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text("Cipher Launcher", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "Set up two PINs: one for Watch Mode and one for the full launcher.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = watchPin,
            onValueChange = { watchPin = it.filter(Char::isDigit).take(12) },
            label = { Text("Watch Mode PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = homePin,
            onValueChange = { homePin = it.filter(Char::isDigit).take(12) },
            label = { Text("Launcher PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSetup(watchPin, homePin) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Cipher")
        }
    }
}

@Composable
private fun EntryScreen(
    onUnlock: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var showPad by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        if (pin.isEmpty()) return

        val unlocked = onUnlock(pin)
        error = !unlocked
        if (unlocked) {
            pin = ""
            showPad = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(showPad) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount < -14f) {
                            showPad = true
                            error = false
                        } else if (dragAmount > 20f && showPad) {
                            showPad = false
                            error = false
                        }
                    }
                )
            }
    ) {
        if (!showPad) {
            EntryClock()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EntryClock(compact = true)

                Spacer(Modifier.height(8.dp))

                Text(
                    if (pin.isEmpty()) "Enter PIN" else "•".repeat(pin.length),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    letterSpacing = 4.sp
                )

                if (error) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Wrong PIN",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                NumberPad(
                    onDigit = { digit ->
                        if (pin.length < 12) {
                            pin += digit
                            error = false
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            error = false
                        }
                    },
                    onSubmit = ::submit
                )
            }
        }
    }
}

@Composable
private fun EntryClock(compact: Boolean = false) {
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val hour = SimpleDateFormat("HH", Locale.getDefault()).format(now)
    val minute = SimpleDateFormat("mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE, d MMMM", Locale.getDefault()).format(now)

    Column(
        modifier = if (compact) {
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        } else {
            Modifier.fillMaxSize()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            hour,
            fontSize = if (compact) 48.sp else 78.sp,
            lineHeight = if (compact) 46.sp else 70.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-3).sp
        )
        Text(
            minute,
            fontSize = if (compact) 48.sp else 78.sp,
            lineHeight = if (compact) 46.sp else 70.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-3).sp
        )
        Spacer(Modifier.height(if (compact) 2.dp else 12.dp))
        Text(
            date.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 10.sp else 12.sp,
            letterSpacing = 1.6.sp
        )
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "⌫", "0", "✓"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(
                                color = Color(0xFF151619),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "✓" -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            key,
                            fontSize = if (key.length == 1 && key[0].isDigit()) 22.sp else 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchScreen(
    steps: Long,
    battery: Int,
    showSeconds: Boolean,
    onTap: () -> Unit
) {
    var now by remember { mutableStateOf(Date()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(if (showSeconds) 1000L else 30_000L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onTap() }
                )
            }
    ) {
        val pageWidth = maxWidth

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(pageWidth)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        SimpleDateFormat("HH", Locale.getDefault()).format(now),
                        fontSize = 98.sp,
                        lineHeight = 88.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-4).sp
                    )
                    Text(
                        SimpleDateFormat("mm", Locale.getDefault()).format(now),
                        fontSize = 98.sp,
                        lineHeight = 88.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-4).sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(maxWidth)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        SimpleDateFormat("EEE", Locale.getDefault())
                            .format(now)
                            .uppercase(),
                        fontSize = 52.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        SimpleDateFormat("dd", Locale.getDefault()).format(now),
                        fontSize = 74.sp,
                        lineHeight = 68.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        SimpleDateFormat("MMMM", Locale.getDefault())
                            .format(now)
                            .uppercase(),
                        fontSize = 23.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        steps.toString(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "STEPS",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "BATTERY  $battery%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    repository: LauncherRepository,
    steps: Long,
    battery: Int,
    onSettings: () -> Unit,
    onLock: () -> Unit
) {
    val apps = remember { repository.apps() }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(time, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                Text(date.uppercase(), fontSize = 9.sp, letterSpacing = 1.2.sp)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Outlined.Lock, contentDescription = "Lock")
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("STEPS", fontSize = 9.sp, letterSpacing = 1.sp)
                    Text("$steps", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BATTERY", fontSize = 9.sp, letterSpacing = 1.sp)
                    Text("$battery%", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Text(
            "APPS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                AppTile(app = app, onClick = { repository.launch(app) })
            }
        }
    }
}

@Composable
private fun AppTile(
    app: LaunchableApp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsScreen(
    security: SecurityStore,
    amoled: Boolean,
    showSeconds: Boolean,
    onAmoledChanged: (Boolean) -> Unit,
    onSecondsChanged: (Boolean) -> Unit,
    openHomeSettings: () -> Unit,
    onBack: () -> Unit,
    onLock: () -> Unit
) {
    var changeTarget by remember { mutableStateOf<PinTarget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(14.dp))
        Text("APPEARANCE", fontSize = 10.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.primary)
        SettingSwitch(
            "AMOLED black",
            "Pure black surfaces for OLED watches.",
            amoled,
            onAmoledChanged
        )
        SettingSwitch(
            "Seconds",
            "Show seconds on the watch face.",
            showSeconds,
            onSecondsChanged
        )

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))

        Text("SECURITY", fontSize = 10.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.primary)

        SettingButton("Change Watch PIN") { changeTarget = PinTarget.WATCH }
        SettingButton("Change Launcher PIN") { changeTarget = PinTarget.HOME }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))

        Text("SYSTEM", fontSize = 10.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.primary)
        SettingButton("Set Cipher as default launcher", openHomeSettings)
        SettingButton("Lock now", onLock)

        Spacer(Modifier.height(18.dp))
        Text(
            "Cipher Launcher 0.1.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }

    changeTarget?.let { target ->
        PinChangeDialog(
            title = if (target == PinTarget.WATCH) "Change Watch PIN" else "Change Launcher PIN",
            onDismiss = { changeTarget = null },
            onChange = { current, next ->
                val ok = if (target == PinTarget.WATCH) {
                    security.changeWatchPin(current, next)
                } else {
                    security.changeHomePin(current, next)
                }
                if (ok) changeTarget = null
                ok
            }
        )
    }
}

private enum class PinTarget { WATCH, HOME }

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingButton(
    title: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PinChangeDialog(
    title: String,
    onDismiss: () -> Unit,
    onChange: (String, String) -> Boolean
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it.filter(Char::isDigit).take(12) },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = next,
                    onValueChange = { next = it.filter(Char::isDigit).take(12) },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (next.length !in 4..12) {
                        error = "Use 4–12 digits."
                    } else if (!onChange(current, next)) {
                        error = "Current PIN is incorrect."
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CipherTheme(
    amoled: Boolean,
    content: @Composable () -> Unit
) {
    val background = if (amoled) Color.Black else Color(0xFF050507)

    val scheme = darkColorScheme(
        primary = Color(0xFFB8F2E6),
        onPrimary = Color(0xFF00201C),
        background = background,
        surface = if (amoled) Color.Black else Color(0xFF0D0D10),
        surfaceVariant = Color(0xFF17181C),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5),
        onSurfaceVariant = Color(0xFFAAA6AA)
    )

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
