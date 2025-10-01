package com.example.prog7314_universe.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Models.SavingsContribution
import com.example.prog7314_universe.databinding.ItemContributionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ContributionsAdapter(
    private var items: List<SavingsContribution>
) : RecyclerView.Adapter<ContributionsAdapter.VH>() {

    inner class VH(val b: ItemContributionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemContributionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = items[position]
        val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(c.amount)
        h.b.contributionAmount.text = formattedAmount

        val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        h.b.contributionDate.text = formatter.format(c.contributionDate.toDate())
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<SavingsContribution>) {
        items = newList
        notifyDataSetChanged()
    }
}
