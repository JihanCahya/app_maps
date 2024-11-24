package com.polinema.mi.app_maps.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R

import com.polinema.mi.app_maps.databinding.ActivityEditLaporanBinding
import java.util.Calendar

class EditLaporanActivity : Fragment(), View.OnClickListener {

    private lateinit var b : ActivityEditLaporanBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    private var kodeLaporan = ""
    val categories = listOf("PT. GARUDA", "PT. ELANG")

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var laporanRef: DatabaseReference
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityEditLaporanBinding.inflate(layoutInflater)
        v = b.root

        kodeLaporan = arguments?.getString("kodeLaporan").toString()
        b.btnKembali2.setOnClickListener(this)
        b.btnPilihFoto2.setOnClickListener(this)
        b.btnDatePicker2.setOnClickListener(this)
        b.btnUpdateData.setOnClickListener(this)

        setupFirebase()
        setupPtSpinner()
        getLaporanDataId(kodeLaporan)

        return v
    }

    private fun setupPtSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spNamaPt2.adapter = adapter
    }

    private fun setupFirebase() {
        auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://app-maps-91b91-default-rtdb.asia-southeast1.firebasedatabase.app/")
        laporanRef = database.getReference("laporan").child(uid)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnKembali2 -> {
                val fragment = DashboardActivity()
                val fragmentManager = parentFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.frameLayout, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }
            R.id.btnPilihFoto2 -> {
                openImageChooser()
            }
            R.id.btnDatePicker2 -> {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(
                    thisParent,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val selectedDate = "$selectedDay-${selectedMonth + 1}-$selectedYear"
                        b.inputTanggal2.setText(selectedDate)
                    },
                    year, month, dayOfMonth
                )

                datePickerDialog.show()
            }
            R.id.btnUpdateData -> {
                updateLaporanToFirebase(kodeLaporan)
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            b.imvFoto2.setImageURI(imageUri)
        }
    }

    private fun getLaporanDataId(kode: String) {
        laporanRef.child(kode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val laporanData = snapshot.getValue(Laporan::class.java)
                    laporanData?.let {
                        b.updateKodeLaporan.setText("kode laporan : ${it.kodeLaporan}")
                        b.inputTanggal2.setText(it.tanggal)
                        b.inputKubikasi2.setText(it.kubikasi)
                        b.inputRitase2.setText(it.ritase)

                        val position = categories.indexOf(it.namaPt)
                        if (position != -1) {
                            b.spNamaPt2.setSelection(position)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "$error.message", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateLaporanToFirebase(kode: String){
        val user: FirebaseUser? = auth.currentUser
        val userId = user?.uid

        val kodeLaporan = kode
        val namaPt = b.spNamaPt2.selectedItem.toString()
        val tanggal = b.inputTanggal2.text.toString()
        val kubikasi = b.inputKubikasi2.text.toString()
        val ritase = b.inputRitase2.text.toString()
        val fotoUri = imageUri.toString()


        if (namaPt.isEmpty() || tanggal.isEmpty() || kubikasi.isEmpty() || ritase.isEmpty() || fotoUri.isNullOrEmpty()) {
            Toast.makeText(thisParent, "Semua form wajib dilengkapi", Toast.LENGTH_SHORT).show()
            return
        }

        val laporanUpdates = mapOf(
            "kodeLaporan" to kodeLaporan,
            "namaPt" to namaPt,
            "tanggal" to tanggal,
            "kubikasi" to kubikasi,
            "ritase" to ritase,
            "foto" to fotoUri
        )

        if (userId != null) {
            laporanRef.child(kode).updateChildren(laporanUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(thisParent, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        val fragment = DashboardActivity()
                        val fragmentManager = parentFragmentManager
                        val transaction = fragmentManager.beginTransaction()
                        transaction.replace(R.id.frameLayout, fragment)
                        transaction.addToBackStack(null)
                        transaction.commit()
                    } else {
                        Toast.makeText(thisParent, "Data gagal diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    data class Laporan(
        val kodeLaporan: String = "",
        val namaPt: String = "",
        val tanggal: String = "",
        val kubikasi: String = "",
        val ritase: String = "",
        val foto: String = ""
    )
}