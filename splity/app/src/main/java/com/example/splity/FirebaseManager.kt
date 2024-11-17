package com.example.splity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

// Data class representing a User
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

// Data class representing a Group
data class Group(
    val id: String = "",
    val name: String = "",
    val members: Map<String, Boolean> = mapOf()  // Use Map for members
)

// Data class representing an Expense
data class Expense(
    val id: String = "",
    val groupId: String? = null,  // Make groupId nullable for individual expenses
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitBetween: List<String> = listOf(),
    val date: Long = 0 ,
    val settled: Boolean = false  // New field to track whether the expense is settled
)

object FirebaseManager {
    // Firebase database reference
    val database: DatabaseReference = Firebase.database("https://splity-f31e0e9f-default-rtdb.firebaseio.com/").reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Get the current user's ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Create a new user and save to Firebase
    suspend fun createUser(name: String, email: String, password: String): User {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Failed to create user")
        val user = User(userId, name, email)
        database.child("users").child(userId).setValue(user).await()
        return user
    }

    // Create a new group with members and save to Firebase
    suspend fun createGroup(name: String, members: List<String>): Group {
        val groupId = database.child("groups").push().key ?: throw Exception("Failed to generate group ID")
        val membersMap = members.associateWith { true }  // Convert list to map
        val group = Group(groupId, name, membersMap)
        database.child("groups").child(groupId).setValue(group).await()
        return group
    }

    // Add a new expense, groupId is nullable for individual expenses
    fun addExpense(groupId: String?, description: String, amount: Double, paidBy: String, members: List<String>) {
        val expenseId = database.child("expenses").push().key ?: return
        val expense = Expense(
            id = expenseId,
            groupId = groupId,
            description = description,
            amount = amount,
            paidBy = paidBy,
            splitBetween = members,
            date = Date().time
        )
        database.child("expenses").child(expenseId).setValue(expense)
    }

    // Get expenses without a group
    fun getExpensesWithoutGroup(): Query {
        return FirebaseDatabase.getInstance()
            .getReference("expenses")

    }

    // Get expenses for a specific group or individual expenses (null groupId)
    fun getGroupExpenses(groupId: String?): Query {
        return if (groupId == null) {
            getExpensesWithoutGroup()  // Call the helper method for expenses without group
        } else {
            database.child("expenses").orderByChild("groupId").equalTo(groupId)
        }
    }

    // Get all individual expenses (where groupId is null)
    fun getIndividualExpenses(): Query {
        return getExpensesWithoutGroup()
    }

    // Calculate balances for a group or individual expense and return via callback
    fun calculateBalances(groupId: String?, callback: (Map<String, Double>) -> Unit) {
        getGroupExpenses(groupId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balances = mutableMapOf<String, Double>()
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java) ?: continue
                    val splitAmount = expense.amount / expense.splitBetween.size
                    expense.splitBetween.forEach { userId ->
                        balances[userId] = (balances[userId] ?: 0.0) - splitAmount
                    }
                    balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount
                }
                callback(balances)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if necessary
            }
        })
    }

    // Mark an expense as settled in Firebase
    fun settleExpense(expenseId: String) {
        database.child("expenses").child(expenseId).child("settled").setValue(true)
    }
}
