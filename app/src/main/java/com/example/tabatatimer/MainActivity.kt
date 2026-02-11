package com.example.tabatatimer

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabatatimer.ui.theme.TabataTimerTheme

class MainActivity : ComponentActivity() {
    private var tabataService by mutableStateOf<TabataService?>(null)
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TabataService.LocalBinder
            tabataService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            tabataService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Garder l'écran allumé pendant l'utilisation de l'appli
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configuration pour afficher l'appli sur l'écran de verrouillage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val intent = Intent(this, TabataService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            TabataTimerTheme {
                val service = tabataService
                if (service != null) {
                    TabataApp(service)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun TabataApp(service: TabataService) {
    val currentState by service.currentState.collectAsState()
    val timeLeft by service.timeLeft.collectAsState()
    val currentCycle by service.currentCycle.collectAsState()
    val currentSet by service.currentSet.collectAsState()

    var config by remember { mutableStateOf(TabataConfig()) }

    when (currentState) {
        TimerState.IDLE -> {
            ConfigScreen(
                config = config,
                onConfigChange = { config = it },
                onStart = {
                    val intent = Intent(service, TabataService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        service.startForegroundService(intent)
                    } else {
                        service.startService(intent)
                    }
                    service.startTimer(config)
                }
            )
        }
        TimerState.FINISHED -> {
            FinishedScreen(onDone = { service.stopTimer() })
        }
        else -> {
            TimerScreen(
                state = currentState,
                timeLeft = timeLeft,
                cycle = currentCycle,
                totalCycles = config.cycles,
                set = currentSet,
                totalSets = config.sets,
                isInfinite = config.infiniteCycles,
                onStop = { service.stopTimer() },
                onPause = { service.pauseTimer() },
                onResume = { service.resumeTimer() }
            )
        }
    }
}

@Composable
fun FinishedScreen(onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFD700)) // Or / Jaune
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BRAVO !",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Séance terminée",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "RETOUR AU MENU",
                    color = Color(0xFFDAA520),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ConfigScreen(
    config: TabataConfig,
    onConfigChange: (TabataConfig) -> Unit,
    onStart: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("DÉMARRER LA SÉANCE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Configuration",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF536694)
            )

            TimeConfigRow("Préparation", config.prepare, Icons.AutoMirrored.Filled.DirectionsRun) { onConfigChange(config.copy(prepare = it)) }
            TimeConfigRow("Travail", config.work, Icons.Default.FitnessCenter) { onConfigChange(config.copy(work = it)) }
            TimeConfigRow("Repos", config.rest, Icons.Default.Timer) { onConfigChange(config.copy(rest = it)) }
            
            CycleConfigRow(
                config.cycles, 
                config.infiniteCycles,
                onValueChange = { onConfigChange(config.copy(cycles = it)) },
                onInfiniteChange = { onInfinite ->
                    onConfigChange(config.copy(infiniteCycles = onInfinite))
                }
            )

            ValueConfigRow("Séries", config.sets, Icons.Default.Replay) { onConfigChange(config.copy(sets = it)) }
            TimeConfigRow("Repos entre séries", config.restBetweenSets, Icons.Default.Weekend) { onConfigChange(config.copy(restBetweenSets = it)) }
            TimeConfigRow("Récupération finale", config.coolDown, Icons.Default.SelfImprovement) { onConfigChange(config.copy(coolDown = it)) }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TimeConfigRow(label: String, totalSeconds: Int, icon: ImageVector, onValueChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeDisplay = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

    ConfigBaseRow(label, timeDisplay, icon, 
        onDecrement = { if (totalSeconds > 0) onValueChange(totalSeconds - 1) },
        onIncrement = { onValueChange(totalSeconds + 1) },
        onValueClick = { showDialog = true }
    )

    if (showDialog) {
        DurationPickerDialog(
            title = label,
            initialSeconds = totalSeconds,
            onDismiss = { showDialog = false },
            onConfirm = { 
                onValueChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun CycleConfigRow(value: Int, isInfinite: Boolean, onValueChange: (Int) -> Unit, onInfiniteChange: (Boolean) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF536694))
        Spacer(Modifier.width(12.dp))
        Text(text = "Cycles", fontWeight = FontWeight.Bold, color = Color(0xFF2E3D59))
        
        Spacer(Modifier.width(12.dp))
        
        // Dark circular infinity button
        Surface(
            onClick = { onInfiniteChange(!isInfinite) },
            shape = CircleShape,
            color = if (isInfinite) Color(0xFF536694) else Color.LightGray.copy(alpha = 0.3f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "∞",
                    color = if (isInfinite) Color.White else Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > 1) onValueChange(value - 1) }, enabled = !isInfinite) {
                Icon(
                    Icons.Default.RemoveCircleOutline, 
                    contentDescription = null,
                    tint = if (isInfinite) Color.LightGray else Color.Black
                )
            }
            
            Surface(
                onClick = { if (!isInfinite) showDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = if (isInfinite) Color(0xFFC4D1F5) else Color(0xFFE5E9F2),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = if (isInfinite) "∞" else value.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isInfinite) Color(0xFF536694) else Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            IconButton(onClick = { onValueChange(value + 1) }, enabled = !isInfinite) {
                Icon(
                    Icons.Default.AddCircleOutline, 
                    contentDescription = null,
                    tint = if (isInfinite) Color.LightGray else Color.Black
                )
            }
        }
    }
    
    if (showDialog) {
        SimpleValuePickerDialog(
            title = "Nombre de cycles",
            initialValue = value,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ValueConfigRow(label: String, value: Int, icon: ImageVector, onValueChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    ConfigBaseRow(label, value.toString(), icon, 
        onDecrement = { if (value > 1) onValueChange(value - 1) },
        onIncrement = { onValueChange(value + 1) },
        onValueClick = { showDialog = true }
    )
    
    if (showDialog) {
        SimpleValuePickerDialog(
            title = label,
            initialValue = value,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ConfigBaseRow(label: String, valueText: String, icon: ImageVector, onDecrement: () -> Unit, onIncrement: () -> Unit, onValueClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = Color(0xFF536694))
        Spacer(Modifier.width(12.dp))
        Text(text = label, fontWeight = FontWeight.Bold, color = Color(0xFF2E3D59), modifier = Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Black)
            }
            Surface(
                onClick = onValueClick,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE5E9F2),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            IconButton(onClick = onIncrement) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun DurationPickerDialog(title: String, initialSeconds: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var minStr by remember { mutableStateOf((initialSeconds / 60).toString()) }
    var secStr by remember { mutableStateOf((initialSeconds % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditableNumberPicker(
                    valueStr = minStr, 
                    label = "min", 
                    onValueChange = { minStr = it }, 
                    onIncrement = {
                        val current = minStr.toIntOrNull() ?: 0
                        if (current < 99) minStr = (current + 1).toString()
                    },
                    onDecrement = {
                        val current = minStr.toIntOrNull() ?: 0
                        if (current > 0) minStr = (current - 1).toString()
                    }
                )
                Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                EditableNumberPicker(
                    valueStr = secStr, 
                    label = "sec", 
                    onValueChange = { secStr = it },
                    onIncrement = {
                        val s = secStr.toIntOrNull() ?: 0
                        if (s >= 59) {
                            secStr = "0"
                            val m = minStr.toIntOrNull() ?: 0
                            if (m < 99) minStr = (m + 1).toString()
                        } else {
                            secStr = (s + 1).toString()
                        }
                    },
                    onDecrement = {
                        val s = secStr.toIntOrNull() ?: 0
                        if (s <= 0) {
                            val m = minStr.toIntOrNull() ?: 0
                            if (m > 0) {
                                minStr = (m - 1).toString()
                                secStr = "59"
                            }
                        } else {
                            secStr = (s - 1).toString()
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                val finalMin = minStr.toIntOrNull() ?: 0
                val finalSec = secStr.toIntOrNull() ?: 0
                onConfirm(finalMin * 60 + finalSec) 
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun SimpleValuePickerDialog(title: String, initialValue: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var valStr by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EditableNumberPicker(
                    valueStr = valStr, 
                    label = "", 
                    onValueChange = { valStr = it },
                    onIncrement = {
                        val current = valStr.toIntOrNull() ?: 0
                        if (current < 999) valStr = (current + 1).toString()
                    },
                    onDecrement = {
                        val current = valStr.toIntOrNull() ?: 0
                        if (current > 1) valStr = (current - 1).toString()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(valStr.toIntOrNull() ?: initialValue) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun EditableNumberPicker(
    valueStr: String, 
    label: String, 
    onValueChange: (String) -> Unit, 
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
        }
        
        TextField(
            value = valueStr,
            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) onValueChange(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontSize = 28.sp, 
                fontWeight = FontWeight.Bold, 
                textAlign = TextAlign.Center,
                color = Color(0xFF536694)
            ),
            modifier = Modifier.width(80.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFE5E9F2),
                unfocusedContainerColor = Color(0xFFE5E9F2),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        if (label.isNotEmpty()) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TimerScreen(
    state: TimerState,
    timeLeft: Int,
    cycle: Int,
    totalCycles: Int,
    set: Int,
    totalSets: Int,
    isInfinite: Boolean,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val backgroundColor = when (state) {
        TimerState.PREPARE -> Color(0xFF2196F3)
        TimerState.WORK -> Color(0xFFF44336)
        TimerState.REST -> Color(0xFF4CAF50)
        TimerState.REST_BETWEEN_SETS -> Color(0xFFFF9800)
        TimerState.COOL_DOWN -> Color(0xFF9C27B0)
        TimerState.PAUSED -> Color.Gray
        else -> Color.Black
    }

    val stateText = when (state) {
        TimerState.PREPARE -> "PRÉPARATION"
        TimerState.WORK -> "TRAVAIL"
        TimerState.REST -> "REPOS"
        TimerState.REST_BETWEEN_SETS -> "REPOS SÉRIES"
        TimerState.COOL_DOWN -> "RÉCUPÉRATION"
        TimerState.PAUSED -> "PAUSE"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stateText,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            val displayMinutes = timeLeft / 60
            val displaySeconds = timeLeft % 60
            Text(
                text = if (displayMinutes > 0) "%d:%02d".format(displayMinutes, displaySeconds) else displaySeconds.toString(),
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Black
            )
            
            Text(
                text = if (isInfinite) "Cycle $cycle" else "Cycle $cycle / $totalCycles",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Série $set / $totalSets",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (state == TimerState.PAUSED) {
                    LargeCircularButton(onClick = onResume, icon = Icons.Default.PlayArrow, label = "REPRENDRE")
                } else {
                    LargeCircularButton(onClick = onPause, icon = Icons.Default.Pause, label = "PAUSE")
                }
                LargeCircularButton(onClick = onStop, icon = Icons.Default.Stop, label = "STOP", isSecondary = true)
            }
        }
    }
}

@Composable
fun LargeCircularButton(onClick: () -> Unit, icon: ImageVector, label: String, isSecondary: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isSecondary) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f)
            )
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
