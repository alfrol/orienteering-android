package ee.taltech.alfrol.hw02.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.alfrol.hw02.R
import ee.taltech.alfrol.hw02.data.model.Session
import ee.taltech.alfrol.hw02.databinding.HistoryItemBinding
import ee.taltech.alfrol.hw02.ui.utils.UIUtils

class SessionAdapter(
    private val context: Context,
    private val onChildClickListener: OnChildClickListener
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    /**
     * An interface for registering the clicks on the items.
     */
    interface OnChildClickListener {

        /**
         * Executed when a click is performed on the child item.
         *
         * @param sessionId ID of the session that is bound to this child.
         */
        fun onClick(sessionId: Long)

        /**
         * Executed when a remove button is clicked on the child.
         *
         * @param session Session object to remove.
         */
        fun onRemove(session: Session)
    }

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
            HistoryItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = differ.currentList[position]
        holder.bind(session)
        holder.itemView.setOnClickListener {
            onChildClickListener.onClick(session.id)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Session>) = differ.submitList(list)

    inner class SessionViewHolder(
        private val itemBinding: HistoryItemBinding
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(session: Session) {
            with(itemBinding) {
                tvName.text = session.name
                tvDate.text = session.recordedAtIsoShort
                tvDescription.text = session.description
                tvTotalDistance.text = UIUtils.formatDistance(context, session.distance)
                tvTotalDuration.text = UIUtils.formatDuration(context, session.duration)
                tvAveragePace.text = context.getString(R.string.pace, session.pace)

                ibRemoveSessions.setOnClickListener {
                    onChildClickListener.onRemove(session)
                }
            }
        }
    }
}