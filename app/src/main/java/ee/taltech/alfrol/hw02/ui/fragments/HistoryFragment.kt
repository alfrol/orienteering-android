package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.adapters.SessionAdapter
import ee.taltech.alfrol.hw02.databinding.FragmentHistoryBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.HistoryViewModel

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels()

    private lateinit var sessionAdapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSessions.apply {
            sessionAdapter = SessionAdapter(requireContext())
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        historyViewModel.sessionsSortedByRecordedAt.observe(viewLifecycleOwner, {
            sessionAdapter.submitList(it)
        })
    }
}