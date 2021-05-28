package ee.taltech.alfrol.hw02.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.adapters.SessionAdapter
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.databinding.FragmentHistoryBinding
import ee.taltech.alfrol.hw02.ui.viewmodels.SessionViewModel

@AndroidEntryPoint
class HistoryFragment : Fragment(), SessionAdapter.OnChildClickListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by viewModels()

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
            sessionAdapter = SessionAdapter(requireContext(), this@HistoryFragment)
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        sessionViewModel.sessionsSortedByRecordedAt.observe(viewLifecycleOwner, {
            sessionAdapter.submitList(it)
            binding.tvMissingItems.visibility = when (it.isEmpty()) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        })
    }

    override fun onClick(sessionId: Long) {
        val previewSessionAction = HistoryFragmentDirections.actionPreviewSession(
            isPreview = true,
            previewedSessionId = sessionId
        )
        findNavController().navigate(previewSessionAction)
    }

    override fun onRemove(session: Session) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_remove_session)
            .setMessage(R.string.text_session_removal_confirmation)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                sessionViewModel.deleteSession(session)
                sessionAdapter.notifyDataSetChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }
}