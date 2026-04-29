package com.example.recipebookappandorid.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipebookappandorid.databinding.ItemSharedBookBinding
import com.example.recipebookappandorid.model.SharedRecipeBook

class SharedBooksAdapter(
    private val onBookClick: (SharedRecipeBook) -> Unit
) : RecyclerView.Adapter<SharedBooksAdapter.ViewHolder>() {

    private val books = mutableListOf<SharedRecipeBook>()

    inner class ViewHolder(val binding: ItemSharedBookBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSharedBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.binding.tvBookName.text = book.name
        holder.binding.tvBookOwner.text = "Owner: ${book.ownerName.toUsernameLike()}"
        holder.binding.tvBookMembers.text = "${book.memberIds.size} members"
        holder.itemView.setOnClickListener { onBookClick(book) }
    }

    override fun getItemCount(): Int = books.size

    fun submitList(newBooks: List<SharedRecipeBook>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()
    }

    private fun String.toUsernameLike(): String {
        val source = substringBefore("@").trim()
        return source.ifBlank { trim() }
    }
}
