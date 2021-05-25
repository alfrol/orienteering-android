package ee.taltech.alfrol.hw02.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.databinding.FragmentSessionBinding
import ee.taltech.alfrol.hw02.ui.utils.CompassListener
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SessionFragment : Fragment(R.layout.fragment_session),
    OnMapReadyCallback,
    EasyPermissions.PermissionCallbacks,
    CompassListener.OnCompassUpdateCallback {

    companion object {
        private const val GLOBAL_LOCATION_REQUEST_CODE = 0
        private const val MY_LOCATION_REQUEST_CODE = 1
    }

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
        //val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        //mapFragment?.getMapAsync(this)
        with(binding) {
            map.onCreate(savedInstanceState)
            map.getMapAsync(this@SessionFragment)
        }

        // Instantiate the session data fragment as bottom sheet
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

        with(binding) {
            fabSessionStart.setOnClickListener {
                // Only request for permissions before session start
                if (!sessionViewModel.isSessionRunning() && !hasLocationPermission()) {
                    requestLocationPermission()
                } else if (!sessionViewModel.isSessionRunning()) {
                    sessionViewModel.startSession()
                } else {
                    sessionViewModel.stopSession()
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
                if (!hasLocationPermission()) {
                    requestLocationPermission(MY_LOCATION_REQUEST_CODE)
                }
            }
            fabResetMapView.setOnClickListener {

            }
            fabToggleCompass.setOnClickListener {
                sessionViewModel.enableDisableCompass()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()

        if (sessionViewModel.isCompassEnabled()) {
            compassListener.startListening()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set up the map (ui settings, enable my location etc)
        if (hasLocationPermission()) {
            map?.isMyLocationEnabled = true
            with(map?.uiSettings) {
                this?.isMyLocationButtonEnabled = false
                this?.isCompassEnabled = false
            }
        }

        map?.setOnMapClickListener {
            toggleSessionData()
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
            MY_LOCATION_REQUEST_CODE -> {

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.map.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()

        if (sessionViewModel.isCompassEnabled()) {
            compassListener.stopListening()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.map.onDestroy()
        _binding = null
    }

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

    private fun setSettingsAnimation(animation: Animation) {
        with(binding) {
            fabCenterMapView.startAnimation(animation)
            fabResetMapView.startAnimation(animation)
            fabToggleCompass.startAnimation(animation)
        }
    }

    /**
     * Check whether the app has location permissions.
     */
    private fun hasLocationPermission() =
        EasyPermissions.hasPermissions(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)

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
}