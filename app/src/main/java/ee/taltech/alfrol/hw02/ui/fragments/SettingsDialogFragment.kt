package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.DialogFragmentSettingsBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.MenuViewModel
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import ee.taltech.alfrol.hw02.utils.UIUtils

@AndroidEntryPoint
class SettingsDialogFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private var _binding: DialogFragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArrayAdapter<CharSequence>

    private val menuViewModel: MenuViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.track_color_choice,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }.also {
            this.adapter = it
        }

        with(binding) {
            spinnerColorSlow.apply {
                this.adapter = adapter
                onItemSelectedListener = this@SettingsDialogFragment
            }
            spinnerColorNormal.apply {
                this.adapter = adapter
                onItemSelectedListener = this@SettingsDialogFragment
            }
            spinnerColorFast.apply {
                this.adapter = adapter
                onItemSelectedListener = this@SettingsDialogFragment
            }

            etTrackWidth.addTextChangedListener(widthTextChangeListener)
            rbgMapType.setOnCheckedChangeListener { _, id ->
                val mapType = when(id) {
                    R.id.rb_map_type_normal -> GoogleMap.MAP_TYPE_NORMAL
                    else -> GoogleMap.MAP_TYPE_SATELLITE
                }
                sessionViewModel.changeMapType(mapType)
            }

            btnLogout.setOnClickListener {
                menuViewModel.logout()
                updatePreviousBackstackEntry()
                findNavController().popBackStack()
            }
            btnDeleteAccount.setOnClickListener {
                menuViewModel.deleteAccount()
                updatePreviousBackstackEntry()
                findNavController().popBackStack()
            }
        }
        startObserving()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        with(binding) {
            val key = when (parent?.id) {
                spinnerColorSlow.id -> SettingsManager.POLYLINE_SLOW_COLOR_KEY
                spinnerColorNormal.id -> SettingsManager.POLYLINE_NORMAL_COLOR_KEY
                spinnerColorFast.id -> SettingsManager.POLYLINE_FAST_COLOR_KEY
                else -> null
            } ?: return
            val color = getSelectedColor(position)

            // Since we use the default spinner style the elements inside are MaterialTextViews
            (view as? MaterialTextView)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    color
                )
            )
            menuViewModel.saveTrackColor(key, color)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Ignore
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Start observing for changes in the settings.
     */
    private fun startObserving() {
        sessionViewModel.polylineState.observe(viewLifecycleOwner, {
            val state = it ?: return@observe
            with(binding) {
                spinnerColorSlow.setSelection(getSelectedColorPosition(state.colorSlow))
                spinnerColorNormal.setSelection(getSelectedColorPosition(state.colorNormal))
                spinnerColorFast.setSelection(getSelectedColorPosition(state.colorFast))

                etTrackWidth.setText(state.width.toString())
            }
        })
        menuViewModel.trackWidthState.observe(viewLifecycleOwner, {
            val state = it ?: return@observe
            state.error?.let { err -> binding.etTrackWidth.error = getString(err) }
        })
        sessionViewModel.mapType.observe(viewLifecycleOwner, {
            val type = it ?: return@observe
            val checked = when (type) {
                GoogleMap.MAP_TYPE_NORMAL -> R.id.rb_map_type_normal
                else -> R.id.rb_map_type_satellite
            }
            binding.rbgMapType.check(checked)
        })
    }

    /**
     * Update the value of user state in previous backstack entry.
     */
    private fun updatePreviousBackstackEntry() {
        findNavController().previousBackStackEntry!!.savedStateHandle.set(
            C.IS_USER_LOGGED_IN_KEY,
            false
        )
    }

    private val widthTextChangeListener = UIUtils.getTextChangeListener {
        menuViewModel.saveTrackWidth(binding.etTrackWidth.text.toString())
    }

    /**
     * Get the position of the color in the dropdown.
     *
     * @param color Color resource ID.
     */
    private fun getSelectedColorPosition(color: Int) =
        when (color) {
            R.color.primary_light -> adapter.getPosition("Light blue")
            R.color.primary -> adapter.getPosition("Blue")
            R.color.secondary -> adapter.getPosition("Dark blue")
            R.color.light_grey -> adapter.getPosition("Light grey")
            R.color.grey -> adapter.getPosition("Grey")
            R.color.dark_grey -> adapter.getPosition("Dark grey")
            R.color.red -> adapter.getPosition("Red")
            else -> 0
        }

    /**
     * Get the color from the position in the dropdown.
     *
     * @param position Position of the color in the dropdown.
     */
    private fun getSelectedColor(position: Int) =
        when (position) {
            adapter.getPosition("Light blue") -> R.color.primary_light
            adapter.getPosition("Dark blue") -> R.color.secondary
            adapter.getPosition("Light grey") -> R.color.light_grey
            adapter.getPosition("Grey") -> R.color.grey
            adapter.getPosition("Dark grey") -> R.color.dark_grey
            adapter.getPosition("Red") -> R.color.red
            else -> R.color.primary
        }
}