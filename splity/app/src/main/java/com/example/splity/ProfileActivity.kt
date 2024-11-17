package com.example.splity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ProfileActivity : Fragment() {

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // Initialize views
        nameTextView = view.findViewById(R.id.textViewName)
        emailTextView = view.findViewById(R.id.textViewEmail)

        // Initialize Firebase references
        database = Firebase.database.reference
        auth = FirebaseAuth.getInstance()

        // Fetch user details from the database
        loadUserData()

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Reference to the current user's data in Firebase
            val userRef = database.child("users").child(userId)

            userRef.get().addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value as? String ?: "Name not found"
                val email = snapshot.child("email").value as? String ?: "Email not found"

                // Set the values to the TextViews
                nameTextView.text = name
                emailTextView.text = email
            }.addOnFailureListener {
                // Handle the error
                nameTextView.text = "Error loading name"
                emailTextView.text = "Error loading email"
            }
        }
    }
}
