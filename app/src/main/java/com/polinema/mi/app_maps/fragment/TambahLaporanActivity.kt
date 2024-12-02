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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityTambahLaporanBinding
import java.util.Calendar

class TambahLaporanActivity : Fragment(), View.OnClickListener {

    private lateinit var b : ActivityTambahLaporanBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var laporanRef: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityTambahLaporanBinding.inflate(layoutInflater)
        v = b.root

        b.btnKembali.setOnClickListener(this)
        b.btnPilihFoto.setOnClickListener(this)
        b.btnDatePicker.setOnClickListener(this)
        b.btnTambahData.setOnClickListener(this)

        setupPtSpinner()
        setupFirebase()

        return v
    }

    private fun setupPtSpinner() {
        val categories = listOf("PT. GARUDA", "PT. ELANG")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spNamaPt.adapter = adapter
    }

    private fun setupFirebase() {
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://app-maps-91b91-default-rtdb.asia-southeast1.firebasedatabase.app/")
        laporanRef = database.getReference("laporan")
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnKembali -> {
                val fragment = DashboardActivity()
                val fragmentManager = parentFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.frameLayout, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }
            R.id.btnPilihFoto -> {
                openImageChooser()
            }
            R.id.btnDatePicker -> {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(
                    thisParent,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val selectedDate = "$selectedDay-${selectedMonth + 1}-$selectedYear"
                        b.inputTanggal.setText(selectedDate)
                    },
                    year, month, dayOfMonth
                )

                datePickerDialog.show()
            }
            R.id.btnTambahData -> {
                saveLaporanToFirebase()
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
            b.imvFoto.setImageURI(imageUri)
        }
    }
    private fun saveLaporanToFirebase() {
        val user: FirebaseUser? = auth.currentUser
        val userId = user?.uid

        val kodeLaporan = "Laporan_${System.currentTimeMillis()}"
        val namaPt = b.spNamaPt.selectedItem.toString()
        val tanggal = b.inputTanggal.text.toString()
        val kubikasi = b.inputKubikasi.text.toString()
        val ritase = b.inputRitase.text.toString()
        val fotoUri = imageUri.toString()

        if (namaPt.isEmpty() || tanggal.isEmpty() || kubikasi.isEmpty() || ritase.isEmpty() || fotoUri.isNullOrEmpty()) {
            Toast.makeText(thisParent, "Semua form wajib dilengkapi", Toast.LENGTH_SHORT).show()
            return
        }

        val laporan = Laporan(
            kodeLaporan = kodeLaporan,
            userId = userId,
            namaPt = namaPt,
            tanggal = tanggal,
            kubikasi = kubikasi,
            ritase = ritase,
            foto = fotoUri
        )

        if (userId != null) {
            laporanRef.child(userId).child(kodeLaporan).setValue(laporan)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(thisParent, "Data berhasil dimasukkan", Toast.LENGTH_SHORT).show()
                        val fragment = DashboardActivity()
                        val fragmentManager = parentFragmentManager
                        val transaction = fragmentManager.beginTransaction()
                        transaction.replace(R.id.frameLayout, fragment)
                        transaction.addToBackStack(null)
                        transaction.commit()
                    } else {
                        Toast.makeText(thisParent, "Data gagal dimasukkan", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    data class Laporan(
        val kodeLaporan: String,
        val userId: String?,
        val namaPt: String,
        val tanggal: String,
        val kubikasi: String,
        val ritase: String,
        val foto: String
    )
}