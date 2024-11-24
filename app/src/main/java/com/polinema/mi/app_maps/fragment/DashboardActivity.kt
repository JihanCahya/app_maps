package com.polinema.mi.app_maps.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityDashboardBinding

class DashboardActivity : Fragment() {

    private lateinit var b : ActivityDashboardBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var laporanRef: DatabaseReference

    private lateinit var adapter: LaporanAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityDashboardBinding.inflate(layoutInflater)
        v = b.root

        b.btnTambahLaporan.setOnClickListener {
            val fragment = TambahLaporanActivity()
            val fragmentManager = parentFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.frameLayout, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        setupFirebase()
        setupRecyclerView()
        loadLaporan()

        return v
    }

    private fun setupFirebase() {
        auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://app-maps-91b91-default-rtdb.asia-southeast1.firebasedatabase.app/")
        laporanRef = database.getReference("laporan").child(uid)
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter(mutableListOf(),
            onEditClick = { laporan ->
                val fragment = EditLaporanActivity()
                val bundle = Bundle()
                bundle.putString("kodeLaporan", laporan.kodeLaporan)
                fragment.arguments = bundle
                val fragmentManager = parentFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.frameLayout, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            },
            onDeleteClick = { laporan ->
                AlertDialog.Builder(thisParent)
                    .setTitle("Konfirmasi")
                    .setMessage("Apakah Anda yakin ingin menghapus laporan ini?")
                    .setPositiveButton("Ya") { dialog, which ->
                        deleteLaporan(laporan)
                    }
                    .setNegativeButton("Tidak") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }
        )

        b.rvLaporan.layoutManager = LinearLayoutManager(requireContext())
        b.rvLaporan.adapter = adapter
    }

    private fun loadLaporan() {
        laporanRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val laporans = mutableListOf<Laporan>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val laporan = productSnapshot.getValue(Laporan::class.java)
                        laporan?.let {
                            Log.d("ProductFragment", """
                            Loaded product:
                            Nama PT: ${it.namaPt}
                        """.trimIndent())
                            laporans.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e("ProductFragment", "Error parsing product: ${productSnapshot.key}", e)
                    }
                }

                Log.d("ProductFragment", "Total products loaded: ${laporans.size}")
                adapter.updateProducts(laporans)

                b.rvLaporan.let { recyclerView ->
                    Log.d("ProductFragment", """
                    RecyclerView state:
                    Adapter attached: ${recyclerView.adapter != null}
                    Layout Manager attached: ${recyclerView.layoutManager != null}
                    Adapter item count: ${recyclerView.adapter?.itemCount}
                """.trimIndent())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductFragment", "Failed to load products: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load products: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteLaporan(laporan: Laporan) {
        laporanRef.child(laporan.kodeLaporan).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Berhasil menghapus produk", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}