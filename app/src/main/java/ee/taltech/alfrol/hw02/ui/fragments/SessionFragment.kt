package ee.taltech.alfrol.hw02.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import ee.taltech.alfrol.hw02.service.StopwatchService
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

        private const val SESSION_DATA_TOTAL = 0
        private const val SESSION_DATA_CHECKPOINT = 1
        private const val SESSION_DATA_WAYPOINT = 2
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()
    private val args: SessionFragmentArgs by navArgs()

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
            if (args.isPreview) {
                bottomSheetBehavior.isDraggable = false
            }
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
            GLOBAL_LOCATION_REQUEST_CODE -> {
                startLocationService(C.ACTION_START_SERVICE)
                startStopwatchService(C.ACTION_START_SERVICE, C.STOPWATCH_TOTAL)
            }
            MY_LOCATION_REQUEST_CODE -> startLocationService(C.ACTION_GET_CURRENT_LOCATION)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        with(map?.uiSettings) {
            this?.isMyLocationButtonEnabled = false
            this?.isCompassEnabled = false
        }

        if (args.isPreview) {
            setupMapForPreview()
        } else {
            setupMapForSession()
        }
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

    @SuppressLint("MissingPermission")
    private fun setupMapForSession() {
        map?.setOnMapClickListener(this)
        map?.setOnMapLongClickListener(this)
        map?.setOnCameraMoveStartedListener(this)

        if (UIUtils.hasLocationPermission(requireContext())) {
            map?.isMyLocationEnabled = true

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

    private fun setupMapForPreview() {
        with(binding) {
            fabSessionStart.visibility = View.GONE
            fabSettings.visibility = View.GONE
        }

        sessionViewModel.getSessionWithLocationPoints(args.previewedSessionId)
            .observe(viewLifecycleOwner, {
                it?.let { sessionWithLocationPoints ->
                    val session = sessionWithLocationPoints.session
                    val locationPoints = sessionWithLocationPoints.locationPoints

                    Log.d("SessionFragment", "setupMapForPreview: $locationPoints")

                    updateDistance(session.distance, SESSION_DATA_TOTAL)
                    updateDuration(session.duration, SESSION_DATA_TOTAL)
                    updatePace(session.pace, SESSION_DATA_TOTAL)

                    locationPoints.forEach { point ->
                        val latLng = LatLng(point.latitude, point.longitude)

                        when (point.type) {
                            C.LOC_TYPE_ID -> pathPoints.add(latLng)
                            C.CP_TYPE_ID -> checkpoints.add(latLng)
                        }
                    }
                    addAllPathPoints()
                    addAllCheckpoints()
                }
            })
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
     * Start the [StopwatchService].
     *
     * @param action One of the [C.ACTION_START_SERVICE], [C.ACTION_STOP_SERVICE].
     * @param stopwatchType The type of stopwatch to start. Should be one of the
     * [C.STOPWATCH_TOTAL], [C.STOPWATCH_CHECKPOINT], [C.STOPWATCH_WAYPOINT]
     */
    private fun startStopwatchService(action: String, stopwatchType: Int) =
        Intent(requireContext(), StopwatchService::class.java).also {
            it.action = action
            it.putExtra(C.STOPWATCH_TYPE_KEY, stopwatchType)
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
        LocationService.checkpoints.observe(viewLifecycleOwner, {
            updateCheckpoints(it)
        })
        LocationService.waypoint.observe(viewLifecycleOwner, {
            updateWaypoint(it)
        })
        LocationService.currentLocation.observe(viewLifecycleOwner, {
            isFollowingDevice = true
            navigateToLocation(it)
        })
        LocationService.totalDistance.observe(viewLifecycleOwner, {
            updateDistance(it, SESSION_DATA_TOTAL)
        })
        LocationService.totalAveragePace.observe(viewLifecycleOwner, {
            updatePace(it, SESSION_DATA_TOTAL)
        })
        LocationService.checkpointDistance.observe(viewLifecycleOwner, {
            updateDistance(it, SESSION_DATA_CHECKPOINT)
        })
        LocationService.checkpointAveragePace.observe(viewLifecycleOwner, {
            updatePace(it, SESSION_DATA_CHECKPOINT)
        })
        LocationService.waypointDistance.observe(viewLifecycleOwner, {
            updateDistance(it, SESSION_DATA_WAYPOINT)
        })
        LocationService.waypointAveragePace.observe(viewLifecycleOwner, {
            updatePace(it, SESSION_DATA_WAYPOINT)
        })

        StopwatchService.total.observe(viewLifecycleOwner, {
            updateDuration(it, SESSION_DATA_TOTAL)
        })
        StopwatchService.checkpoint.observe(viewLifecycleOwner, {
            updateDuration(it, SESSION_DATA_CHECKPOINT)
        })
        StopwatchService.waypoint.observe(viewLifecycleOwner, {
            updateDuration(it, SESSION_DATA_WAYPOINT)
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
     * Update the distance value.
     *
     * @param distance New distance to set to the textview.
     * @param type Which distance type to update.
     */
    private fun updateDistance(distance: Float, type: Int) {
        val formattedDistance = UIUtils.formatDistance(requireContext(), distance)

        with(binding) {
            when (type) {
                SESSION_DATA_TOTAL -> totalDistance.text = formattedDistance
                SESSION_DATA_CHECKPOINT -> checkpointDistance.text = formattedDistance
                SESSION_DATA_WAYPOINT -> waypointDistance.text = formattedDistance
            }
        }
    }

    /**
     * Update the average pace.
     *
     * @param pace New pace to set to the textview.
     * @param type Which pace type to update.
     */
    private fun updatePace(pace: Float, type: Int) {
        val formattedPace = getString(R.string.pace, pace)

        with(binding) {
            when (type) {
                SESSION_DATA_TOTAL -> totalPace.text = formattedPace
                SESSION_DATA_CHECKPOINT -> checkpointPace.text = formattedPace
                SESSION_DATA_WAYPOINT -> waypointPace.text = formattedPace
            }
        }
    }

    /**
     * Update the duration value.
     *
     * @param duration New duration to set to the textview.
     * @param type Which duration type to update.
     */
    private fun updateDuration(duration: Long, type: Int) {
        val durationFormatted = UIUtils.formatDuration(requireContext(), duration)

        with(binding) {
            when (type) {
                SESSION_DATA_TOTAL -> totalDuration.text = durationFormatted
                SESSION_DATA_CHECKPOINT -> checkpointDuration.text = durationFormatted
                SESSION_DATA_WAYPOINT -> waypointDuration.text = durationFormatted
            }
        }

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
            showEndDialog()
        }
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
        polylineOptions = PolylineOptions()
            .color(ContextCompat.getColor(requireContext(), R.color.primary))
            .width(10.0f)
            .addAll(pathPoints)
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
     * Show dialog before stopping the session to ensure user does not accidentally stop it.
     */
    private fun showEndDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_stop_session)
            .setMessage(R.string.text_stopping_confirmation)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                binding.loadingSpinner.visibility = View.VISIBLE
                stopSession()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun stopSession() {
        startLocationService(C.ACTION_STOP_SERVICE)
        startStopwatchService(C.ACTION_STOP_SERVICE, C.STOPWATCH_TOTAL)
        findNavController().popBackStack()
        UIUtils.showToast(requireContext(), R.string.text_session_ended)
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
            binding.loadingSpinner.visibility = View.VISIBLE

            startLocationService(C.ACTION_START_SERVICE)
            startLocationService(C.ACTION_GET_CURRENT_LOCATION)
            startStopwatchService(C.ACTION_START_SERVICE, C.STOPWATCH_TOTAL)
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
            LocationService.addNewCheckpoint(location)
            startStopwatchService(C.ACTION_START_SERVICE, C.STOPWATCH_CHECKPOINT)
            popupWindow.dismiss()
        }
        popupViewBinding.buttonAddWaypoint.setOnClickListener {
            LocationService.addNewWaypoint(location)
            startStopwatchService(C.ACTION_START_SERVICE, C.STOPWATCH_WAYPOINT)
            popupWindow.dismiss()
        }
    }
}