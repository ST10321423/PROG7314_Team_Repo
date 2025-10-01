package com.example.prog7314_universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.ContributionsAdapter
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivitySavingsGoalBinding
import com.example.prog7314_universe.viewmodel.SavingsContributionViewModel
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel

class SavingsGoalActivity : ComponentActivity() {

    private lateinit var b: ActivitySavingsGoalBinding
    private lateinit var vm: SavingsGoalViewModel
    private lateinit var contribVm: SavingsContributionViewModel
    private lateinit var adapter: ContributionsAdapter

    private var userId: String = ""
    private var goals: List<SavingsGoal> = emptyList()
    private var selectedGoalId: String = ""

    companion object {
        private const val KEY_USER = "USER_ID"
        fun start(ctx: Context, userId: String) {
            ctx.startActivity(Intent(ctx, SavingsGoalActivity::class.java).putExtra(KEY_USER, userId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySavingsGoalBinding.inflate(layoutInflater)
        setContentView(b.root)

        vm = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        contribVm = ViewModelProvider(this)[SavingsContributionViewModel::class.java]
        userId = intent.getStringExtra(KEY_USER) ?: ""
        if (userId.isBlank()) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        adapter = ContributionsAdapter(emptyList())
        b.contributionsRecyclerView.layoutManager = LinearLayoutManager(this)
        b.contributionsRecyclerView.adapter = adapter

        vm.getSavingsGoals(userId).observe(this) { list ->
            goals = list
            val names = list.map { it.goalName }
            b.goalTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }


            b.goalTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val g = goals[pos]
                    selectedGoalId = g.id
                    updateProgress(g)
                    loadContributions(g.id)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun updateProgress(goal: SavingsGoal) {

        contribVm.getContributions(userId, goal.id).observe(this) { list ->
            val total = list.sumOf { it.amount }
            val progress = if (goal.targetAmount > 0) ((total / goal.targetAmount) * 100).toInt() else 0
            b.txtAmount.text = "R${total.toInt()}"
            b.txtProgress.text = "You've saved $progress% of your goal!"
            b.progressCircle.setProgressCompat(progress, true)
        }
    }

    private fun loadContributions(goalId: String) {

        contribVm.getContributions(userId, goalId).observe(this) { contributions ->
            adapter.updateList(contributions)
        }
    }
}
