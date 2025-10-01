package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivityCreateSavingsGoalBinding
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.Timestamp
import java.util.Calendar

class CreateSavingsGoalActivity : ComponentActivity() {

    private lateinit var b: ActivityCreateSavingsGoalBinding
    private lateinit var vm: SavingsGoalViewModel
    private var userId: String = ""
    private var selectedDeadline: Timestamp = Timestamp.now()

    companion object {
        private const val KEY_USER = "USER_ID"
        fun start(ctx: Context, userId: String) {
            ctx.startActivity(Intent(ctx, CreateSavingsGoalActivity::class.java).putExtra(KEY_USER, userId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateSavingsGoalBinding.inflate(layoutInflater)
        setContentView(b.root)

        vm = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        userId = intent.getStringExtra(KEY_USER) ?: ""
        if (userId.isBlank()) { Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show(); finish(); return }

        b.edtDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDeadline = Timestamp(cal.time)
                b.edtDate.setText("$d/${m+1}/$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnCreateGoal.setOnClickListener {
            val name = b.edtName.text.toString().trim()
            val target = b.edtAmount.text.toString().toDoubleOrNull()
            val dateTxt = b.edtDate.text.toString().trim()

            if (name.isBlank() || target == null || dateTxt.isBlank()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            val goal = SavingsGoal(
                userOwnerId = userId,
                goalName = name,
                targetAmount = target,
                deadline = selectedDeadline,
                createdAt = Timestamp.now()
            )
            vm.insertSavingsGoal(goal) { ok ->
                Toast.makeText(this, if (ok) "Goal created!" else "Failed to create goal", Toast.LENGTH_SHORT).show()
                if (ok) finish()
            }
        }
    }
}
