package ee.taltech.alfrol.hw02.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ee.taltech.alfrol.hw02.C
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StopwatchService : LifecycleService() {

    companion object {
        val total = MutableLiveData<Long>()
        val checkpoint = MutableLiveData<Long>()
        val waypoint = MutableLiveData<Long>()

        private const val STOPWATCH_TOTAL = 0
        private const val STOPWATCH_CHECKPOINT = 1
        private const val STOPWATCH_WAYPOINT = 2
    }

    private val intentFilter = IntentFilter().apply {
        addAction(C.ACTION_START_CHECKPOINT_STOPWATCH)
        addAction(C.ACTION_START_WAYPOINT_STOPWATCH)
    }
    private val stopwatchBroadcastReceiver = StopwatchBroadcastReceiver()

    private var isRunning = false
    private var isCheckpointRunning = false
    private var isWaypointRunning = false

    private var duration = 0L
    private var checkpointDuration = 0L
    private var waypointDuration = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                C.ACTION_START_SERVICE -> startStopwatch(STOPWATCH_TOTAL)
                C.ACTION_STOP_SERVICE -> stopStopwatch()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStopwatch()
    }

    /**
     * Post the initial values for the livedata.
     */
    private fun postInitialValues() {
        isRunning = false
        isCheckpointRunning = false
        isWaypointRunning = false

        duration = 0L
        checkpointDuration = 0L
        waypointDuration = 0L

        total.postValue(0L)
        checkpoint.postValue(0L)
        waypoint.postValue(0L)
    }

    /**
     * Start the stopwatch.
     *
     * @param type Type of the stopwatch to start. One of the
     * [STOPWATCH_TOTAL], [STOPWATCH_CHECKPOINT], [STOPWATCH_WAYPOINT].
     */
    private fun startStopwatch(type: Int) {
        when (type) {
            // Reset all values here.
            STOPWATCH_TOTAL -> {
                postInitialValues()
                isRunning = true
                run()
                LocalBroadcastManager
                    .getInstance(this)
                    .registerReceiver(stopwatchBroadcastReceiver, intentFilter)
            }
            // When checkpoint or waypoint stopwatch is started it's automatically reset
            STOPWATCH_CHECKPOINT -> {
                isCheckpointRunning = true
                checkpointDuration = 0L
                checkpoint.postValue(0L)
            }
            STOPWATCH_WAYPOINT -> {
                isWaypointRunning = true
                waypointDuration = 0L
                waypoint.postValue(0L)
            }
        }
    }

    /**
     * Stop the stopwatch and reset everything.
     */
    private fun stopStopwatch() {
        postInitialValues()

        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(stopwatchBroadcastReceiver)
        stopSelf()
    }

    /**
     * Start running the stopwatch.
     * It's run as a coroutine with delay of 1 ms.
     */
    private fun run() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isRunning) {
                if (isCheckpointRunning) {
                    checkpointDuration++
                    checkpoint.postValue(checkpointDuration)
                }
                if (isWaypointRunning) {
                    waypointDuration++
                    waypoint.postValue(waypointDuration)
                }

                duration++
                total.postValue(duration)

                delay(1)
            }
        }
    }

    /**
     * Broadcast receiver for the secondary stopwatch actions.
     */
    private inner class StopwatchBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    C.ACTION_START_CHECKPOINT_STOPWATCH -> startStopwatch(STOPWATCH_CHECKPOINT)
                    C.ACTION_START_WAYPOINT_STOPWATCH -> startStopwatch(STOPWATCH_WAYPOINT)
                }
            }
        }
    }
}