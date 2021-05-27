package ee.taltech.alfrol.hw02.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.databinding.RvItemBinding
import ee.taltech.alfrol.hw02.ui.utils.UIUtils

class SessionAdapter(
    private val context: Context
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        return SessionViewHolder(
            RvItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = differ.currentList[position]
        holder.bind(session)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Session>) = differ.submitList(list)

    inner class SessionViewHolder(private val itemBinding: RvItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(session: Session) {
            itemBinding.tvName.text = session.name
            itemBinding.tvDate.text = session.recordedAtIsoShort
            itemBinding.tvDescription.text = session.description
            itemBinding.tvTotalDistance.text = UIUtils.formatDistance(context, session.distance)
            itemBinding.tvTotalDuration.text = UIUtils.formatDuration(context, session.duration)
            itemBinding.tvAveragePace.text = context.getString(R.string.pace, session.pace)
        }
    }
}