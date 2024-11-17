package com.example.splity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter
    private val groups = mutableListOf<Group>()
    private lateinit var auth: FirebaseAuth
    private lateinit var emptyStateTextView: TextView

    companion object {
        private const val TAG = "GroupsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_groups, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewGroups)
        emptyStateTextView = view.findViewById(R.id.textViewEmptyState)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter(groups) { group ->
            val intent = Intent(activity, GroupDetailActivity::class.java)
            intent.putExtra("GROUP_ID", group.id)
            intent.putExtra("GROUP_NAME", group.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val buttonAddGroup: Button = view.findViewById(R.id.buttonAddGroup)
        val editTextGroupName: EditText = view.findViewById(R.id.editTextGroupName)

        buttonAddGroup.setOnClickListener {
            val groupName = editTextGroupName.text.toString()
            if (groupName.isNotEmpty()) {
                if (auth.currentUser != null) {
                    createGroup(groupName)
                } else {
                    Toast.makeText(context, "Please log in to create a group", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(activity, LoginActivity::class.java))
                }
            } else {
                Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
            }
        }

        loadGroups()  // Load the groups on startup

        return view
    }

    private fun createGroup(groupName: String) {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                val currentUserId = currentUser?.uid ?: throw Exception("User not logged in")

                // Create group with a map of members
                withContext(Dispatchers.IO) {
                    FirebaseManager.createGroup(groupName, listOf(currentUserId))
                }

                withContext(Dispatchers.Main) {
                    view?.findViewById<EditText>(R.id.editTextGroupName)?.text?.clear()
                    Toast.makeText(context, "Group created successfully", Toast.LENGTH_SHORT).show()
                    loadGroups()  // Reload groups after creating a new one
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating group: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadGroups() {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to view groups", Toast.LENGTH_LONG).show()
            startActivity(Intent(activity, LoginActivity::class.java))
            return
        }

        val currentUserId = auth.currentUser?.uid ?: return
        FirebaseManager.database.child("groups")
            .orderByChild("members/$currentUserId")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groups.clear()
                    for (groupSnapshot in snapshot.children) {
                        val group = groupSnapshot.getValue(Group::class.java)
                        group?.let { groups.add(it) }
                    }
                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading groups: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (groups.isEmpty()) {
                emptyStateTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.notifyDataSetChanged()
            }
        }
    }
}
