package com.example.recipebookappandorid.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.recipebookappandorid.R
import com.example.recipebookappandorid.databinding.FragmentSharedBookDetailsBinding
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.SharedBookMember
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.viewmodel.SharedBookDetailsViewModel

class SharedBookDetailsFragment : Fragment(R.layout.fragment_shared_book_details) {

    private var _binding: FragmentSharedBookDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedBookDetailsViewModel by viewModels()
    private lateinit var recipesAdapter: MyRecipesAdapter
    private lateinit var membersAdapter: SharedMembersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSharedBookDetailsBinding.bind(view)

        val args = SharedBookDetailsFragmentArgs.fromBundle(requireArguments())

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        recipesAdapter = MyRecipesAdapter(::openRecipe, ::confirmRemoveRecipe)
        binding.rvSharedBookRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSharedBookRecipes.adapter = recipesAdapter

        membersAdapter = SharedMembersAdapter(::showManageMemberDialog)
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembers.adapter = membersAdapter

        binding.btnAddSharedRecipe.setOnClickListener {
            val action =
                SharedBookDetailsFragmentDirections.actionSharedBookDetailsFragmentToAddRecipeFragment(
                    sharedBookId = args.bookId,
                    sharedBookName = args.bookName
                )
            findNavController().navigate(action)
        }

        binding.btnShareExistingRecipe.setOnClickListener { showShareExistingDialog() }
        binding.btnInviteMember.setOnClickListener { showInviteDialog() }
        binding.btnLeaveBook.setOnClickListener { confirmLeaveBook() }

        observeViewModel()
        viewModel.loadBook(args.bookId)
    }

    private fun observeViewModel() {
        viewModel.book.observe(viewLifecycleOwner) { book ->
            if (book != null) {
                binding.tvSharedBookName.text = book.name
                binding.tvSharedBookMeta.text =
                    "Owner: ${book.ownerName.toUsernameLike()} · Members: ${book.memberNames.joinToString(", ")}"

                val currentUserId =
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val currentRole = book.roleFor(currentUserId).orEmpty()
                val canContribute = currentRole == SharedBookRole.OWNER || currentRole == SharedBookRole.EDITOR
                val canManage = currentRole == SharedBookRole.OWNER

                binding.btnAddSharedRecipe.visibility = if (canContribute) View.VISIBLE else View.GONE
                binding.btnShareExistingRecipe.visibility = if (canContribute) View.VISIBLE else View.GONE
                binding.btnInviteMember.visibility = if (canContribute) View.VISIBLE else View.GONE
                binding.btnLeaveBook.visibility =
                    if (currentUserId.isNotBlank() && currentUserId != book.ownerId) View.VISIBLE else View.GONE

                recipesAdapter.setRemovalEnabled(canContribute)
                membersAdapter.submitList(book.members(), canManage)
            }
        }

        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipesAdapter.submitList(recipes)
            binding.tvSharedBookEmpty.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.shareableRecipes.observe(viewLifecycleOwner) {
            binding.btnShareExistingRecipe.isEnabled = it.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressSharedBook.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                if (message == "You left the book") {
                    findNavController().navigateUp()
                } else {
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showInviteDialog() {
        val context = requireContext()
        val emailInput = EditText(context).apply { hint = "Collaborator email" }
        val roleSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(SharedBookRole.EDITOR, SharedBookRole.VIEWER)
            )
        }

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(emailInput)
            addView(roleSpinner)
        }

        AlertDialog.Builder(context)
            .setTitle("Invite Member")
            .setView(container)
            .setPositiveButton("Send") { _, _ ->
                viewModel.inviteMember(
                    email = emailInput.text.toString(),
                    role = roleSpinner.selectedItem?.toString().orEmpty()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageMemberDialog(member: SharedBookMember) {
        val context = requireContext()
        val roles = listOf(SharedBookRole.EDITOR, SharedBookRole.VIEWER)
        val roleSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                roles
            )
            setSelection(roles.indexOf(member.role).coerceAtLeast(0))
        }

        AlertDialog.Builder(context)
            .setTitle(member.name)
            .setView(roleSpinner)
            .setPositiveButton("Save Role") { _, _ ->
                viewModel.updateMemberRole(member, roleSpinner.selectedItem?.toString().orEmpty())
            }
            .setNeutralButton("Remove") { _, _ ->
                viewModel.removeMember(member)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showShareExistingDialog() {
        val recipes = viewModel.shareableRecipes.value.orEmpty()
        if (recipes.isEmpty()) {
            Snackbar.make(binding.root, "No personal recipes available to share", Snackbar.LENGTH_LONG)
                .show()
            return
        }

        val labels = recipes.map { recipe ->
            recipe.title.ifBlank { "Untitled recipe" }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Share Existing Recipe")
            .setItems(labels) { _, which ->
                viewModel.shareExistingRecipe(recipes[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLeaveBook() {
        AlertDialog.Builder(requireContext())
            .setTitle("Leave shared book?")
            .setMessage("You will lose access to recipes in this book.")
            .setPositiveButton("Leave") { _, _ ->
                viewModel.leaveBook()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmRemoveRecipe(recipe: Recipe) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove recipe from book?")
            .setMessage("This will remove the recipe from this recipe book.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeRecipeFromBook(recipe)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openRecipe(recipe: Recipe) {
        val action =
            SharedBookDetailsFragmentDirections.actionSharedBookDetailsFragmentToRecipeDetailsFragment(
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun String.toUsernameLike(): String {
        val source = substringBefore("@").trim()
        return source.ifBlank { trim() }
    }
}
