package ee.taltech.alfrol.hw02.ui.utils

import android.Manifest
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.gms.location.LocationRequest
import pub.devrel.easypermissions.EasyPermissions

/**
 * A collection of utility functions for operating with the UI.
 */
object UIUtils {

    /**
     * Show a small informational message to the user.
     * Mostly meant for displaying an error message.
     */
    fun showToast(context: Context, @StringRes message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Construct a [TextWatcher] instance with the provided [callback].
     *
     * @param callback A function that will be executed on [TextWatcher.afterTextChanged].
     */
    fun getTextChangeListener(callback: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length != 0 || s.isEmpty() && before > 0) {
                    callback()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    /**
     * Check whether the app has location permissions.
     */
    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)

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