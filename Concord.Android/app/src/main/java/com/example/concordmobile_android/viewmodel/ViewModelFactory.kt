package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.concordmobile_android.data.repository.ConcordRepository

class ConcordViewModelFactory(
    private val repository: ConcordRepository,
    private val friendId: Int? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository) as T
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(repository, friendId ?: 0) as T
            modelClass.isAssignableFrom(ContactsViewModel::class.java) -> ContactsViewModel(repository) as T
            modelClass.isAssignableFrom(RequestsViewModel::class.java) -> RequestsViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository, friendId ?: 0) as T
            else -> throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
        }
    }
}
