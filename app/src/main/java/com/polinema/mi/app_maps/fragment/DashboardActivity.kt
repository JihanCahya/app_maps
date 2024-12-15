package com.polinema.mi.app_maps.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.auth.InAppNotificationManager
import com.polinema.mi.app_maps.auth.Notification
import com.polinema.mi.app_maps.databinding.ActivityDashboardBinding
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import com.polinema.mi.app_maps.utils.Constants


class DashboardActivity : Fragment() {
    private lateinit var b: ActivityDashboardBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View
    val apiUrl = Constants.BASE_URL
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var laporanRef: DatabaseReference
    private lateinit var storageRef: FirebaseStorage
    private lateinit var notificationManager: InAppNotificationManager
    private lateinit var adapter: LaporanAdapter
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            // You can set the image to an ImageView if needed
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityDashboardBinding.inflate(layoutInflater)
        v = b.root
        notificationManager = InAppNotificationManager(requireContext())
        notificationManager.initialize(requireActivity())
        setupFirebase()
        setupRecyclerView()
        loadLaporan()
        listenForNotifications()
        return v
    }
    private fun listenForNotifications() {
        val uid = auth.currentUser?.uid ?: return
        val notificationsRef = database.getReference("notifications").child(uid)

        notificationsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notification = snapshot.getValue(Notification::class.java)
                notification?.let { showNotification(it) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removal if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle movement if needed
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardActivity", "Error listening for notifications: ${error.message}")
            }
        })
    }

    private fun showNotification(notification: Notification) {
        val type = when (notification.type) {
            "success" -> InAppNotificationManager.NotificationType.SUCCESS
            "warning" -> InAppNotificationManager.NotificationType.WARNING
            "error" -> InAppNotificationManager.NotificationType.ERROR
            else -> InAppNotificationManager.NotificationType.INFO
        }

        notificationManager.showInAppNotification(
            title = notification.title ?: "Notification",
            message = notification.message ?: "",
            type = type
        )
    }
    private fun setupFirebase() {
        auth = Firebase.auth
        val uid = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/")
        laporanRef = database.getReference("laporan").child(uid)
        storageRef = FirebaseStorage.getInstance()
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter(mutableListOf(),
            onLaporClick = { laporan ->
                val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                if (laporan.tanggal == today) {
                    showUpdateLaporanDialog(laporan)
                } else {
                    Toast.makeText(thisParent, "Hanya dapat dilaporkan pada tanggal ${laporan.tanggal}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        b.rvLaporan.layoutManager = LinearLayoutManager(requireContext())
        b.rvLaporan.adapter = adapter
    }

    private fun showUpdateLaporanDialog(laporan: Laporan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_laporan, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Update Laporan")
            .setView(dialogView)
            .create()

        val etRitase = dialogView.findViewById<EditText>(R.id.etRitase)
        val etKubikasi = dialogView.findViewById<EditText>(R.id.etKubikasi)
        val btnUploadFoto = dialogView.findViewById<Button>(R.id.btnUploadFoto)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)
        val ivSuratJalan = dialogView.findViewById<ImageView>(R.id.ivSuratJalan)

        btnUploadFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            val ritase = etRitase.text.toString().toIntOrNull()
            val kubikasi = etKubikasi.text.toString().toIntOrNull()

            if (ritase == null || kubikasi == null || selectedImageUri == null) {
                Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadLaporanToFirebase(laporan, ritase, kubikasi, selectedImageUri!!)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadLaporanToFirebase(laporan: Laporan, ritase: Int, kubikasi: Int, imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val storageReference = storageRef.reference.child("surat_jalan/${uid}/${laporan.kodeLaporan}")

        storageReference.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val updates = mapOf(
                        "ritase" to ritase,
                        "kubikasi" to kubikasi,
                        "foto_surat_jalan" to uri.toString(),
                        "status" to "1"
                    )

                    laporanRef.child(laporan.kodeLaporan).updateChildren(updates)
                        .addOnSuccessListener {
                            // After successful Firebase update, send to API
                            sendLaporanToApi(laporan.kodeLaporan, ritase, kubikasi, imageUri, uri.toString())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal update laporann: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal upload foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendLaporanToApi(kodeLaporan: String, ritase: Int, kubikasi: Int, imageUri: Uri, downloadUrl: String) {
        // Create Retrofit service
        val apiService = RetrofitClient.getInstance(apiUrl).create(LaporanApiService::class.java)

        // Prepare request body parts using the Firebase download URL
        val kodeLaporanBody = RequestBody.create("text/plain".toMediaTypeOrNull(), kodeLaporan)
        val ritaseBody = RequestBody.create("text/plain".toMediaTypeOrNull(), ritase.toString())
        val kubikasiBody = RequestBody.create("text/plain".toMediaTypeOrNull(), kubikasi.toString())
        val statusBody = RequestBody.create("text/plain".toMediaTypeOrNull(), "1")
        val fotoUrlBody = RequestBody.create("text/plain".toMediaTypeOrNull(), downloadUrl)

        // Launch coroutine to make API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.updateLaporan(
                    kodeLaporan = kodeLaporanBody,
                    ritase = ritaseBody,
                    kubikasi = kubikasiBody,
                    status = statusBody,
                    foto_surat_jalan = fotoUrlBody
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Laporan berhasil diupdate di API", Toast.LENGTH_SHORT).show()
                    } else {
                        // Parse error response for more details
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(requireContext(), "Gagal update laporan di API: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: HttpException) {
                // Handle HTTP exceptions
                val errorBody = e.response()?.errorBody()?.string()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "HTTP Error: ${e.message()} - $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                // Handle network errors
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Handle any other exceptions
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Unexpected Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    // Update the LaporanApiService interface
    interface LaporanApiService {
        @Multipart
        @POST("laporan/update")
        suspend fun updateLaporan(
            @Part("kode_laporan") kodeLaporan: RequestBody,
            @Part("ritase") ritase: RequestBody,
            @Part("kubikasi") kubikasi: RequestBody,
            @Part("status") status: RequestBody,
            @Part("foto_surat_jalan") foto_surat_jalan: RequestBody
        ): Response<UpdateLaporanResponse>
    }
    object RetrofitClient {
        fun getInstance(baseUrl: String): Retrofit {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
    // Utility function to get real path from URI
    private fun getRealPathFromURI(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = columnIndex?.let { cursor.getString(it) } ?: ""
        cursor?.close()
        return path
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

    // Laporan Update API Interfac
    data class UpdateLaporanResponse(
        val success: Boolean,
        val message: String,
        val data: Any? = null
    )
}