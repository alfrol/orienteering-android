package ee.taltech.alfrol.hw02.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StopwatchService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}