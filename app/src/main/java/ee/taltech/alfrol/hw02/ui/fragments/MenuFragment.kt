package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentMenuBinding
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : Fragment(R.layout.fragment_menu) {

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = findNavController()

        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle

        savedStateHandle.getLiveData<Boolean>(C.IS_USER_LOGGED_IN_KEY)
            .observe(currentBackStackEntry, { isLoggedIn ->
                if (!isLoggedIn) {
                    val actionAuthenticate = MenuFragmentDirections.actionAuthenticate()
                    findNavController().navigate(actionAuthenticate)
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val user = settingsManager.loggedInUser.first()
            if (user == null) {
                val actionAuthenticate = MenuFragmentDirections.actionAuthenticate()
                findNavController().navigate(actionAuthenticate)
            }
        }

        with(binding) {
            newSessionButtonLayout.setOnClickListener {
                val actionStartSession = MenuFragmentDirections.actionStartSession()
                findNavController().navigate(actionStartSession)
            }
            sessionsButtonLayout.setOnClickListener {
                val viewHistoryAction = MenuFragmentDirections.actionViewHistory()
                findNavController().navigate(viewHistoryAction)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}