package ee.taltech.alfrol.hw02.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import ee.taltech.alfrol.hw02.C
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class StopwatchService : LifecycleService() {

    companion object {
        val total = MutableLiveData(0L)
        val checkpoint = MutableLiveData(0L)
        val waypoint = MutableLiveData(0L)
    }

    private lateinit var executorService: ScheduledExecutorService

    private var isTotalRunning = false
    private var isCheckpointRunning = false
    private var isWaypointRunning = false

    private var totalDuration = 0L
    private var checkpointDuration = 0L
    private var waypointDuration = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val stopwatchType = it.getIntExtra(C.STOPWATCH_TYPE_KEY, C.STOPWATCH_TOTAL)

            when (it.action) {
                C.ACTION_START_SERVICE -> startStopwatch(stopwatchType)
                C.ACTION_STOP_SERVICE -> stopStopwatch(stopwatchType)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::executorService.isInitialized) {
            executorService.shutdown()
        }
    }

    /**
     * Start running the stopwatch.
     *
     * Starting the stopwatch (whichever type) resets it.
     *
     * @param which What type of stopwatch to run. Should be one of the
     * [C.STOPWATCH_TOTAL], [C.STOPWATCH_CHECKPOINT], [C.STOPWATCH_WAYPOINT]
     */
    private fun startStopwatch(which: Int) {
        when (which) {
            C.STOPWATCH_TOTAL -> {
                isTotalRunning = true
                totalDuration = 0L

                // We only need to start the timer once when total stopwatch is started.
                run()
            }
            C.STOPWATCH_CHECKPOINT -> {
                isCheckpointRunning = true
                checkpointDuration = 0L
            }
            C.STOPWATCH_WAYPOINT -> {
                isWaypointRunning = true
                waypointDuration = 0L
            }
        }
    }

    /**
     * Stop running the stopwatch.
     *
     * @param which What type of stopwatch to stop. Should be one of the
     * [C.STOPWATCH_TOTAL], [C.STOPWATCH_CHECKPOINT], [C.STOPWATCH_WAYPOINT]
     */
    private fun stopStopwatch(which: Int) {
        when (which) {
            C.STOPWATCH_TOTAL -> {
                isTotalRunning = false
                isCheckpointRunning = false
                isWaypointRunning = false

                // When total stopwatch is shutdown then all others must also be shutdown
                executorService.shutdown()
            }
            C.STOPWATCH_CHECKPOINT -> {
                isCheckpointRunning = false
            }
            C.STOPWATCH_WAYPOINT -> {
                isWaypointRunning = false
            }
        }
    }

    /**
     * Start the executor service with the interval of 1 millisecond.
     */
    private fun run() {
        executorService = Executors.newScheduledThreadPool(1)
        executorService.scheduleAtFixedRate(
            {
                if (isTotalRunning) {
                    total.postValue(++totalDuration)
                }
                if (isCheckpointRunning) {
                    checkpoint.postValue(++checkpointDuration)
                }
                if (isWaypointRunning) {
                    waypoint.postValue(++waypointDuration)
                }
            },
            0, 1, TimeUnit.MILLISECONDS
        )
    }
}