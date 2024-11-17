package com.example.splity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for displaying groups in a RecyclerView
class GroupAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    // ViewHolder class for individual group items
    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNameTextView: TextView = itemView.findViewById(R.id.textViewGroupName)

        fun bind(group: Group) {
            groupNameTextView.text = group.name
            itemView.setOnClickListener { onGroupClick(group) } // Handle group click
        }
    }

    // Inflate the group item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false) // Replace with your item layout
        return GroupViewHolder(view)
    }

    // Bind the group data to the ViewHolder
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    // Return the total number of groups
    override fun getItemCount(): Int = groups.size
}
