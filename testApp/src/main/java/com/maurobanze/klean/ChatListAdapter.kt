package com.maurobanze.klean

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val context: Context
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private var chats: List<String> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.list_item_chat, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]

        holder.item = chat
        holder.textViewName.text = chat
    }

    override fun getItemCount(): Int = chats.size

    fun swapItems(newVenueList: List<String>) {
        if (newVenueList === this.chats)
            return

        val diffCallback = ChatListDiff(this.chats, newVenueList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.chats = newVenueList
        diffResult.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var item: String

        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    }
}

private class ChatListDiff(
    private val oldList: List<String>,
    private val newList: List<String>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}