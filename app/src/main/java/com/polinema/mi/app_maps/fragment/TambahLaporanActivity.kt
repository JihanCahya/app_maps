package com.polinema.mi.app_maps.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.Calendar
import retrofit2.http.Multipart
import android.provider.MediaStore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Part
import java.io.File
import com.polinema.mi.app_maps.utils.Constants


class TambahLaporanActivity : Fragment(), View.OnClickListener {
    val apiUrl = Constants.BASE_URL
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

    private var BASE_URL = "http://172.20.10.3:8000/api/v1/"

    private fun setupPtSpinner() {
        val retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val ptService = retrofit.create(PTService::class.java)

        ptService.getAllPT().enqueue(object : Callback<List<PT>> {
            override fun onResponse(call: Call<List<PT>>, response: Response<List<PT>>) {
                if (response.isSuccessful) {
                    val ptList = response.body() ?: emptyList()
                    val ptNames = ptList.map { it.nama }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ptNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    b.spNamaPt.adapter = adapter
                } else {
                    Toast.makeText(thisParent, "Failed to load PT data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PT>>, t: Throwable) {
                Toast.makeText(thisParent, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("","Error: ${t.message}")
            }
        })
    }
    interface PTService {
        @GET("pt")
        fun getAllPT(): Call<List<PT>>
    }
    private fun setupFirebase() {
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/")
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
                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val ptService = retrofit.create(PTService::class.java)

                ptService.getAllPT().enqueue(object : Callback<List<PT>> {
                    override fun onResponse(call: Call<List<PT>>, response: Response<List<PT>>) {
                        if (response.isSuccessful) {
                            val ptList = response.body() ?: emptyList()
                            val selectedPt = ptList.find { it.nama == b.spNamaPt.selectedItem.toString() }

                            if (selectedPt != null) {
                                saveLaporanToFirebase(selectedPt)
                            } else {
                                Toast.makeText(thisParent, "Pilih PT terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(thisParent, "Gagal mengambil data PT", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<List<PT>>, t: Throwable) {
                        Toast.makeText(thisParent, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
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
    private fun saveLaporanToFirebase(selectedPt: PT) {
        val user: FirebaseUser? = auth.currentUser
        val userId = user?.uid

        val kodeLaporan = "Laporan_${System.currentTimeMillis()}"
        val namaPt = b.spNamaPt.selectedItem.toString()
        val tanggal = b.inputTanggal.text.toString()
        val kubikasi = b.inputKubikasi.text.toString()
        val ritase = b.inputRitase.text.toString()

        if (namaPt.isEmpty() || tanggal.isEmpty() || kubikasi.isEmpty() || ritase.isEmpty()) {
            Toast.makeText(thisParent, "Semua form wajib dilengkapi", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if image is selected
        if (imageUri == null) {
            Toast.makeText(thisParent, "Pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a reference for the image in Firebase Storage
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("laporan/${kodeLaporan}_${System.currentTimeMillis()}.jpg")

        // Upload image to Firebase Storage
        imageRef.putFile(imageUri!!).addOnSuccessListener { taskSnapshot ->
            // Get the download URL of the uploaded image
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()

                // Create Laporan object with the image URL
                val laporan = Laporan(
                    kodeLaporan = kodeLaporan,
                    userId = userId,
                    namaPt = namaPt,
                    tanggal = tanggal,
                    kubikasi = kubikasi,
                    ritase = ritase,
                    foto = imageUrl, // Use the Firebase Storage URL
                    idPt = selectedPt.id_pt
                )

                // Save to Firebase Realtime Database
                if (userId != null) {
                    laporanRef.child(userId).child(kodeLaporan).setValue(laporan)
                        .addOnCompleteListener { firebaseTask ->
                            if (firebaseTask.isSuccessful) {
                                // Kirim ke MySQL dengan URL foto
                                sendLaporanToMySQL(
                                    imageUrl, // Kirim URL foto
                                    userId,
                                    selectedPt,
                                    tanggal,
                                    ritase,
                                    kubikasi,
                                    kodeLaporan
                                )
                                Toast.makeText(thisParent, "Laporan berhasil disimpan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(thisParent, "Data gagal dimasukkan di Firebase", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(thisParent, "Gagal menyimpan: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(thisParent, "Gagal mendapatkan URL gambar: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(thisParent, "Gagal mengunggah gambar: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getRealPathFromURI(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        return cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            it.getString(columnIndex)
        } ?: uri.path ?: ""
    }

    private fun sendLaporanToMySQL(
        imageUrl: String, // Ganti parameter MultipartBody.Part dengan String imageUrl
        userId: String,
        selectedPt: PT,
        tanggal: String,
        ritase: String,
        kubikasi: String,
        kodeLaporan: String
    ) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val laporanService = retrofit.create(LaporanService::class.java)

        // Ubah interface LaporanService untuk menerima URL foto sebagai string


        // Tambahkan data class untuk request body
        val laporanRequest = LaporanRequest(
            id_user = userId,
            id_pt = selectedPt.id_pt,
            tanggal = tanggal,
            ritase = ritase,
            kubikasi = kubikasi,
            foto = imageUrl, // Kirim URL foto dari Firebase
            id_laporan_fb = kodeLaporan
        )

        laporanService.createLaporan(laporanRequest).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(thisParent, "Data berhasil dimasukkan", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        println("Error Response Code: ${response.code()}")
                        println("Error Response Body: $errorBody")
                        Toast.makeText(
                            thisParent,
                            "Gagal: HTTP ${response.code()} - $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        println("Error parsing error response: ${e.message}")
                        Toast.makeText(
                            thisParent,
                            "Gagal mengirim data: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                println("Network Error: ${t.message}")
                println("Stack trace: ${t.stackTraceToString()}")
                Toast.makeText(
                    thisParent,
                    "Network Error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    data class LaporanRequest(
        val id_user: String,
        val id_pt: Int,
        val tanggal: String,
        val ritase: String,
        val kubikasi: String,
        val foto: String,
        val id_laporan_fb: String
    )

    // Buat objek request dengan URL foto

    // Update the interface for proper error handling

    // Helper method to navigate to dashboard
    private fun navigateToDashboard() {
        val fragment = DashboardActivity()
        val fragmentManager = parentFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
    interface LaporanService {
        @POST("laporan/store")
        fun createLaporan(
            @Body laporanData: LaporanRequest
        ): Call<Map<String, Any>>
    }
    data class Laporan(
        val kodeLaporan: String,
        val userId: String?,
        val namaPt: String,
        val tanggal: String,
        val kubikasi: String,
        val ritase: String,
        val foto: String,
        val idPt: Int
    )
    data class PT(
        val id_pt: Int,
        val nama: String,
        val harga_perkubikasi: Int,
        val ongkos_supir: Int,
        val harga_material: Int
    )
}