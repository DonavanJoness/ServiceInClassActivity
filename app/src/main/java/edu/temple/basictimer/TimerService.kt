package edu.temple.basictimer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerInfo {
    private val _currentTime = MutableStateFlow(0)
    val currentTime = _currentTime.asStateFlow()

    fun postNewTime(currentTime: Int) {
        _currentTime.value = currentTime
    }
}

class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var paused = false
    private var remainingTime = 0

    private val tick = object : Runnable {
        override fun run() {
            if (!isRunning) return
            remainingTime--
            TimerInfo.postNewTime(remainingTime)
            if (remainingTime > 0) {
                handler.postDelayed(this, 1000)
            } else {
                isRunning = false
            }
        }
    }

    inner class TimerBinder : Binder() {
        val isRunning: Boolean
            get() = this@TimerService.isRunning
        val isPaused: Boolean
            get() = paused

        fun start(startValue: Int) {
            this@TimerService.start(startValue)
        }

        fun stop() {
            stopTimer()
        }

        fun pause() {
            this@TimerService.pause()
        }
    }

    override fun onBind(intent: Intent): IBinder = TimerBinder()

    fun start(startValue: Int) {
        if (isRunning) return
        if (paused) {
            paused = false
            isRunning = true
            handler.postDelayed(tick, 1000)
            return
        }
        remainingTime = startValue.coerceAtLeast(0)
        TimerInfo.postNewTime(remainingTime)
        isRunning = remainingTime > 0
        if (isRunning) handler.postDelayed(tick, 1000)
    }

    fun pause() {
        if (paused) {
            start(remainingTime)
        } else if (isRunning) {
            handler.removeCallbacks(tick)
            paused = true
            isRunning = false
        }
    }

    private fun stopTimer() {
        handler.removeCallbacks(tick)
        isRunning = false
        paused = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopTimer()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
