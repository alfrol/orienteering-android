package ee.taltech.alfrol.hw02.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.databinding.SessionPopupMenuBinding
import ee.taltech.alfrol.hw02.ui.states.CompassState
import ee.taltech.alfrol.hw02.ui.states.SessionState
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
    GoogleMap.OnMapLongClickListener,
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

    private var currentCompassAngle = 0.0f
    private var areSettingsOpen = false
    private var map: GoogleMap? = null
    private var points: MutableList<LatLng> = mutableListOf()
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

        sessionViewModel.sessionState.observe(viewLifecycleOwner, sessionStateObserve)
        sessionViewModel.locationState.observe(viewLifecycleOwner, locationStateObserver)
        sessionViewModel.currentLocation.observe(viewLifecycleOwner, currentLocationObserver)
        sessionViewModel.compassState.observe(viewLifecycleOwner, compassStateObserver)
        sessionViewModel.checkpoints.observe(viewLifecycleOwner, checkpointsObserver)
        sessionViewModel.waypoint.observe(viewLifecycleOwner, waypointObserver)
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
            GLOBAL_LOCATION_REQUEST_CODE -> sessionViewModel.startSession()
            MY_LOCATION_REQUEST_CODE -> sessionViewModel.getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.setOnMapClickListener(this)
        map?.setOnMapLongClickListener(this)

        if (UIUtils.hasLocationPermission(requireContext())) {
            map?.isMyLocationEnabled = true
            with(map?.uiSettings) {
                this?.isMyLocationButtonEnabled = false
                this?.isCompassEnabled = false
            }

            // When the map is ready try to focus on current device location
            binding.fabCenterMapView.performClick()
        }

        // Add all path points, checkpoints and a waypoint if the map was recreated
        addAllPoints()
        addAllCheckpoints()
        addWaypoint()
    }

    override fun onMapClick(p0: LatLng) {
        toggleSessionData()
    }

    override fun onMapLongClick(location: LatLng) {
        showPopup(location)
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
     * Observer for changes in session state.
     */
    private val sessionStateObserve = Observer<SessionState> { sessionState ->
        if (sessionState == null) {
            return@Observer
        }

        with(binding) {
            fabSessionStart.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), sessionState.buttonColor)
            fabSessionStart.setImageResource(sessionState.buttonIcon)
        }
    }

    /**
     * Observer for changes in compass state.
     */
    private val compassStateObserver = Observer<CompassState> { compassState ->
        if (compassState == null) {
            return@Observer
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
    }

    /**
     * Observer for changes in the location points.
     */
    private val locationStateObserver = Observer<MutableList<LatLng>> { locationState ->
        if (locationState == null) {
            return@Observer
        }

        if (sessionViewModel.isSessionRunning()) {
            points = locationState
            addLastPoint()
        }
    }

    /**
     * Observer for changes in the current device location.
     */
    private val currentLocationObserver = Observer<LatLng> { location ->
        if (location == null) {
            return@Observer
        }
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))
    }


    /**
     * Observer for changes in the checkpoints list.
     */
    private val checkpointsObserver = Observer<MutableList<LatLng>> { newCheckpoints ->
        if (newCheckpoints == null) {
            return@Observer
        }

        checkpoints = newCheckpoints
        addLastCheckpoint()
    }

    /**
     * Observer for changes in the waypoint.
     */
    private val waypointObserver = Observer<LatLng> { newWaypoint ->
        if (newWaypoint == null) {
            return@Observer
        }

        waypoint = newWaypoint
        addWaypoint()
    }

    /**
     * Listener for session start/stop button.
     */
    private val onClickSessionStart = View.OnClickListener {
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
            sessionViewModel.getCurrentLocation()
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
            }
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