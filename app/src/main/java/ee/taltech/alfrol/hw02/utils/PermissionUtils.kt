package ee.taltech.alfrol.hw02.utils

import android.Manifest
import android.content.Context
import androidx.fragment.app.Fragment
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import pub.devrel.easypermissions.EasyPermissions

object PermissionUtils {

    /**
     * Check whether the app has location permission.
     */
    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasStorageWritePermission(context: Context) =
        EasyPermissions.hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * Request the location permission.
     */
    fun requestLocationPermission(host: Fragment) =
        EasyPermissions.requestPermissions(
            host,
            host.getString(R.string.text_location_permission_rationale),
            C.LOCATION_PERMISSION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    /**
     * Request the storage write permission.
     */
    fun requestStorageWritePermission(host: Fragment) =
        EasyPermissions.requestPermissions(
            host,
            host.getString(R.string.text_storage_write_permission_rationale),
            C.STORAGE_WRITE_PERMISSION_REQUEST_CODE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
}