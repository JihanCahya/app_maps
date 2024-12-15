package com.polinema.mi.app_maps.auth
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityLoginBinding
import android.Manifest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Date

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b : ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    lateinit var progressDialog: ProgressDialog
    var currentUser : FirebaseUser? = null
    private lateinit var notificationManager: InAppNotificationManager
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    }

    override fun onStart() {
        super.onStart()
//        if (isLoggedIn()) {
//            autoLogin()
//        }
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
//
    private fun saveLoginStatus(email: String, uid: String) {
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", true)
            putString("email", email)
            putString("uid", uid)
            apply()
        }
    }

    private fun autoLogin() {
        val email = sharedPreferences.getString("email", "")
        val uid = sharedPreferences.getString("uid", "")

        if (!email.isNullOrBlank() && !uid.isNullOrBlank()) {
            checkUserRole(uid)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnLogDaftar -> {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            }

            R.id.btnLogMasuk -> {
                progressDialog.show()
                val email = b.logEmail.text.toString()
                val password = b.logPassword.text.toString()

                if (email.isEmpty() || password.isEmpty()){
                    progressDialog.dismiss()
                    notificationManager.showInAppNotification(
                        title = "Login Gagal",
                        message = "Mohon isi form email dan password",
                        type = InAppNotificationManager.NotificationType.ERROR
                    )
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                currentUser = auth.currentUser
                                if (currentUser != null) {
                                    if (currentUser!!.isEmailVerified) {
                                        saveLoginStatus(currentUser!!.email.toString(), currentUser!!.uid)
                                        checkUserRole(currentUser!!.uid)
                                    } else {
                                        progressDialog.dismiss()
                                        notificationManager.showInAppNotification(
                                            title = "Verifikasi Email",
                                            message = "Email anda belum terverifikasi",
                                            type = InAppNotificationManager.NotificationType.WARNING
                                        )
                                    }
                                }
                            } else {
                                progressDialog.dismiss()
                                notificationManager.showInAppNotification(
                                    title = "Login Gagal",
                                    message = "Email/password salah",
                                    type = InAppNotificationManager.NotificationType.ERROR
                                )
                            }
                        }
                }
            }
        }
    }

    private fun checkUserRole(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/")
            .reference.child("users").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nama = currentUser!!.displayName.toString()

                    // Show welcome notification
                    notificationManager.showInAppNotification(
                        title = "Login Berhasil",
                        message = "Selamat datang $nama",
                        type = InAppNotificationManager.NotificationType.SUCCESS
                    )

                    // Delayed navigation to prevent notification from being dismissed immediately
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(1500) // Give user time to see notification
                        val intent = Intent(this@LoginActivity, BaseActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    notificationManager.showInAppNotification(
                        title = "Login Gagal",
                        message = "Data pengguna tidak ditemukan",
                        type = InAppNotificationManager.NotificationType.ERROR
                    )
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                notificationManager.showInAppNotification(
                    title = "Kesalahan Database",
                    message = "Terjadi kesalahan saat mengakses database",
                    type = InAppNotificationManager.NotificationType.ERROR
                )
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Initialize In-App Notification Manager
        notificationManager = InAppNotificationManager(this)
        notificationManager.initialize(this)

        b.btnLogDaftar.setOnClickListener(this)
        b.btnLogMasuk.setOnClickListener(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proses login")
        progressDialog.setMessage("Silahkan tunggu...")

        auth = Firebase.auth

        // Request notification permission for Android 13+ (API level 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }


    companion object {
        private const val RC_SIGN_IN = 200
        private const val NOTIFICATION_PERMISSION_CODE = 100
    }
}

