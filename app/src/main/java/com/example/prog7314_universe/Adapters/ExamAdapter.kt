package com.example.prog7314_universe.Adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.R
import com.example.prog7314_universe.Models.Exam

class ExamAdapter(
    private val context: Context,
    private val examList: MutableList<Exam>
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    inner class ExamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subject: TextView = itemView.findViewById(R.id.examSubject)
        val date: TextView = itemView.findViewById(R.id.examDate)
        val description: TextView = itemView.findViewById(R.id.examDescription)
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
        holder.subject.text = exam.subject
        holder.date.text = exam.date
        holder.description.text = exam.description

        holder.btnEdit.setOnClickListener { showEditDialog(exam, position) }

        holder.btnDelete.setOnClickListener {
            examList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, examList.size)
        }
    }

    override fun getItemCount() = examList.size

    fun addExam(exam: Exam) {
        examList.add(exam)
        notifyItemInserted(examList.size - 1)
    }

    private fun showEditDialog(exam: Exam, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_exam, null)
        val editSubject = dialogView.findViewById<EditText>(R.id.editExamSubject)
        val editDate = dialogView.findViewById<EditText>(R.id.editExamDate)
        val editDescription = dialogView.findViewById<EditText>(R.id.editExamDescription)

        editSubject.setText(exam.subject)
        editDate.setText(exam.date)
        editDescription.setText(exam.description)

        AlertDialog.Builder(context)
            .setTitle("Edit Exam")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedExam = exam.copy(
                    subject = editSubject.text.toString(),
                    date = editDate.text.toString(),
                    description = editDescription.text.toString()
                )
                examList[position] = updatedExam
                notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
