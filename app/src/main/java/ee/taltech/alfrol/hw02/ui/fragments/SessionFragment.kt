package ee.taltech.alfrol.hw02.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.databinding.SessionPopupMenuBinding
import ee.taltech.alfrol.hw02.service.LocationService
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.utils.CompassListener
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

@AndroidEntryPoint
class SessionFragment : Fragment(R.layout.fragment_session),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnCameraMoveStartedListener,
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
    private lateinit var polylineOptions: PolylineOptions

    private var isRunning = false
    private var isFollowingDevice = true
    private var currentCompassAngle = 0.0f
    private var areSettingsOpen = false
    private var map: GoogleMap? = null
    private var pathPoints: MutableList<LatLng> = mutableListOf()
    private var polyline: Polyline? = null
    private var checkpoints: MutableList<LatLng> = mutableListOf()
    private var waypoint: LatLng? = null
    private var waypointMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)

        binding.fabSessionStart.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.color_fab_session)
        compassListener = CompassListener(requireContext(), this)
        // TODO: Add possibility to customize
        polylineOptions = PolylineOptions()
            .color(ContextCompat.getColor(requireContext(), R.color.primary))
            .width(10.0f)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this@SessionFragment)

            fabSessionStart.setOnClickListener(onClickSessionStart)
            fabSettings.setOnClickListener(onClickSettings)
            fabCenterMapView.setOnClickListener(onClickCenterView)
            fabResetMapView.setOnClickListener(onClickResetMapView)
            fabToggleCompass.setOnClickListener(onClickToggleCompass)

            // Instantiate the session data as bottom sheet
            bottomSheetBehavior = BottomSheetBehavior.from(sessionData)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        startObserving()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        if (sessionViewModel.isCompassEnabled()) {
            compassListener.startListening()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
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
            GLOBAL_LOCATION_REQUEST_CODE -> startLocationService(C.ACTION_START_SERVICE)
            MY_LOCATION_REQUEST_CODE -> startLocationService(C.ACTION_GET_CURRENT_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener(this)
        map?.setOnMapLongClickListener(this)
        map?.setOnCameraMoveStartedListener(this)

        if (UIUtils.hasLocationPermission(requireContext())) {
            map?.isMyLocationEnabled = true
            with(map?.uiSettings) {
                this?.isMyLocationButtonEnabled = false
                this?.isCompassEnabled = false
            }

            // When the map is ready try to focus on current device location
            onClickCenterView.onClick(null)
        } else {
            checkLocationSettings()
        }

        // Add all path points, checkpoints and a waypoint if the map was recreated
        addAllPathPoints()
        addAllCheckpoints()
        addWaypoint()
    }

    override fun onMapClick(p0: LatLng) {
        toggleSessionData()
    }

    override fun onMapLongClick(location: LatLng) {
        showPopup(location)
    }

    override fun onCameraMoveStarted(reason: Int) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            isFollowingDevice = false
        }
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
     * Start (or stop) the [LocationService].
     */
    private fun startLocationService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    /**
     * Start observing the changes from the [LocationService] and [SessionViewModel].
     */
    private fun startObserving() {
        LocationService.isRunning.observe(viewLifecycleOwner, {
            updateIsRunning(it)
        })
        LocationService.pathPoints.observe(viewLifecycleOwner, {
            updatePathPoints(it)
        })
        LocationService.currentLocation.observe(viewLifecycleOwner, {
            isFollowingDevice = true
            navigateToLocation(it)
        })

        sessionViewModel.checkpoints.observe(viewLifecycleOwner, {
            updateCheckpoints(it)
        })
        sessionViewModel.waypoint.observe(viewLifecycleOwner, {
            updateWaypoint(it)
        })
        sessionViewModel.compassState.observe(viewLifecycleOwner, {
            updateCompassState(it)
        })
    }

    /**
     * Update the tracking state and session start/stop button style.
     */
    private fun updateIsRunning(isRunning: Boolean) {
        this.isRunning = isRunning

        with(binding) {
            val color = when (isRunning) {
                true -> R.color.red
                false -> R.color.secondary
            }
            val icon = when (isRunning) {
                true -> R.drawable.ic_stop
                false -> R.drawable.ic_start
            }

            with(fabSessionStart) {
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), color)
                setImageResource(icon)
            }
            loadingSpinner.visibility = View.GONE
        }
    }

    /**
     * If the session is running, update the path points list.
     */
    private fun updatePathPoints(pathPoints: MutableList<LatLng>) {
        if (isRunning) {
            this.pathPoints = pathPoints
            addLastPoint()
        }
    }

    /**
     * Update the list of checkpoints.
     */
    private fun updateCheckpoints(checkpoints: MutableList<LatLng>) {
        this.checkpoints = checkpoints
        addLastCheckpoint()
    }

    /**
     * Update current waypoint.
     */
    private fun updateWaypoint(waypoint: LatLng?) {
        this.waypoint = waypoint ?: return
        addWaypoint()
    }

    /**
     * Update the compass state (enable or disable it).
     */
    private fun updateCompassState(compassState: CompassState) {
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
    }

    /**
     * Listener for session start/stop button.
     */
    private val onClickSessionStart = View.OnClickListener {
        // Only request for permissions here before session start
        if (!isRunning && !UIUtils.hasLocationPermission(
                requireContext()
            )
        ) {
            requestLocationPermission()
        } else if (!isRunning) {
            checkLocationSettings()
        } else {
            startLocationService(C.ACTION_STOP_SERVICE)
        }

        binding.loadingSpinner.visibility = View.VISIBLE
    }

    /**
     * Listener for settings button.
     */
    private val onClickSettings = View.OnClickListener {
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

    /**
     * Listener for center view button.
     */
    private val onClickCenterView = View.OnClickListener {
        if (!UIUtils.hasLocationPermission(requireContext())) {
            requestLocationPermission(MY_LOCATION_REQUEST_CODE)
        } else {
            startLocationService(C.ACTION_GET_CURRENT_LOCATION)
        }
    }

    /**
     * Listener for reset map view button.
     */
    private val onClickResetMapView = View.OnClickListener {
        // TODO: Rotate the camera to point to the north
    }

    /**
     * Listener for compass button.
     */
    private val onClickToggleCompass = View.OnClickListener {
        sessionViewModel.toggleCompass()
    }

    /**
     * Add the last point in points list to the map.
     */
    private fun addLastPoint() {
        if (pathPoints.isNotEmpty()) {
            if (polyline != null) {
                polyline!!.remove()
            }

            val lastPathPoint = pathPoints.last()
            polylineOptions.add(lastPathPoint)
            polyline = map?.addPolyline(polylineOptions)

            if (isFollowingDevice) {
                navigateToLocation(lastPathPoint)
            }
        }
    }

    /**
     * Add all points from the points list to the map.
     */
    private fun addAllPathPoints() {
        if (polyline != null) {
            polyline!!.remove()
        }
        polylineOptions.addAll(pathPoints)
        polyline = map?.addPolyline(polylineOptions)

        if (isFollowingDevice && pathPoints.isNotEmpty()) {
            navigateToLocation(pathPoints.last())
        }
    }

    /**
     * Add the last checkpoint from the checkpoints list.
     */
    private fun addLastCheckpoint() {
        if (checkpoints.isNotEmpty()) {
            val markerOptions = MarkerOptions().position(checkpoints.last())
            map?.addMarker(markerOptions)
        }
    }

    /**
     * Add all checkpoint markers to the map.
     */
    private fun addAllCheckpoints() =
        checkpoints.forEach { ckpt ->
            val markerOptions = MarkerOptions().position(ckpt)
            map?.addMarker(markerOptions)
        }

    /**
     * Add a new waypoint marker to the map.
     */
    private fun addWaypoint() =
        waypoint?.let {
            if (waypointMarker != null) {
                waypointMarker!!.remove()
            }
            val markerOptions = MarkerOptions().position(it)
            waypointMarker = map?.addMarker(markerOptions)
        }

    /**
     * Navigate the camera to the current location.
     *
     * @param location Location on the map to navigate to.
     */
    private fun navigateToLocation(location: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))
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
     * Check whether the suitable location settings are enabled for tracking.
     * If the result is successful the starts tracking, otherwise does nothing.
     */
    private fun checkLocationSettings() {
        val interval: Long
        val fastestInterval: Long

        runBlocking {
            interval = settingsManager.locationUpdateInterval.first()
            fastestInterval = settingsManager.locationUpdateFastestInterval.first()
        }

        val request = UIUtils.getLocationRequest(interval, fastestInterval)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)

        val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationService(C.ACTION_START_SERVICE)
            startLocationService(C.ACTION_GET_CURRENT_LOCATION)
        }

        task.addOnFailureListener {
            // TODO: Handle the case where settings are not enabled
        }
    }

    /**
     * Show popup for adding checkpoint or waypoint on the map at the specified location.
     *
     * @param location Where to add a new checkpoint.
     */
    private fun showPopup(location: LatLng) {
        val popupViewBinding = SessionPopupMenuBinding.inflate(layoutInflater)
        val popupView = popupViewBinding.root

        val width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val height = ConstraintLayout.LayoutParams.WRAP_CONTENT

        val popupWindow = PopupWindow(popupView, width, height, true)
        val position = map?.projection?.toScreenLocation(location) ?: Point()

        popupWindow.showAtLocation(
            binding.imageViewCompass,
            Gravity.NO_GRAVITY,
            position.x,
            position.y
        )

        popupViewBinding.buttonAddCheckpoint.setOnClickListener {
            sessionViewModel.addCheckpoint(location)
            popupWindow.dismiss()
        }
        popupViewBinding.buttonAddWaypoint.setOnClickListener {
            sessionViewModel.addWaypoint(location)
            popupWindow.dismiss()
        }
    }
}