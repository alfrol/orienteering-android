package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.ui.utils.CompassListener

@AndroidEntryPoint
class SessionFragment : Fragment(R.layout.fragment_session),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnCameraMoveStartedListener,
    CompassListener.OnCompassUpdateCallback {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

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

    private var isViewMode = false
    private var isFollowingDevice = true
    private var currentCompassAngle = 0.0f
    private var areSettingsOpen = false
    private var map: GoogleMap? = null

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
            fabCenterMapView.setOnClickListener(onClickCenterView)
            fabResetMapView.setOnClickListener(onClickResetMapView)
            fabToggleCompass.setOnClickListener(onClickToggleCompass)
            fabAddCheckpoint.setOnClickListener(onClickAddCheckpoint)
            fabAddWaypoint.setOnClickListener(onClickAddWaypoint)

            // Instantiate the session data as bottom sheet
            bottomSheetBehavior = BottomSheetBehavior.from(sessionData)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Only listen to map events in session mode
        if (!isViewMode) {
            map?.setOnMapClickListener(this)
            map?.setOnCameraMoveStartedListener(this)
        }

        with(map?.uiSettings) {
            this?.isMyLocationButtonEnabled = false
            this?.isCompassEnabled = false
        }
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
     * Listener for session start/stop button.
     */
    private val onClickSessionStart = View.OnClickListener {

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

    }

    /**
     * Listener for reset map view button.
     */
    private val onClickResetMapView = View.OnClickListener {
    }

    /**
     * Listener for compass button.
     */
    private val onClickToggleCompass = View.OnClickListener {
    }

    /**
     * Listener for checkpoint adding button.
     */
    private val onClickAddCheckpoint = View.OnClickListener {

    }

    /**
     * Listener for waypoint adding button.
     */
    private val onClickAddWaypoint = View.OnClickListener {

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