package ee.taltech.alfrol.hw02.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.service.LocationService
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.PolylineState
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import ee.taltech.alfrol.hw02.utils.CompassListener
import ee.taltech.alfrol.hw02.utils.LocationUtils
import ee.taltech.alfrol.hw02.utils.PermissionUtils
import ee.taltech.alfrol.hw02.utils.UIUtils
import kotlinx.coroutines.delay
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

@AndroidEntryPoint
class SessionFragment : Fragment(R.layout.fragment_session),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    EasyPermissions.PermissionCallbacks,
    GoogleMap.OnCameraMoveStartedListener,
    CompassListener.OnCompassUpdateCallback {

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val args: SessionFragmentArgs by navArgs()
    private val sessionViewModel: SessionViewModel by viewModels()

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

    private var isViewMode = false
    private var isTracking = false
    private var isFollowingDevice = true
    private var currentCompassAngle = 0.0f
    private var isCompassEnabled = false
    private var areSettingsOpen = false

    private var map: GoogleMap? = null
    private var polylineState = PolylineState(C.DEFAULT_POLYLINE_COLOR, C.DEFAULT_POLYLINE_WIDTH)
    private var waypointMarker: Marker? = null

    private var pathPoints: MutableList<Location> = mutableListOf()
    private var checkpoints: MutableList<Location> = mutableListOf()
    private var waypoint: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)

        isViewMode = args.isPreview

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

            fabSessionStart.setOnClickListener(onClickSessionStart)
            fabSettings.setOnClickListener(onClickSettings)
            fabCenterMapView.setOnClickListener(onClickCenterMapView)
            fabResetMapView.setOnClickListener {
                resetCameraRotation()
            }
            fabToggleCompass.setOnClickListener {
                sessionViewModel.toggleCompass()
            }
            fabAddCheckpoint.setOnClickListener(onClickAddCheckpoint)
            fabAddWaypoint.setOnClickListener(onClickAddWaypoint)

            // Instantiate the session data as bottom sheet
            bottomSheetBehavior = BottomSheetBehavior.from(sessionData)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        startObserving()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        if (isCompassEnabled) {
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
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // Ignore
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build()
        } else {
            PermissionUtils.requestLocationPermission(this)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Only listen to map events in session mode
        if (!isViewMode) {
            map?.setOnMapClickListener(this)
            map?.setOnCameraMoveStartedListener(this)

            if (PermissionUtils.hasLocationPermission(requireContext())) {
                map?.isMyLocationEnabled = true
                // When map is ready wait for 1 second and navigate to the current location
                lifecycleScope.launchWhenCreated {
                    delay(1000L)
                    startLocationService(C.ACTION_GET_CURRENT_LOCATION)
                }
            }
        }

        with(map?.uiSettings) {
            this?.isMyLocationButtonEnabled = false
            this?.isCompassEnabled = false
        }
        restoreMapData()
    }

    override fun onMapClick(p0: LatLng) {
        toggleSessionData()
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

        if (isCompassEnabled) {
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
     * Start observing the livedata changes from the [LocationService] and [SessionViewModel].
     */
    private fun startObserving() {
        LocationService.isTracking.observe(viewLifecycleOwner, isTrackingObserver)
        LocationService.pathPoints.observe(viewLifecycleOwner, pathPointsObserver)
        LocationService.checkpoints.observe(viewLifecycleOwner, checkpointsObserver)
        LocationService.waypoint.observe(viewLifecycleOwner, {
            waypoint = it?.also { addWaypoint() }
        })
        LocationService.currentLocation.observe(viewLifecycleOwner, {
            it?.let { loc -> navigateToCurrentLocation(loc) }
        })

        sessionViewModel.polylineState.observe(viewLifecycleOwner, {
            polylineState = it?.also { addAllPathPoints() } ?: polylineState
        })
        sessionViewModel.compassState.observe(viewLifecycleOwner, {
            it?.let { compassState -> toggleCompass(compassState) }
        })
    }

    /**
     * Start the [LocationService] with the given action.
     *
     * @param action Action to set in the intent.
     */
    private fun startLocationService(action: String) =
        Intent(requireContext(), LocationService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }

    /**
     * Send the broadcast with the given action.
     *
     * @param action Action to use in the broadcast intent.
     */
    private fun sendLocationActionBroadcast(action: String) =
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent(action))

    /**
     * Listener for session start/stop button.
     */
    private val onClickSessionStart = View.OnClickListener {
        if (isTracking) {
            startLocationService(C.ACTION_STOP_SERVICE)
        } else {
            if (PermissionUtils.hasLocationPermission(requireContext())) {
                startLocationService(C.ACTION_START_SERVICE)
            } else {
                PermissionUtils.requestLocationPermission(this)
            }
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
    private val onClickCenterMapView = View.OnClickListener {
        if (PermissionUtils.hasLocationPermission(requireContext())) {
            startLocationService(C.ACTION_GET_CURRENT_LOCATION)
        } else {
            PermissionUtils.requestLocationPermission(this)
        }
    }

    /**
     * Listener for checkpoint adding button.
     */
    private val onClickAddCheckpoint = View.OnClickListener {
        if (isTracking) {
            sendLocationActionBroadcast(C.ACTION_ADD_CHECKPOINT)
        }
    }

    /**
     * Listener for waypoint adding button.
     */
    private val onClickAddWaypoint = View.OnClickListener {
        if (isTracking) {
            sendLocationActionBroadcast(C.ACTION_ADD_WAYPOINT)
        }
    }

    /**
     * Observer for [LocationService.isTracking].
     */
    private val isTrackingObserver = Observer<Boolean> {
        isTracking = it ?: false
        setSessionButtonStyle()
        setLocationActionButtonsVisibility()
    }

    /**
     * Observer for [LocationService.pathPoints].
     */
    private val pathPointsObserver = Observer<MutableList<Location>> {
        pathPoints = it ?: mutableListOf()
        addLastPathPoint()

        if (pathPoints.isNotEmpty()) {
            sessionViewModel.saveLocationPoint(pathPoints.last(), C.LOC_TYPE_ID)
        }
    }

    /**
     * Observer for [LocationService.checkpoints].
     */
    private val checkpointsObserver = Observer<MutableList<Location>> {
        checkpoints = it ?: mutableListOf()
        addLastCheckpoint()

        if (checkpoints.isNotEmpty()) {
            sessionViewModel.saveLocationPoint(checkpoints.last(), C.CP_TYPE_ID)
        }
    }

    /**
     * Add the last point from the path points to the map.
     */
    private fun addLastPathPoint() {
        if (!isTracking || pathPoints.size < 2) {
            return
        }

        val lastPathPoint = pathPoints.last()
        val preLastPathPoint = pathPoints[pathPoints.lastIndex - 1]
        val latLngLast = LatLng(lastPathPoint.latitude, lastPathPoint.longitude)
        val latLngPreLast = LatLng(preLastPathPoint.latitude, preLastPathPoint.longitude)

        val polylineOptions =
            UIUtils.getPolylineOptions(requireContext(), polylineState.color, polylineState.width)
        polylineOptions.add(latLngPreLast).add(latLngLast)
        map?.addPolyline(polylineOptions)

        // If following the device then always keep the last path point centered.
        if (isFollowingDevice) {
            navigateToCurrentLocation(lastPathPoint)
        }
    }

    /**
     * Add all the points from the path points list to the map.
     */
    private fun addAllPathPoints() {
        if (pathPoints.isEmpty()) {
            return
        }

        val polylineOptions =
            UIUtils.getPolylineOptions(requireContext(), polylineState.color, polylineState.width)
        polylineOptions.addAll(LocationUtils.mapLocationToLatLng(pathPoints))
        map?.addPolyline(polylineOptions)

        if (isFollowingDevice) {
            navigateToCurrentLocation(pathPoints.last())
        }
    }

    /**
     * Add the last point from the checkpoints list to the map.
     */
    private fun addLastCheckpoint() {
        if (!isTracking || checkpoints.isEmpty()) {
            return
        }

        val lastCheckpoint = checkpoints.last()
        val latLng = LatLng(lastCheckpoint.latitude, lastCheckpoint.longitude)
        val markerOptions = MarkerOptions().apply {
            position(latLng)
            icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_checkpoint_marker))
        }
        map?.addMarker(markerOptions)
    }

    /**
     * Add all the points from the checkpoints list to the map.
     */
    private fun addAllCheckpoints() =
        checkpoints.forEach {
            val latLng = LatLng(it.latitude, it.longitude)
            val markerOptions = MarkerOptions().apply {
                position(latLng)
                icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_checkpoint_marker))
            }
            map?.addMarker(markerOptions)
        }

    /**
     * Add a new waypoint on the map.
     *
     * If there was a waypoint previously then it gets replaced by the new one.
     */
    private fun addWaypoint() {
        if (!isTracking || waypoint == null) {
            return
        }

        if (waypointMarker != null) {
            waypointMarker!!.remove()
        }

        val latLng = LatLng(waypoint!!.latitude, waypoint!!.longitude)
        val markerOptions = MarkerOptions().apply {
            position(latLng)
            icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_waypoint_marker))
        }
        waypointMarker = map?.addMarker(markerOptions)
    }

    /**
     * Restore polyline, checkpoints and waypoint data.
     */
    private fun restoreMapData() {
        addAllPathPoints()
        addAllCheckpoints()
        addWaypoint()
    }

    /**
     * Navigate the camera to the current device location.
     */
    private fun navigateToCurrentLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, C.DEFAULT_MAP_ZOOM),
            C.DEFAULT_MAP_CAMERA_ANIMATION_DURATION,
            null
        )
    }

    /**
     * Reset the camera rotation to point to the north.
     */
    private fun resetCameraRotation() {
        map?.let { m ->
            val target = m.cameraPosition.target
            val zoom = m.cameraPosition.zoom
            val tilt = m.cameraPosition.tilt
            val bearing = 0.0f

            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(CameraPosition(target, zoom, tilt, bearing))
            )
        }
    }

    /**
     * Set the style of the session button depending on the [isTracking] state.
     */
    private fun setSessionButtonStyle() =
        with(binding) {
            val color: Int
            val icon: Int

            when (isTracking) {
                true -> {
                    color = R.color.red
                    icon = R.drawable.ic_stop
                }
                false -> {
                    color = R.color.secondary
                    icon = R.drawable.ic_start
                }
            }

            fabSessionStart.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), color)
            fabSessionStart.setImageResource(icon)
        }

    /**
     * Set the visibility of Add Checkpoint and Add Waypoint buttons.
     */
    private fun setLocationActionButtonsVisibility() {
        with(binding) {
            val visibility = when (isTracking) {
                true -> View.VISIBLE
                false -> View.GONE
            }

            fabAddCheckpoint.visibility = visibility
            fabAddWaypoint.visibility = visibility
        }
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
     * Start or stop listening to compass updates depending on the state.
     */
    private fun toggleCompass(compassState: CompassState) {
        with(binding) {
            isCompassEnabled = compassState.isEnabled
            fabToggleCompass.setImageResource(compassState.compassButtonIcon)
            imageViewCompass.clearAnimation()

            when (compassState.isEnabled) {
                true -> {
                    imageViewCompass.visibility = View.VISIBLE
                    compassListener.startListening()
                }
                false -> {
                    imageViewCompass.visibility = View.GONE
                    compassListener.stopListening()
                }
            }
        }
    }

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
}