package com.capybara.hypericonlab.modules.icon.domain.repository

import com.capybara.hypericonlab.modules.icon.domain.model.InitializationPersistenceState
import kotlinx.coroutines.flow.Flow

interface InitializationStateRepository {
    val state: Flow<InitializationPersistenceState>

    suspend fun save(state: InitializationPersistenceState)

    suspend fun clear()
}
