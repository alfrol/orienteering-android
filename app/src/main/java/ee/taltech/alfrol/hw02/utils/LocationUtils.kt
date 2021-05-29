package ee.taltech.alfrol.hw02.utils

import com.google.android.gms.location.LocationRequest

object LocationUtils {

    /**
     * Construct a new [LocationRequest].
     */
    fun getLocationRequest(interval: Long, fastestInterval: Long): LocationRequest {
        return LocationRequest.create().apply {
            this.interval = interval
            this.fastestInterval = fastestInterval
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
}