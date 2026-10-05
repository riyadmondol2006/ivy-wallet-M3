package com.ivy.data

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.sync.UniqueId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataObserver @Inject constructor() {
    /**
     * Buffered so a slow collector (e.g. auto-sync uploading a backup) never blocks the
     * repository write that posted the event. Collectors that fall behind see the newest events.
     */
    private val _writeEvents = MutableSharedFlow<DataWriteEvent>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val writeEvents: Flow<DataWriteEvent> = _writeEvents

    suspend fun post(event: DataWriteEvent) {
        _writeEvents.emit(event)
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 64
    }
}

sealed interface DataWriteEvent {
    data object AllDataChange : AccountChange, CategoryChange

    sealed interface AccountChange : DataWriteEvent
    data class SaveAccounts(val accounts: List<Account>) : AccountChange
    data class DeleteAccounts(val operation: DeleteOperation<AccountId>) : AccountChange

    sealed interface CategoryChange : DataWriteEvent
    data class SaveCategories(val categories: List<Category>) : CategoryChange
    data class DeleteCategories(val operation: DeleteOperation<CategoryId>) : CategoryChange

    sealed interface TagChange : DataWriteEvent
    data class SaveTags(val tags: List<Tag>) : TagChange
    data class DeleteTags(val operation: DeleteOperation<TagId>) : TagChange
}

sealed interface DeleteOperation<out Id : UniqueId> {
    data object All : DeleteOperation<Nothing>
    data class Just<Id : UniqueId>(val ids: List<Id>) : DeleteOperation<Id>
}
