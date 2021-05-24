package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.databinding.FragmentLoginBinding
import ee.taltech.alfrol.hw02.ui.utils.UIUtils
import ee.taltech.alfrol.hw02.ui.viewmodels.LoginViewModel


@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val args: LoginFragmentArgs by navArgs()
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Go directly to the registration screen if necessary
        if (args.jumpToRegistration) {
            val actionRegister = LoginFragmentDirections.actionRegister()
            findNavController().navigate(actionRegister)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel.loginFormState.observe(viewLifecycleOwner, { state ->
            if (state == null) {
                return@observe
            }
            binding.buttonLogin.isEnabled = state.isDataValid

            state.emailError?.let {
                binding.inputEmail.error = getString(it)
            }
            state.passwordError?.let {
                binding.inputPassword.error = getString(it)
            }
        })
        loginViewModel.loginResult.observe(viewLifecycleOwner, { result ->
            if (result == null) {
                return@observe
            }
            binding.loadingSpinner.visibility = View.GONE

            result.error?.let {
                UIUtils.showToast(requireContext(), it)
            }
            if (result.success) {
                findNavController().popBackStack()
            }
        })

        with(binding) {
            inputEmail.addTextChangedListener(loginDataChanged)
            inputPassword.addTextChangedListener(loginDataChanged)

            buttonLogin.setOnClickListener {
                loadingSpinner.visibility = View.VISIBLE
                login()
            }
            inputPassword.setOnEditorActionListener { _, actionId, _ ->
                // Submit the login request if the user submits the form via keyboard
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loadingSpinner.visibility = View.VISIBLE
                    login()
                }
                false
            }
            textViewRegistrationLink.setOnClickListener {
                val actionRegister = LoginFragmentDirections.actionRegister()
                findNavController().navigate(actionRegister)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Submit the login details to the [LoginViewModel].
     */
    private fun login() {
        with(binding) {
            loginViewModel.login(inputEmail.text.toString(), inputPassword.text.toString())
        }
    }

    /**
     * Forward the login details to the [LoginViewModel] for validity check.
     */
    private val loginDataChanged = UIUtils.getTextChangeListener {
        with(binding) {
            loginViewModel.loginDataChanged(
                inputEmail.text.toString(),
                inputPassword.text.toString()
            )
        }
    }
}