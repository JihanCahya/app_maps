package com.polinema.mi.app_maps.fragment

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ItemLaporanBinding
import java.text.SimpleDateFormat
import java.util.Locale

class LaporanAdapter(
    private var Laporan: MutableList<Laporan>,
    private val onLaporClick: (Laporan) -> Unit
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
                // Improved date formatting with null safety
                val inputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

                // Safely format date or use original if parsing fails
                tvTanggal.text = try {
                    inputDateFormat.parse(laporan.tanggal)?.let { outputDateFormat.format(it) }
                        ?: laporan.tanggal
                } catch (e: Exception) {
                    laporan.tanggal
                }

                // Set text with null safety
                tvPt.text = laporan.namaPt ?: "Tidak Diketahui"
                tvKubikasi.text = "Kubikasi: ${laporan.kubikasi ?: "-"}"
                tvRitase.text = "Ritase: ${laporan.ritase ?: "-"}"
                Log.d("LaporanStatus", "Status gambar: ${laporan.foto_surat_jalan ?: "Unknown"}")

                // Improved image loading
                Glide.with(itemView.context)
                    .load(laporan.foto_surat_jalan)
                    .placeholder(R.drawable.img) // Default placeholder
                    .error(R.drawable.img) // Default error image
                    .centerCrop() // Use centerCrop to fill the ImageView
                    .into(ivProductImage)

                // Logging with null-safe status check
                Log.d("LaporanStatus", "Status laporan: ${laporan.status ?: "Unknown"}")

                // Button state management
                val isReported = laporan.status == "1"
                btnLapor.apply {
                    if (isReported) {
                        setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.green))
                        text = "Sudah Dilaporkan"
                        isEnabled = false
                    } else {
                        setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.default_button_color))
                        text = "Laporkan"
                        isEnabled = true
                    }

                    // Set click listener
                    setOnClickListener { onLaporClick(laporan) }
                }
            }
        }
    }
}