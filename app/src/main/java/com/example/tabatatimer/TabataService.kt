package com.example.tabatatimer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerState {
    IDLE, PREPARE, WORK, REST, REST_BETWEEN_SETS, COOL_DOWN, FINISHED, PAUSED
}

data class TabataConfig(
    val prepare: Int = 10,
    val work: Int = 20,
    val rest: Int = 10,
    val cycles: Int = 8,
    val sets: Int = 1,
    val restBetweenSets: Int = 0,
    val coolDown: Int = 0,
    val infiniteCycles: Boolean = false
)

class TabataService : Service() {

    private val binder = LocalBinder()
    private var timer: CountDownTimer? = null
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 60)
    private var vibrator: Vibrator? = null
    
    private var soundPool: SoundPool? = null
    private var bellSoundId: Int = -1

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft = _timeLeft.asStateFlow()

    private val _currentState = MutableStateFlow(TimerState.IDLE)
    val currentState = _currentState.asStateFlow()

    private val _currentCycle = MutableStateFlow(1)
    val currentCycle = _currentCycle.asStateFlow()

    private val _countSet = MutableStateFlow(1)
    val currentSet = _countSet.asStateFlow()

    private var config = TabataConfig()
    private var stateBeforePause: TimerState = TimerState.IDLE

    inner class LocalBinder : Binder() {
        fun getService(): TabataService = this@TabataService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        initSoundPool()
        createNotificationChannel()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        bellSoundId = soundPool?.load(this, R.raw.boxing_bell, 1) ?: -1
    }

    fun startTimer(tabataConfig: TabataConfig) {
        config = tabataConfig
        _countSet.value = 1
        _currentCycle.value = 1
        
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        
        // Start foreground with full screen intent immediately
        startForeground(1, createNotification("Préparation..."))
        
        runNextPhase(TimerState.PREPARE)
    }

    private fun runNextPhase(state: TimerState) {
        _currentState.value = state
        val duration = when (state) {
            TimerState.PREPARE -> config.prepare
            TimerState.WORK -> config.work
            TimerState.REST -> config.rest
            TimerState.REST_BETWEEN_SETS -> config.restBetweenSets
            TimerState.COOL_DOWN -> config.coolDown
            else -> 0
        }

        if (duration <= 0 && state != TimerState.FINISHED) {
            advanceState()
            return
        }

        if (state == TimerState.FINISHED) {
            _timeLeft.value = 0
            updateNotification("Séance terminée !")
            playBoxingBell()
            vibrate(longArrayOf(0, 800, 200, 800))
            stopForeground(STOP_FOREGROUND_DETACH)
            return
        }

        startCountdown(duration)
    }

    private fun startCountdown(seconds: Int) {
        timer?.cancel()
        _timeLeft.value = seconds
        timer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000).toInt()
                _timeLeft.value = remaining
                updateNotification("${translateState(_currentState.value)}: ${formatTime(remaining)}")
                
                if (remaining in 1..5) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    vibrate(100)
                }
            }

            override fun onFinish() {
                playBoxingBell()
                vibrate(500)
                advanceState()
            }
        }.start()
    }

    private fun playBoxingBell() {
        if (bellSoundId != -1) {
            soundPool?.play(bellSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            toneGenerator.startTone(ToneGenerator.TONE_SUP_PIP, 500)
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "%d:%02d".format(m, s) else "$s s"
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    private fun advanceState() {
        when (_currentState.value) {
            TimerState.PREPARE -> runNextPhase(TimerState.WORK)
            TimerState.WORK -> {
                if (config.infiniteCycles || _currentCycle.value < config.cycles) {
                    runNextPhase(TimerState.REST)
                } else if (_countSet.value < config.sets) {
                    _currentCycle.value = 1
                    _countSet.value++
                    runNextPhase(TimerState.REST_BETWEEN_SETS)
                } else {
                    runNextPhase(TimerState.COOL_DOWN)
                }
            }
            TimerState.REST -> {
                _currentCycle.value++
                runNextPhase(TimerState.WORK)
            }
            TimerState.REST_BETWEEN_SETS -> runNextPhase(TimerState.WORK)
            TimerState.COOL_DOWN -> runNextPhase(TimerState.FINISHED)
            else -> {}
        }
    }

    fun pauseTimer() {
        if (_currentState.value != TimerState.PAUSED) {
            stateBeforePause = _currentState.value
            _currentState.value = TimerState.PAUSED
            timer?.cancel()
            updateNotification("Pause")
        }
    }

    fun resumeTimer() {
        if (_currentState.value == TimerState.PAUSED) {
            _currentState.value = stateBeforePause
            startCountdown(_timeLeft.value)
        }
    }

    fun stopTimer() {
        timer?.cancel()
        _currentState.value = TimerState.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun translateState(state: TimerState): String {
        return when (state) {
            TimerState.PREPARE -> "Préparation"
            TimerState.WORK -> "Travail"
            TimerState.REST -> "Repos"
            TimerState.REST_BETWEEN_SETS -> "Repos séries"
            TimerState.COOL_DOWN -> "Récupération"
            TimerState.FINISHED -> "Terminé"
            TimerState.PAUSED -> "Pause"
            else -> ""
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tabata_channel",
                "Tabata Timer",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, "tabata_channel")
            .setContentTitle("Chronomètre Tabata")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Forcer l'affichage sur écran de verrouillage
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        return builder.build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        timer?.cancel()
        toneGenerator.release()
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }
}
