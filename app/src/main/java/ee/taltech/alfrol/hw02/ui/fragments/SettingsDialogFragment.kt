package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.databinding.DialogFragmentSettingsBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.MenuViewModel
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel
import ee.taltech.alfrol.hw02.utils.UIUtils

@AndroidEntryPoint
class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentSettingsBinding? = null
    private val binding get() = _binding!!

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
        with(binding) {
            rbColorPrimary.setOnClickListener {
                menuViewModel.saveTrackColor(R.color.primary)
            }
            rbColorSecondary.setOnClickListener {
                menuViewModel.saveTrackColor(R.color.secondary)
            }
            rbColorGrey.setOnClickListener {
                menuViewModel.saveTrackColor(R.color.grey)
            }
            rbColorRed.setOnClickListener {
                menuViewModel.saveTrackColor(R.color.red)
            }

            etTrackWidth.addTextChangedListener(widthTextChangeListener)

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
                when (state.colorSlow) {
                    R.color.secondary -> rbColorSecondary.isChecked = true
                    R.color.red -> rbColorRed.isChecked = true
                    R.color.grey -> rbColorGrey.isChecked = true
                    else -> rbColorPrimary.isChecked = true
                }

                etTrackWidth.setText(state.width.toString())
            }
        })
        menuViewModel.trackWidthState.observe(viewLifecycleOwner, {
            val state = it ?: return@observe
            state.error?.let { err -> binding.etTrackWidth.error = getString(err) }
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
}