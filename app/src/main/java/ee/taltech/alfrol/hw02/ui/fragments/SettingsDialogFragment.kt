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
import ee.taltech.alfrol.hw02.databinding.DialogFragmentSettingsBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.MenuViewModel

@AndroidEntryPoint
class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val menuViewModel: MenuViewModel by viewModels()

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}