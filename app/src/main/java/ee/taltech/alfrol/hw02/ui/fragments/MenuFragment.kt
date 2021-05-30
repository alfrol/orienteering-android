package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentMenuBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.MenuViewModel
import ee.taltech.alfrol.hw02.utils.UIUtils
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : Fragment(R.layout.fragment_menu) {

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var backStackEntry: NavBackStackEntry

    private val menuViewModel: MenuViewModel by viewModels()

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

        backStackEntry = findNavController().getBackStackEntry(R.id.menuFragment)
        backStackEntry.lifecycle.addObserver(backstackEntryLifecycleObserver)

        // Listen to the current view's lifecycle events and remove the observer when destroyed.
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                backStackEntry.lifecycle.removeObserver(backstackEntryLifecycleObserver)
            }
        })

        with(binding) {
            settingsButton.setOnClickListener {
                val actionOpenSettings = MenuFragmentDirections.actionOpenSettings()
                findNavController().navigate(actionOpenSettings)
            }
            newSessionButtonLayout.setOnClickListener {
                val actionStartSession = MenuFragmentDirections.actionStartSession()
                findNavController().navigate(actionStartSession)
            }
            sessionsButtonLayout.setOnClickListener {
                val viewHistoryAction = MenuFragmentDirections.actionViewHistory()
                findNavController().navigate(viewHistoryAction)
            }
        }

        startObserving()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Observer for the backstack entry (current fragment) lifecycle event.s
     * Whenever the event is [Lifecycle.Event.ON_RESUME] we want to check
     * whether the user is still logged in.
     */
    private val backstackEntryLifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            val savedStateHandle = backStackEntry.savedStateHandle
            val isUserLoggedIn = savedStateHandle.get<Boolean>(C.IS_USER_LOGGED_IN_KEY)

            if (isUserLoggedIn != true) {
                val authAction = MenuFragmentDirections.actionAuthenticate()
                findNavController().navigate(authAction)
            }
        }
    }

    /**
     * Start observing the general statistics.
     */
    private fun startObserving() {
        menuViewModel.sessionsCount.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarTotalSessions.visibility = View.GONE
                tvTotalSessions.text = it.toString()
            }
        })
        menuViewModel.totalDistance.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarTotalDistance.visibility = View.GONE
                tvTotalDistance.text = UIUtils.formatDistance(requireContext(), it ?: 0.0f)
            }
        })
        menuViewModel.checkpointCount.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarTotalCheckpoints.visibility = View.GONE
                tvTotalCheckpoints.text = it.toString()
            }
        })
        menuViewModel.averageDistance.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarAverageDistance.visibility = View.GONE
                tvAverageDistance.text = UIUtils.formatDistance(requireContext(), it ?: 0.0f)
            }
        })
        menuViewModel.averageDuration.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarAverageDuration.visibility = View.GONE
                tvAverageDuration.text = UIUtils.formatDuration(requireContext(), it ?: 0, false)
            }
        })
        menuViewModel.averagePace.observe(viewLifecycleOwner, {
            with(binding) {
                progressBarAveragePace.visibility = View.GONE
                tvAveragePace.text = getString(R.string.pace, it ?: 0.0f)
            }
        })
    }
}