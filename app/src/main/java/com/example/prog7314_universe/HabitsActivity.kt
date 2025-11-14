package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HabitsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirects to HabitListActivity
        startActivity(Intent(this, HabitListFragment::class.java))
        finish()
    }
}