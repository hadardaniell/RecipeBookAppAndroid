package com.example.recipebookappandorid.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemSharedInviteBinding
import com.example.recipebookappandorid.model.SharedBookInvite

class SharedInvitesAdapter(
    private val onAccept: (SharedBookInvite) -> Unit,
    private val onDecline: (SharedBookInvite) -> Unit
) : RecyclerView.Adapter<SharedInvitesAdapter.ViewHolder>() {

    private val invites = mutableListOf<SharedBookInvite>()

    inner class ViewHolder(val binding: ItemSharedInviteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSharedInviteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val invite = invites[position]
        holder.binding.tvInviteBook.text = invite.bookName
        holder.binding.tvInviteMeta.text =
            "Invited by ${invite.inviterName} as ${invite.role}"
        holder.binding.btnAcceptInvite.setOnClickListener { onAccept(invite) }
        holder.binding.btnDeclineInvite.setOnClickListener { onDecline(invite) }
    }

    override fun getItemCount(): Int = invites.size

    fun submitList(newInvites: List<SharedBookInvite>) {
        invites.clear()
        invites.addAll(newInvites)
        notifyDataSetChanged()
    }
}
