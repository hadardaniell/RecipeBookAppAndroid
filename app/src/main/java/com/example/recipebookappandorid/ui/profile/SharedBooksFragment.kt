package com.example.recipebookappandorid.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentSharedBooksBinding
import com.example.recipebookappandorid.model.SharedBookInvite
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.viewmodel.SharedBooksViewModel

class SharedBooksFragment : Fragment(R.layout.fragment_shared_books) {

    private var _binding: FragmentSharedBooksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedBooksViewModel by viewModels()
    private lateinit var booksAdapter: SharedBooksAdapter
    private lateinit var invitesAdapter: SharedInvitesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSharedBooksBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        booksAdapter = SharedBooksAdapter(::openBook)
        binding.rvSharedBooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSharedBooks.adapter = booksAdapter

        invitesAdapter = SharedInvitesAdapter(
            onAccept = { invite -> viewModel.acceptInvite(invite) },
            onDecline = { invite -> viewModel.declineInvite(invite) }
        )
        binding.rvInvites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvites.adapter = invitesAdapter

        binding.btnCreateSharedBook.setOnClickListener { showCreateBookDialog() }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.books.observe(viewLifecycleOwner) { books ->
            booksAdapter.submitList(books)
            binding.tvSharedBooksEmpty.visibility =
                if (books.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.invites.observe(viewLifecycleOwner) { invites ->
            invitesAdapter.submitList(invites)
            binding.tvInvitesEmpty.visibility =
                if (invites.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressSharedBooks.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnCreateSharedBook.isEnabled = !isLoading
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCreateBookDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * resources.displayMetrics.density).toInt())
        }

        val bookNameInput = EditText(context).apply { hint = "Book name" }
        val collaboratorsInput = EditText(context).apply {
            hint = "Emails, one per line or comma separated"
            minLines = 4
        }
        val roleSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                SharedBookRole.all
            )
            setSelection(SharedBookRole.all.indexOf(viewModel.defaultInviteRole()))
        }

        container.addView(bookNameInput)
        container.addView(collaboratorsInput)
        container.addView(roleSpinner)

        AlertDialog.Builder(context)
            .setTitle("Create Shared Book")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val selectedRole = roleSpinner.selectedItem?.toString().orEmpty()
                val invitations = collaboratorsInput.text.toString()
                    .split(",", "\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { it to selectedRole }
                viewModel.createBook(bookNameInput.text.toString(), invitations)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openBook(book: SharedRecipeBook) {
        val action = SharedBooksFragmentDirections.actionSharedBooksFragmentToSharedBookDetailsFragment(
            bookId = book.id,
            bookName = book.name
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
