package com.example.prog7314_universe.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.R
import com.example.prog7314_universe.Models.Exam

class ExamAdapter(
    private var examList: List<Exam>,
    private val onEdit: (Exam) -> Unit,
    private val onDelete: (Exam) -> Unit,
    private val onToggleComplete: (Exam, Boolean) -> Unit
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    inner class ExamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.cbExamComplete)
        val subject: TextView = itemView.findViewById(R.id.tvExamSubject)
        val module: TextView = itemView.findViewById(R.id.tvExamModule)
        val date: TextView = itemView.findViewById(R.id.tvExamDate)
        val time: TextView = itemView.findViewById(R.id.tvExamTime)
        val description: TextView = itemView.findViewById(R.id.tvExamDescription)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditExam)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteExam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        val exam = examList[position]

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = exam.isCompleted

        holder.subject.text = exam.subject
        holder.module.text = exam.module
        holder.date.text = exam.date
        holder.time.text = if (exam.startTime.isNotEmpty() && exam.endTime.isNotEmpty()) {
            "${exam.startTime} - ${exam.endTime}"
        } else {
            "Time not set"
        }
        holder.description.text = exam.description.ifEmpty { "No description" }

        // Applies strikethrough if completed
        if (exam.isCompleted) {
            holder.subject.paintFlags = holder.subject.paintFlags or
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.subject.alpha = 0.5f
        } else {
            holder.subject.paintFlags = holder.subject.paintFlags and
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.subject.alpha = 1.0f
        }

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onToggleComplete(exam, isChecked)
        }

        holder.btnEdit.setOnClickListener {
            onEdit(exam)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(exam)
        }
    }

    override fun getItemCount() = examList.size

    fun updateList(newList: List<Exam>) {
        examList = newList
        notifyDataSetChanged()
    }
}
