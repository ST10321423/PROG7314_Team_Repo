package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.prog7314_universe.R
import com.example.prog7314_universe.databinding.ActivityAddContributionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddContributionActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddContributionBinding
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddContributionBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Status bar color (optional; keep your color)
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color)

        // ------- Goals dropdown (Material AutoCompleteTextView) -------
        // TODO: replace with your ViewModel/Repo data. Using sample items for now:
        val goals = listOf("Cape Town Trip", "Another Goal", "Third Goal")
        b.actvGoal.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, goals))

        // ------- Date picker -------
        b.etDate.setOnClickListener { showDatePicker() }
        // default date (today)
        b.etDate.setText(formatDate(calendar.time))

        // ------- Save button -------
        b.btnAddContribution.setOnClickListener {
            val amountText = b.etAmount.text?.toString()?.trim().orEmpty()
            val amount = amountText.replace("R", "").trim().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                toast("Enter a valid amount")
                return@setOnClickListener
            }

            val goalName = b.actvGoal.text?.toString()?.trim().orEmpty()
            if (goalName.isEmpty()) {
                toast("Select a goal")
                return@setOnClickListener
            }

            val pickedDate = calendar.time // use Timestamp(pickedDate) if Firestore

            // TODO: Replace with your repo save call
            // e.g. contributionRepo.add(userId, goalId, amount, pickedDate) { success -> ... }
            toast("Contribution added: R${"%.2f".format(amount)} to \"$goalName\" on ${formatDate(pickedDate)}")
            finish()
        }

        // Optional back button if you add one to the layout:
        b.btnBack?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                b.etDate.setText(formatDate(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(date)

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
