package com.example.recipebookappandorid.ui.profile

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bumptech.glide.Glide
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentProfileBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.viewmodel.AuthViewModel
import com.example.recipebookappandorid.viewmodel.MyRecipesViewModel
import com.example.recipebookappandorid.viewmodel.ProfileViewModel
import com.example.recipebookappandorid.viewmodel.SharedBooksViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val myRecipesViewModel: MyRecipesViewModel by viewModels()
    private val sharedBooksViewModel: SharedBooksViewModel by viewModels()

    private lateinit var myRecipesAdapter: MyRecipesAdapter
    private lateinit var booksAdapter: SharedBooksAdapter
    private lateinit var invitesAdapter: SharedInvitesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        setupLists()
        setupTabs()

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }

        binding.btnCreateSharedBook.setOnClickListener { showCreateBookDialog() }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadCurrentUser()
        sharedBooksViewModel.sync()
    }

    private fun observeViewModel() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val fallbackName = user.email.substringBefore("@")
                    .replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                binding.tvName.text = user.name.ifBlank { fallbackName }
                binding.tvUsername.text = user.toUsername()

                Glide.with(this)
                    .load(user.profileImageUrl.ifBlank { null })
                    .placeholder(R.drawable.ic_profile_avatar_placeholder)
                    .fallback(R.drawable.ic_profile_avatar_placeholder)
                    .error(R.drawable.ic_profile_avatar_placeholder)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                binding.tvName.text = "Guest"
                binding.tvUsername.text = "@guest"
                binding.profileImage.setImageResource(R.drawable.ic_profile_avatar_placeholder)
            }
        }

        myRecipesViewModel.myRecipes.observe(viewLifecycleOwner) { recipes ->
            myRecipesAdapter.submitList(recipes)
            binding.tvMyRecipesEmpty.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }

        sharedBooksViewModel.books.observe(viewLifecycleOwner) { books ->
            booksAdapter.submitList(books)
            binding.tvSharedBooksEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
        }

        sharedBooksViewModel.invites.observe(viewLifecycleOwner) { invites ->
            invitesAdapter.submitList(invites)
            binding.tvInvitesEmpty.visibility = if (invites.isEmpty()) View.VISIBLE else View.GONE
        }

        sharedBooksViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressSharedBooks.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnCreateSharedBook.isEnabled = !isLoading
        }

        sharedBooksViewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupLists() {
        myRecipesAdapter = MyRecipesAdapter(::openRecipe)
        binding.rvMyRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyRecipes.adapter = myRecipesAdapter

        booksAdapter = SharedBooksAdapter(::openBook)
        binding.rvSharedBooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSharedBooks.adapter = booksAdapter

        invitesAdapter = SharedInvitesAdapter(
            onAccept = { invite -> sharedBooksViewModel.acceptInvite(invite) },
            onDecline = { invite -> sharedBooksViewModel.declineInvite(invite) }
        )
        binding.rvInvites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvites.adapter = invitesAdapter
    }

    private fun setupTabs() {
        showTab(position = 0)
        binding.profileTabs.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                showTab(tab.position)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) = Unit

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) = Unit
        })
    }

    private fun showTab(position: Int) {
        binding.layoutMyRecipesSection.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.layoutSharedSection.visibility = if (position == 1) View.VISIBLE else View.GONE
    }

    private fun performLogout() {
        authViewModel.logout()

        val rootNavController = androidx.navigation.Navigation.findNavController(
            requireActivity(),
            R.id.nav_host_fragment
        )

        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.mainContainerFragment, true)
            .build()

        rootNavController.navigate(R.id.loginFragment, null, navOptions)
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
            setSelection(SharedBookRole.all.indexOf(sharedBooksViewModel.defaultInviteRole()))
        }

        container.addView(bookNameInput)
        container.addView(collaboratorsInput)
        container.addView(roleSpinner)

        MaterialAlertDialogBuilder(context)
            .setTitle("Create Shared Book")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val selectedRole = roleSpinner.selectedItem?.toString().orEmpty()
                val invitations = collaboratorsInput.text.toString()
                    .split(",", "\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { it to selectedRole }
                sharedBooksViewModel.createBook(bookNameInput.text.toString(), invitations)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openRecipe(recipe: Recipe) {
        val action = ProfileFragmentDirections.actionProfileFragmentToRecipeDetailsFragment(
            id = recipe.id,
            description = recipe.description,
            imageUrl = recipe.imageUrl,
            title = recipe.title,
            authorId = recipe.authorId,
            authorName = recipe.authorName,
            prepTime = recipe.prepTime,
            difficulty = recipe.difficulty,
            category = recipe.category,
            ingredients = recipe.ingredients,
            steps = recipe.steps,
            notes = recipe.notes,
            sharedBookId = recipe.sharedBookId,
            sharedBookName = recipe.sharedBookName,
            sharedWithUserIds = recipe.sharedWithUserIds.toTypedArray(),
            sharedRole = recipe.sharedRole,
            isRemote = false
        )
        findNavController().navigate(action)
    }

    private fun openBook(book: SharedRecipeBook) {
        val action = ProfileFragmentDirections.actionProfileFragmentToSharedBookDetailsFragment(
            bookId = book.id,
            bookName = book.name
        )
        findNavController().navigate(action)
    }

    private fun com.example.recipebookappandorid.model.User.toUsername(): String {
        val handleSource = email.substringBefore("@").ifBlank { name }
        val normalized = handleSource
            .trim()
            .replace("\\s+".toRegex(), "_")
            .lowercase()
        return "@${normalized.ifBlank { "chef" }}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
