package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.SavingsContribution
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.R
import com.example.prog7314_universe.databinding.ActivityAddContributionBinding
import com.example.prog7314_universe.viewmodel.SavingsContributionViewModel
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddContributionActivity : AppCompatActivity() {

    private lateinit var b: ActivityAddContributionBinding
    private val calendar: Calendar = Calendar.getInstance()
    private lateinit var goalViewModel: SavingsGoalViewModel
    private lateinit var contributionViewModel: SavingsContributionViewModel

    private var userId: String = ""
    private var selectedGoalId: String = ""
    private var selectedDate: Timestamp = Timestamp(calendar.time)
    private var goals: List<SavingsGoal> = emptyList()

    companion object {
        private const val EXTRA_USER_ID = "USER_ID"
        private const val EXTRA_GOAL_ID = "GOAL_ID"

        fun start(context: Context, userId: String, goalId: String? = null) {
            context.startActivity(
                Intent(context, AddContributionActivity::class.java)
                    .putExtra(EXTRA_USER_ID, userId)
                    .apply { goalId?.let { putExtra(EXTRA_GOAL_ID, it) } }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddContributionBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Status bar color (optional; keep your color)
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color)

        goalViewModel = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        contributionViewModel = ViewModelProvider(this)[SavingsContributionViewModel::class.java]

        userId = intent.getStringExtra(EXTRA_USER_ID)
            ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            toast("Unable to determine user. Please sign in again.")
            finish()
            return
        }
        selectedGoalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: ""

        observeGoals()

        // ------- Date picker -------
        b.etContributionDate.setOnClickListener { showDatePicker() }
        // default date (today)
        selectedDate = Timestamp(calendar.time)
        b.etContributionDate.setText(formatDate(calendar.time))

        // ------- Save button -------
        b.btnAddContribution.setOnClickListener {
            val amountText = b.etContributionAmt.text?.toString()?.trim().orEmpty()
            val amount = amountText.replace("R", "").trim().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                toast("Enter a valid amount")
                return@setOnClickListener
            }

            if (selectedGoalId.isBlank()) {
                toast("Select a goal")
                return@setOnClickListener
            }

            val contribution = SavingsContribution(
                goalId = selectedGoalId,
                amount = amount,
                contributionDate = selectedDate
            )

            setLoading(true)
            contributionViewModel.addContribution(userId, selectedGoalId, contribution) { success ->
                setLoading(false)
                if (success) {
                    toast("Contribution saved")
                    finish()
                } else {
                    toast("Failed to save contribution")
                }
            }
        }


    }

    private fun observeGoals() {
        goalViewModel.getSavingsGoals(userId).observe(this) { list ->
            goals = list
            if (list.isNullOrEmpty()) {
                toast("No savings goals found. Create one first.")
                finish()
                return@observe
            }

            val names = list.map { it.goalName }
            b.spinnerContributionGoal.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, names))

            val initialIndex = list.indexOfFirst { it.id == selectedGoalId }
            val resolvedIndex = if (initialIndex >= 0) initialIndex else 0
            selectedGoalId = list.getOrNull(resolvedIndex)?.id.orEmpty()
            b.spinnerContributionGoal.setText(list.getOrNull(resolvedIndex)?.goalName.orEmpty(), false)

            b.spinnerContributionGoal.setOnItemClickListener { _, _, position, _ ->
                selectedGoalId = goals.getOrNull(position)?.id.orEmpty()
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val pickedDate = calendar.time
                selectedDate = Timestamp(pickedDate)
                b.etContributionDate.setText(formatDate(pickedDate))
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

    private fun setLoading(loading: Boolean) {
        b.btnAddContribution.isEnabled = !loading
        b.btnAddContribution.alpha = if (loading) 0.7f else 1f
    }
}
