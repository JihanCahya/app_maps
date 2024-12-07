package com.polinema.mi.app_maps.auth

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.MainActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityLoginBinding
import com.polinema.mi.app_maps.map.maps

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b : ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    lateinit var progressDialog: ProgressDialog
    var currentUser : FirebaseUser? = null
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    }
    companion object{
        private const val RC_SIGN_IN = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLogDaftar.setOnClickListener(this)
        b.btnLogMasuk.setOnClickListener(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proses login")
        progressDialog.setMessage("Silahkan tunggu...")

        auth = Firebase.auth
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
                    Toast.makeText(this, "Mohon isi form email dan password", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(this, "Email anda belum terverifikasi", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Email/password salah", Toast.LENGTH_LONG).show()
                            }
                            progressDialog.dismiss()
                        }
                }
            }
        }
    }

    private fun checkUserRole(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/").reference.child("users").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nama = currentUser!!.displayName.toString()
                    val intent = Intent(this@LoginActivity, BaseActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this@LoginActivity, "Selamat datang $nama", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Terjadi kesalahan saat mengakses database", Toast.LENGTH_LONG).show()
            }
        })
    }
}