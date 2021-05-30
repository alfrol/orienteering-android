package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.C
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.SettingsManager
import ee.taltech.alfrol.hw02.databinding.FragmentMenuBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.MenuViewModel
import ee.taltech.alfrol.hw02.utils.UIUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MenuFragment : Fragment(R.layout.fragment_menu) {

    @Inject
    lateinit var settingsManager: SettingsManager

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val menuViewModel: MenuViewModel by viewModels()

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
            val user = settingsManager.getValue(SettingsManager.LOGGED_IN_USER_ID_KEY, null).first()
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

        startObserving()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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