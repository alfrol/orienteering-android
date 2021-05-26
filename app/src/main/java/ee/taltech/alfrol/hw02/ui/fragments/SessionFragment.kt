package ee.taltech.alfrol.hw02.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.service.LocationService
import ee.taltech.alfrol.hw02.ui.utils.CompassListener
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import kotlinx.coroutines.flow.first
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

@AndroidEntryPoint
class SessionFragment : Fragment(R.layout.fragment_session),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    EasyPermissions.PermissionCallbacks,
    CompassListener.OnCompassUpdateCallback {

    companion object {
        private const val GLOBAL_LOCATION_REQUEST_CODE = 0
        private const val MY_LOCATION_REQUEST_CODE = 1
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()

    private val openSettings: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.settings_button_open)
    }
    private val closeSettings: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.settings_button_close)
    }
    private val fromTop: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.from_top)
    }
    private val toTop: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.to_top)
    }

    private lateinit var compassListener: CompassListener
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var currentCompassAngle = 0.0f
    private var areSettingsOpen = false
    private var map: GoogleMap? = null
    private var points: MutableList<LatLng> = mutableListOf()
    private var polyline: Polyline? = null
    private var polylineOptions: PolylineOptions = PolylineOptions()
        .color(ContextCompat.getColor(requireContext(), R.color.primary))
        .width(10.0f)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)

        binding.fabSessionStart.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.color_fab_session)
        compassListener = CompassListener(requireContext(), this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this@SessionFragment)
        }

        // Instantiate the session data as bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.sessionData)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        sessionViewModel.sessionState.observe(viewLifecycleOwner, { sessionState ->
            if (sessionState == null) {
                return@observe
            }

            with(binding) {
                fabSessionStart.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), sessionState.buttonColor)
                fabSessionStart.setImageResource(sessionState.buttonIcon)
            }
        })
        sessionViewModel.compassState.observe(viewLifecycleOwner, { compassState ->
            if (compassState == null) {
                return@observe
            }

            with(binding) {
                imageViewCompass.clearAnimation()
                imageViewCompass.isVisible = compassState.isEnabled
                fabToggleCompass.setImageResource(compassState.compassButtonIcon)
            }
            if (compassState.isEnabled) {
                compassListener.startListening()
            } else {
                compassListener.stopListening()
            }
        })

        LocationService.points.observe(viewLifecycleOwner, { points ->
            if (sessionViewModel.isSessionRunning()) {
                this.points = points
                addLastPoint()
            }
        })

        with(binding) {
            fabSessionStart.setOnClickListener {
                // Only request for permissions here before session start
                if (!sessionViewModel.isSessionRunning() && !UIUtils.hasLocationPermission(
                        requireContext()
                    )
                ) {
                    requestLocationPermission()
                } else if (!sessionViewModel.isSessionRunning()) {
                    checkLocationSettings()
                } else {
                    sessionViewModel.stopSession()
                    startStopLocationService(C.ACTION_STOP_SERVICE)
                }
            }
            fabSettings.setOnClickListener {
                when (areSettingsOpen) {
                    true -> {
                        setSettingsVisibility(View.INVISIBLE)
                        setSettingsAnimation(toTop)
                        it.startAnimation(closeSettings)
                    }
                    false -> {
                        setSettingsVisibility(View.VISIBLE)
                        setSettingsAnimation(fromTop)
                        it.startAnimation(openSettings)
                    }
                }
                areSettingsOpen = !areSettingsOpen
            }
            fabCenterMapView.setOnClickListener {
                if (!UIUtils.hasLocationPermission(requireContext())) {
                    requestLocationPermission(MY_LOCATION_REQUEST_CODE)
                }
            }
            fabResetMapView.setOnClickListener {
                // TODO: Rotate the camera to point to the north
            }
            fabToggleCompass.setOnClickListener {
                sessionViewModel.toggleCompass()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        addAllPoints()

        if (sessionViewModel.isCompassEnabled()) {
            compassListener.startListening()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener(this)

        if (UIUtils.hasLocationPermission(requireContext())) {
            map?.isMyLocationEnabled = true
            with(map?.uiSettings) {
                this?.isMyLocationButtonEnabled = false
                this?.isCompassEnabled = false
            }
        }
    }

    override fun onMapClick(p0: LatLng) {
        toggleSessionData()
    }

    override fun onCompassUpdate(angle: Float) {
        val rotateAnim = RotateAnimation(
            currentCompassAngle,
            angle,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnim.duration = 100
        rotateAnim.fillAfter = true

        // Apply the compass image rotation animation based on the new angle
        binding.imageViewCompass.startAnimation(rotateAnim)
        currentCompassAngle = angle
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestLocationPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            GLOBAL_LOCATION_REQUEST_CODE -> {
                sessionViewModel.startSession()
                startStopLocationService(C.ACTION_START_SERVICE)
            }
            MY_LOCATION_REQUEST_CODE -> {
                // TODO: Navigate to the user location
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()

        if (sessionViewModel.isCompassEnabled()) {
            compassListener.stopListening()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    /**
     * Add the last point in points list to the map.
     */
    private fun addLastPoint() {
        if (points.isNotEmpty()) {
            if (polyline != null) {
                polyline!!.remove()
            }
            polylineOptions.add(points.last())
            polyline = map?.addPolyline(polylineOptions)
        }
    }

    /**
     * Add all points from the points list to the map.
     */
    private fun addAllPoints() {
        if (polyline != null) {
            polyline!!.remove()
        }
        polylineOptions.addAll(points)
        polyline = map?.addPolyline(polylineOptions)
    }

    /**
     * Set the visibility of settings buttons.
     */
    private fun setSettingsVisibility(visibility: Int) {
        with(binding) {
            fabCenterMapView.visibility = visibility
            fabCenterMapView.isEnabled = visibility == View.VISIBLE

            fabResetMapView.visibility = visibility
            fabResetMapView.isEnabled = visibility == View.VISIBLE

            fabToggleCompass.visibility = visibility
            fabToggleCompass.isEnabled = visibility == View.VISIBLE
        }
    }

    /**
     * Animate the settings buttons when opened/closed.
     */
    private fun setSettingsAnimation(animation: Animation) {
        with(binding) {
            fabCenterMapView.startAnimation(animation)
            fabResetMapView.startAnimation(animation)
            fabToggleCompass.startAnimation(animation)
        }
    }

    /**
     * Request location permissions.
     *
     * @param code Optional request code, by default [GLOBAL_LOCATION_REQUEST_CODE].
     */
    private fun requestLocationPermission(code: Int = GLOBAL_LOCATION_REQUEST_CODE) =
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.text_location_permission_rationale),
            code,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    /**
     * Toggle the session data bottom sheet.
     */
    private fun toggleSessionData() {
        bottomSheetBehavior.apply {
            state = if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                // Hide the bottom sheet if it was in the collapsed state
                BottomSheetBehavior.STATE_HIDDEN
            } else {
                // In any other case simply bring the bottom sheet back to collapsed state
                BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    /**
     * Start of stop the [LocationService].
     */
    private fun startStopLocationService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    /**
     * Check whether the suitable location settings are enabled for tracking.
     * If the result is successful the starts tracking, otherwise does nothing.
     */
    private fun checkLocationSettings() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            // Try to first get the location update preferences from the datastore
            val interval = settingsManager.locationUpdateInterval.first()
            val fastestInterval = settingsManager.locationUpdateFastestInterval.first()

            val request = UIUtils.getLocationRequest(interval, fastestInterval)
            val builder = LocationSettingsRequest.Builder().addLocationRequest(request)

            val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                sessionViewModel.startSession()
                startStopLocationService(C.ACTION_START_SERVICE)
            }
        }
    }
}