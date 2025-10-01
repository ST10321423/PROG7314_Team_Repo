package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.SavingsContribution
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivityCreateSavingsContributionBinding
import com.example.prog7314_universe.viewmodel.SavingsContributionViewModel
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.Timestamp
import java.util.Calendar

class CreateSavingsContributionActivity : ComponentActivity() {

    private lateinit var b: ActivityCreateSavingsContributionBinding
    private lateinit var goalVm: SavingsGoalViewModel
    private lateinit var contribVm: SavingsContributionViewModel

    private var userId: String = ""
    private var goals: List<SavingsGoal> = emptyList()
    private var selectedGoalId: String = ""
    private var selectedDate: Timestamp = Timestamp.now()

    companion object {
        private const val KEY_USER = "USER_ID"
        fun start(ctx: Context, userId: String) {
            ctx.startActivity(
                Intent(ctx, CreateSavingsContributionActivity::class.java)
                    .putExtra(KEY_USER, userId)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateSavingsContributionBinding.inflate(layoutInflater)
        setContentView(b.root)

        goalVm = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        contribVm = ViewModelProvider(this)[SavingsContributionViewModel::class.java]

        userId = intent.getStringExtra(KEY_USER) ?: ""
        if (userId.isBlank()) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        // Load goals for spinner
        goalVm.getSavingsGoals(userId).observe(this) { list ->
            if (list.isNullOrEmpty()) {
                Toast.makeText(this, "No goals found. Create a goal first!", Toast.LENGTH_SHORT).show()
                finish(); return@observe
            }
            goals = list
            val names = list.map { it.goalName }
            b.goalTypeSpinner.adapter = ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                names
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // Use the regular listener (no custom extension)
            b.goalTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedGoalId = goals[position].id
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        // Date picker
        b.edtDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = Timestamp(cal.time)
                b.edtDate.setText("$d/${m + 1}/$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnAddContribution.setOnClickListener {
            val amount = b.edtAmount.text.toString().toDoubleOrNull()
            if (selectedGoalId.isBlank() || amount == null) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val c = SavingsContribution(
                goalId = selectedGoalId,
                amount = amount,
                contributionDate = selectedDate
            )
            contribVm.addContribution(userId, selectedGoalId, c) { ok ->
                Toast.makeText(this, if (ok) "Contribution added!" else "Failed to add", Toast.LENGTH_SHORT).show()
                if (ok) finish()
            }
        }
    }
}
