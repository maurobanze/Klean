package com.maurobanze.klean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Demonstrates how to use Klean
 */
class ChatListViewModel(initialState: ChatListUiState) :
    UiViewModel<ChatListUiState, ChatListUiAction>(initialState) {

    private val vmScope = CoroutineScope(dispatcherProvider.default())

    override fun onActionReceived(action: ChatListUiAction) {
        when (action) {
            is ChatListUiAction.LoadChats -> executeLoadChats()
        }
    }

    override fun onReduce(currentState: ChatListUiState, change: Change): ChatListUiState {
        return when (change) {
            is ChatListChange.SuccessLoadingChats -> currentState.copy(
                chats = change.chats
            )
            else -> throwUnknownChange(change)
        }
    }

    private fun executeLoadChats() {
        vmScope.launch {
            //fake loading
            delay(2000)
            val chats = listOf("Amanda", "Mãe", "Stélio")

            //dispatch
            dispatchChange(ChatListChange.SuccessLoadingChats(chats))
        }
    }
}

sealed class ChatListUiAction : UiAction {

    object LoadChats : ChatListUiAction()
}

data class ChatListUiState(
    val chats: List<String> = listOf()
) : UiState

sealed class ChatListChange : Change {

    data class SuccessLoadingChats(val chats: List<String>) : ChatListChange()
    object ErrorLoadingChats : ChatListChange()
}