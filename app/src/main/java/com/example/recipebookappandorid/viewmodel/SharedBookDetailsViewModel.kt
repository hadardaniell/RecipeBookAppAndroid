package com.example.recipebookappandorid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipebookappandorid.model.Recipe
import com.example.recipebookappandorid.model.SharedBookMember
import com.example.recipebookappandorid.model.SharedBookRole
import com.example.recipebookappandorid.model.SharedRecipeBook
import com.example.recipebookappandorid.model.User
import com.example.recipebookappandorid.repository.AuthRepository
import com.example.recipebookappandorid.repository.RecipeRepository
import com.example.recipebookappandorid.repository.SharedRecipeBookRepository
import com.example.recipebookappandorid.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SharedBookDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val recipeRepository = RecipeRepository(application)
    private val sharedRecipeBookRepository = SharedRecipeBookRepository(application)
    private val userRepository = UserRepository(application)
    private val allRecipes = recipeRepository.getAllRecipes()
    private val allBooks = sharedRecipeBookRepository.getCachedBooks()
    private val selectedBookId = MutableLiveData<String>()

    private val _book = MutableLiveData<SharedRecipeBook?>()
    val book: LiveData<SharedRecipeBook?> = _book

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _shareableRecipes = MutableLiveData<List<Recipe>>(emptyList())
    val shareableRecipes: LiveData<List<Recipe>> = _shareableRecipes

    val recipes = MediatorLiveData<List<Recipe>>().apply {
        fun refresh() {
            val bookId = selectedBookId.value.orEmpty()
            value = allRecipes.value.orEmpty()
                .filter { recipe -> recipe.sharedBookId == bookId }
                .sortedByDescending { it.createdAt }
        }

        addSource(allRecipes) { refresh() }
        addSource(selectedBookId) { refresh() }
    }

    fun loadBook(bookId: String) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        selectedBookId.value = bookId
        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
                _book.postValue(sharedRecipeBookRepository.getBookById(bookId))
                refreshShareableRecipes(bookId)
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to load book")
            }
            _isLoading.postValue(false)
        }
    }

    fun inviteMember(email: String, role: String) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val bookId = selectedBookId.value.orEmpty()
        if (bookId.isBlank()) return

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                val inviter = currentUser(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.displayName.orEmpty())
                sharedRecipeBookRepository.inviteMember(bookId, inviter, email, role)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
                _book.postValue(sharedRecipeBookRepository.getBookById(bookId))
            }.onSuccess {
                _message.postValue("Invitation sent")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to invite member")
            }
            _isLoading.postValue(false)
        }
    }

    fun updateMemberRole(member: SharedBookMember, role: String) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val bookId = selectedBookId.value.orEmpty()
        if (bookId.isBlank()) return

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.updateMemberRole(bookId, member.userId, role)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
                _book.postValue(sharedRecipeBookRepository.getBookById(bookId))
                refreshRecipesRole(roleUpdatedMemberId = member.userId, newRole = role)
            }.onSuccess {
                _message.postValue("Role updated")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to update role")
            }
            _isLoading.postValue(false)
        }
    }

    fun removeMember(member: SharedBookMember) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val bookId = selectedBookId.value.orEmpty()
        if (bookId.isBlank()) return

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.removeMember(bookId, member.userId)
                removeMemberAccessFromRecipes(member.userId)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
                _book.postValue(sharedRecipeBookRepository.getBookById(bookId))
                refreshShareableRecipes(bookId)
            }.onSuccess {
                _message.postValue("Member removed")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to remove member")
            }
            _isLoading.postValue(false)
        }
    }

    fun leaveBook() {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val bookId = selectedBookId.value.orEmpty()
        if (bookId.isBlank()) return

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                sharedRecipeBookRepository.leaveBook(bookId, firebaseUser.uid)
                removeMemberAccessFromRecipes(firebaseUser.uid)
                sharedRecipeBookRepository.syncForUser(firebaseUser.uid, firebaseUser.email.orEmpty())
            }.onSuccess {
                _message.postValue("You left the book")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to leave the book")
            }
            _isLoading.postValue(false)
        }
    }

    fun shareExistingRecipe(recipe: Recipe) {
        val book = _book.value ?: return
        val firebaseUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                val role = book.roleFor(firebaseUser.uid).orEmpty()
                val sourceRecipeId = recipe.sourceRecipeId.ifBlank { recipe.id }
                if (recipeRepository.recipeExistsInBook(book.id, sourceRecipeId, recipe.title)) {
                    throw IllegalStateException("This recipe already appears in this book")
                }
                val copiedRecipe = recipe.copy(
                    id = UUID.randomUUID().toString(),
                    sourceRecipeId = sourceRecipeId,
                    sharedBookId = book.id,
                    sharedBookName = book.name,
                    sharedWithUserIds = book.memberIds,
                    sharedRole = role,
                    createdAt = System.currentTimeMillis()
                )
                recipeRepository.saveRecipe(copiedRecipe)
                refreshShareableRecipes(book.id)
            }.onSuccess {
                _message.postValue("Recipe shared to book")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to share recipe")
            }
            _isLoading.postValue(false)
        }
    }

    fun removeRecipeFromBook(recipe: Recipe) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val book = _book.value ?: return
        val role = book.roleFor(firebaseUser.uid).orEmpty()
        val canRemove = recipe.authorId == firebaseUser.uid ||
            role == SharedBookRole.OWNER ||
            role == SharedBookRole.EDITOR
        if (!canRemove) {
            _message.value = "You do not have permission to remove this recipe"
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            runCatching {
                recipeRepository.deleteRecipe(recipe.id)
                refreshShareableRecipes(book.id)
            }.onSuccess {
                _message.postValue("Recipe removed from book")
            }.onFailure { exception ->
                _message.postValue(exception.message ?: "Failed to remove recipe from book")
            }
            _isLoading.postValue(false)
        }
    }

    private suspend fun refreshShareableRecipes(bookId: String) {
        val firebaseUser = authRepository.getCurrentUser() ?: return
        val currentUserId = firebaseUser.uid
        _shareableRecipes.postValue(
            allRecipes.value.orEmpty()
                .filter { recipe ->
                    recipe.authorId == currentUserId && isPersonalRecipe(recipe)
                }
                .sortedByDescending { it.createdAt }
        )
    }

    private fun isPersonalRecipe(recipe: Recipe): Boolean {
        if (recipe.sharedBookId.isBlank()) return true
        return allBooks.value.orEmpty().any { it.id == recipe.sharedBookId && it.`private` }
    }

    private suspend fun removeMemberAccessFromRecipes(memberId: String) {
        val bookId = selectedBookId.value.orEmpty()
        if (bookId.isBlank()) return

        recipes.value.orEmpty().forEach { recipe ->
            val updated = recipe.copy(
                sharedWithUserIds = recipe.sharedWithUserIds.filterNot { it == memberId }
            )
            recipeRepository.updateRecipe(updated)
        }
    }

    private suspend fun refreshRecipesRole(roleUpdatedMemberId: String, newRole: String) {
        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId != roleUpdatedMemberId) return

        recipes.value.orEmpty().forEach { recipe ->
            recipeRepository.updateRecipe(recipe.copy(sharedRole = newRole))
        }
    }

    private suspend fun currentUser(uid: String, email: String, displayName: String): User {
        return userRepository.getUser(uid) ?: User(
            uid = uid,
            name = displayName,
            email = email,
            profileImageUrl = ""
        )
    }
}
