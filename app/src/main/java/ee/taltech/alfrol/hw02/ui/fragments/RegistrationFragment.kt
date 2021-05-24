package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.databinding.FragmentRegistrationBinding
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import ee.taltech.alfrol.hw02.ui.viewmodels.RegistrationViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registrationViewModel.registrationFormState.observe(viewLifecycleOwner, { state ->
            if (state == null) {
                return@observe
            }
            binding.buttonRegister.isEnabled = state.isDataValid

            state.firstNameError?.let {
                binding.inputFirstName.error = getString(it)
            }
            state.lastNameError?.let {
                binding.inputLastName.error = getString(it)
            }
            state.emailError?.let {
                binding.inputEmail.error = getString(it)
            }
            state.passwordError?.let {
                binding.inputPassword.error = getString(it)
            }
            state.passwordConfirmationError?.let {
                binding.inputPasswordConfirmation.error = getString(it)
            }
        })
        registrationViewModel.registrationResult.observe(viewLifecycleOwner, { result ->
            if (result == null) {
                return@observe
            }
            binding.loadingSpinner.visibility = View.VISIBLE

            result.error?.let {
                UIUtils.showToast(requireContext(), it)
            }
            if (result.success) {
                val actionCompleteAuth = RegistrationFragmentDirections.actionCompleteAuth()
                findNavController().navigate(actionCompleteAuth)
            }
        })

        with(binding) {
            inputFirstName.addTextChangedListener(registrationDataChanged)
            inputLastName.addTextChangedListener(registrationDataChanged)
            inputEmail.addTextChangedListener(registrationDataChanged)
            inputPassword.addTextChangedListener(registrationDataChanged)
            inputPasswordConfirmation.addTextChangedListener(registrationDataChanged)

            buttonRegister.setOnClickListener {
                loadingSpinner.visibility = View.VISIBLE
                register()
            }
            inputPasswordConfirmation.setOnEditorActionListener { _, actionId, _ ->
                // Submit the registration request if the user submits the form via keyboard
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loadingSpinner.visibility = View.VISIBLE
                    register()
                }
                false
            }
            textViewLoginLink.setOnClickListener {
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Submit the registration details to the [RegistrationViewModel].
     */
    private fun register() {
        with(binding) {
            registrationViewModel.register(
                inputFirstName.text.toString(),
                inputLastName.text.toString(),
                inputEmail.text.toString(),
                inputPassword.text.toString()
            )
        }
    }

    /**
     * Forward the registration details to the [RegistrationViewModel] for validity check.
     */
    private val registrationDataChanged = UIUtils.getTextChangeListener {
        with(binding) {
            registrationViewModel.registrationDataChanged(
                inputFirstName.text.toString(),
                inputLastName.text.toString(),
                inputEmail.text.toString(),
                inputPassword.text.toString(),
                inputPasswordConfirmation.text.toString()
            )
        }
    }
}