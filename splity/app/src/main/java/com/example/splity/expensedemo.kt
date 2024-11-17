package com.example.splity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseDemo : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private val expenses = mutableListOf<Expense>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_expensedemo, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ExpenseAdapter(expenses)
        recyclerView.adapter = adapter

        val buttonAddExpense: Button = view.findViewById(R.id.buttonAddExpense)
        val editTextDescription: EditText = view.findViewById(R.id.editTextDescription)
        val editTextAmount: EditText = view.findViewById(R.id.editTextAmount)
        val editTextPaidBy: EditText = view.findViewById(R.id.editTextPaidBy)  // Paid by input field
        val editTextSplitBetween: EditText = view.findViewById(R.id.editTextSplitBetween)  // Split between input field

        buttonAddExpense.setOnClickListener {
            addExpense(
                editTextDescription,
                editTextAmount,
                editTextPaidBy,
                editTextSplitBetween
            )
        }

        loadExpenses()

        return view
    }

    // Function to add an expense, using the new "Paid by" and "Split between" inputs
    private fun addExpense(
        editTextDescription: EditText,
        editTextAmount: EditText,
        editTextPaidBy: EditText,
        editTextSplitBetween: EditText
    ) {
        val description = editTextDescription.text.toString()
        val amount = editTextAmount.text.toString().toDoubleOrNull()
        val paidBy = editTextPaidBy.text.toString()
        val splitBetweenCount = editTextSplitBetween.text.toString().toIntOrNull()

        if (description.isNotEmpty() && amount != null && paidBy.isNotEmpty() && splitBetweenCount != null && splitBetweenCount > 0) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUserId = withContext(Dispatchers.IO) {
                        FirebaseManager.getCurrentUserId() ?: throw Exception("User not logged in")
                    }
                    val splitAmount = amount / splitBetweenCount
                    val members = List(splitBetweenCount) { currentUserId } // For now, using the current user ID for all members

                    withContext(Dispatchers.IO) {
                        FirebaseManager.addExpense(null, description, splitAmount, paidBy, members)
                    }

                    // Clear inputs after adding the expense
                    editTextDescription.text.clear()
                    editTextAmount.text.clear()
                    editTextPaidBy.text.clear()
                    editTextSplitBetween.text.clear()

                    Toast.makeText(context, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    loadExpenses()  // Reload the expenses
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to load expenses where groupId is null, sorted by time
    private fun loadExpenses() {
        FirebaseManager.getExpensesWithoutGroup().addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                expenses.clear()

                if (snapshot.exists()) {
                    Log.d("ExpenseDemo", "Expenses snapshot found.")
                    for (expenseSnapshot in snapshot.children) {
                        val expense = expenseSnapshot.getValue(Expense::class.java)
                        expense?.let {
                            expenses.add(it)
                            Log.d("ExpenseDemo", "Expense added: $it")
                        }
                    }

                    // Sort the expenses by time in descending order (latest first)
                    expenses.sortByDescending { it.date }
                    adapter.notifyDataSetChanged()
                } else {
                    Log.d("ExpenseDemo", "No expenses found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error loading expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
