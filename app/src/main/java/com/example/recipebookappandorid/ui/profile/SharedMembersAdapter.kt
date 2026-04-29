package com.example.recipebookappandorid.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemSharedMemberBinding
import com.example.recipebookappandorid.model.SharedBookMember

class SharedMembersAdapter(
    private val onManageMember: (SharedBookMember) -> Unit
) : RecyclerView.Adapter<SharedMembersAdapter.ViewHolder>() {

    private val members = mutableListOf<SharedBookMember>()
    private var canManageMembers: Boolean = false

    inner class ViewHolder(val binding: ItemSharedMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSharedMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        holder.binding.tvMemberName.text = member.name
        holder.binding.tvMemberMeta.text = "${member.email} · ${member.role}"
        holder.binding.btnManageMember.visibility =
            if (canManageMembers) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.btnManageMember.setOnClickListener { onManageMember(member) }
    }

    override fun getItemCount(): Int = members.size

    fun submitList(newMembers: List<SharedBookMember>, canManageMembers: Boolean) {
        members.clear()
        members.addAll(newMembers)
        this.canManageMembers = canManageMembers
        notifyDataSetChanged()
    }
}
