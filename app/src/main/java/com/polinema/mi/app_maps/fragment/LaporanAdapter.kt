package com.polinema.mi.app_maps.fragment

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ItemLaporanBinding
import java.text.SimpleDateFormat
import java.util.Locale

class LaporanAdapter(
    private var Laporan: MutableList<Laporan>,
    private val onEditClick: (Laporan) -> Unit,
    private val onDeleteClick: (Laporan) -> Unit
) : RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder>(){

    fun updateProducts(newlaporan: List<Laporan>) {
        Laporan.clear()
        Laporan.addAll(newlaporan)
        Log.d("LaporanAdapter", "Updating laporan: ${Laporan.size} items")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LaporanAdapter.LaporanViewHolder {
        val binding = ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LaporanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LaporanAdapter.LaporanViewHolder, position: Int) {
        val laporan = Laporan[position]
        Log.d("LaporanAdapter", "Binding laporan at position $position: ${laporan.namaPt}")
        holder.bind(laporan)
    }

    override fun getItemCount() = Laporan.size

    inner class LaporanViewHolder(private val binding: ItemLaporanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(laporan: Laporan) {
            binding.apply {
                val inputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

                try {
                    val date = inputDateFormat.parse(laporan.tanggal)
                    tvTanggal.text = date?.let { outputDateFormat.format(it) }
                } catch (e: Exception) {
                    tvTanggal.text = laporan.tanggal
                }

                tvPt.text = laporan.namaPt
                tvKubikasi.text = "Kubikasi: ${laporan.kubikasi}"
                tvRitase.text = "Ritase: ${laporan.ritase}"

                // Tambahkan Glide untuk memuat foto dari URL
                Glide.with(itemView.context)
                    .load(laporan.foto)
                    .placeholder(R.drawable.bg) // Tambahkan placeholder image
                    .error(R.drawable.bg) // Tambahkan error image jika gagal memuat
                    .into(ivProductImage)

                btnEdit.setOnClickListener { onEditClick(laporan) }
                btnDelete.setOnClickListener { onDeleteClick(laporan) }
            }
        }
    }
}