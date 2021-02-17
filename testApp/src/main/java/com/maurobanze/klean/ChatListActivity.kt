package com.maurobanze.klean

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatListActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatListViewModel
    private lateinit var recyclerViewVenues: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        findAndSetupViews()

        viewModel = ChatListViewModel(
            initialState = ChatListUiState()
        )

        observeUiState()
        viewModel.dispatchAction(ChatListUiAction.LoadChats)

    }
    private fun findAndSetupViews() {
        recyclerViewVenues = findViewById(R.id.recyclerViewChats)
        recyclerViewVenues.apply {
            adapter = ChatListAdapter(this@ChatListActivity)
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            setHasFixedSize(true)
        }
    }

    private fun observeUiState(){
        viewModel.uiStateAsFlow().asLiveData().observe(this) {
            renderState(it)
        }
    }

    private fun renderState(uiState: ChatListUiState) {

        val adapter = recyclerViewVenues.adapter as ChatListAdapter
        adapter.swapItems(uiState.chats)
    }
}