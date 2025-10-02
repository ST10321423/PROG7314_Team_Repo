package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.example.prog7314_universe.R

class AddTransactionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contribution)

        // Set status bar color to match the design
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color)

        // Initialize UI elements
        val amountEditText = findViewById<EditText>(R.id.amountEditText)
        val goalSpinner = findViewById<Spinner>(R.id.spinnerGoals)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val addButton = findViewById<Button>(R.id.addButton)
        val backButton = findViewById<Button>(R.id.backButton)

        // Set initial values
        amountEditText.setText("R 550.00")
        amountEditText.setBackgroundColor(ContextCompat.getColor(this, R.color.amount_background))
        dateTextView.text = "21 August 2025"
        dateTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.date_background))

        // Set up spinner with goals
        val goals = arrayOf("Cape Town Trip", "Another Goal", "Third Goal")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        goalSpinner.adapter = adapter
        goalSpinner.setBackgroundColor(ContextCompat.getColor(this, R.color.goal_background))

        // Set button colors and styles
        addButton.setBackgroundColor(ContextCompat.getColor(this, R.color.button_background))
        backButton.setBackgroundColor(ContextCompat.getColor(this, R.color.button_background))


    }
}