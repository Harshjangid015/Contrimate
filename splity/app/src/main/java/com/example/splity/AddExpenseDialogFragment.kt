package com.example.splity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class AddExpenseDialogFragment : DialogFragment() {

    interface OnExpenseAddedListener {
        fun onExpenseAdded(description: String, amount: Double)
    }

    private var listener: OnExpenseAddedListener? = null

    fun setOnExpenseAddedListener(listener: OnExpenseAddedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_expense, container, false)

        val descriptionEditText: EditText = view.findViewById(R.id.editTextDescription)
        val amountEditText: EditText = view.findViewById(R.id.editTextAmount)
        val addButton: Button = view.findViewById(R.id.buttonAddExpense)

        addButton.setOnClickListener {
            val description = descriptionEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull()

            if (description.isNotEmpty() && amount != null) {
                listener?.onExpenseAdded(description, amount)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter valid details", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
