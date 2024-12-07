package com.polinema.mi.app_maps.auth

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.polinema.mi.app_maps.MainActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityRegisterBinding
import com.polinema.mi.app_maps.map.maps
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private lateinit var iv: IvParameterSpec
    private var encryptedText: String = ""
    lateinit var auth: FirebaseAuth
    lateinit var progressDialog: ProgressDialog
    var currentUser : FirebaseUser? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Proses register")
        progressDialog.setMessage("Silahkan tunggu...")

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("users")

        b.btnRegLogin.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        b.btnRegDaftar.setOnClickListener {
            progressDialog.show()
            val nama = b.regNama.text.toString()
            val email = b.regEmail.text.toString()
            val password = b.regPassword.text.toString()

            if (!validateForm(nama, email, password)) {
                return@setOnClickListener
            }

            registerUser(nama, email, password)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isLoggedIn()) {
            autoLogin()
        }
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun autoLogin() {
        val email = sharedPreferences.getString("email", "")
        val uid = sharedPreferences.getString("uid", "")

        if (!email.isNullOrBlank() && !uid.isNullOrBlank()) {
            checkUserRole(uid)
        }
    }

    private fun validateForm(nama: String, email: String, password: String): Boolean {
        progressDialog.dismiss()
        return when {
            nama.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                Toast.makeText(this, "Harap isi semua kolom.", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun registerUser(nama: String, email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    currentUser = auth.currentUser
                    currentUser?.let{ user ->
                        val profileUpdates = userProfileChangeRequest {
                            displayName = nama
                        }
                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            if (it.isSuccessful) {
                                user.sendEmailVerification()
                                saveUserDataToDatabase(user.uid, nama, email, password)
                            } else {
                                Toast.makeText(this, "Gagal memperbarui profil pengguna.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Toast.makeText(this, "Registrasi berhasil, silakan cek email Anda untuk verifikasi.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else{
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Registrasi gagal, email sudah terdaftar", Toast.LENGTH_SHORT).show()
                    } else if (exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this, "Registrasi gagal, email atau kata sandi tidak valid", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Registrasi gagal, terjadi kesalahan: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                progressDialog.dismiss()
            }
    }

    private fun checkUserRole(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/").reference.child("users").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val nama = dataSnapshot.child("nama").value as String
                    val intent = Intent(this@RegisterActivity, maps::class.java)
                    startActivity(intent)
                    Toast.makeText(this@RegisterActivity, "Selamat datang $nama", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RegisterActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@RegisterActivity, "Terjadi kesalahan saat mengakses database", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveUserDataToDatabase(uid: String, nama: String, email: String, password: String) {
        val userData = UserData(
            uid = uid,
            nama = encryptedText(nama),
            email = encryptedText(email),
            password = encryptedText(password),
        )

        usersRef.child(uid).setValue(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Berhasil Menambahkan User", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal Menambahkan User: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun encryptedText(text: String): String {
        val secretKey = "1234567887654321"
        val ivSpec = generateIv()

        return try {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(text.toByteArray())

            val combined = ivSpec.iv + encryptedBytes
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    private fun generateIv(): IvParameterSpec {
        val iv = ByteArray(16)
        java.security.SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }
}

data class UserData(
    val uid: String,
    val nama: String,
    val email: String,
    val password: String
)