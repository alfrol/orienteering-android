package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var savedStateHandle: SavedStateHandle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        savedStateHandle = navController.previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(C.IS_USER_LOGGED_IN_KEY, false)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val isLoggedIn = settingsManager.loggedInUser.first() != null
            savedStateHandle.set(C.IS_USER_LOGGED_IN_KEY, isLoggedIn)

            if (isLoggedIn) {
                navController.popBackStack()
            }
        }

        with(binding) {
            buttonLogin.setOnClickListener {
                val loginAction = WelcomeFragmentDirections.actionLogin()
                findNavController().navigate(loginAction)
            }

            textViewRegistrationLink.setOnClickListener {
                val loginAction =
                    WelcomeFragmentDirections.actionLogin(jumpToRegistration = true)
                findNavController().navigate(loginAction)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}