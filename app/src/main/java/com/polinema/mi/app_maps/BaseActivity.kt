package com.polinema.mi.app_maps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.polinema.mi.app_maps.auth.LoginActivity
import com.polinema.mi.app_maps.databinding.ActivityBaseBinding
import com.polinema.mi.app_maps.fragment.DashboardActivity
import com.polinema.mi.app_maps.fragment.MapActivity
import com.polinema.mi.app_maps.fragment.WebViewActivity
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BaseActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var b: ActivityBaseBinding
    private lateinit var auth: FirebaseAuth
    var currentUser: FirebaseUser? = null
    lateinit var fragMap : MapActivity
    lateinit var fragDashboard : DashboardActivity
    lateinit var fragWebView : WebViewActivity
    lateinit var ft : FragmentTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        b.bottomNavigationView.setOnItemSelectedListener(this)
        fragMap = MapActivity()
        fragDashboard = DashboardActivity()
        fragWebView = WebViewActivity()
    }

    override fun onStart() {
        super.onStart()
        ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameLayout, fragDashboard).commit()
        b.frameLayout.setBackgroundColor(
            Color.argb(255, 255, 255, 255)
        )
        b.frameLayout.visibility = View.VISIBLE
        true
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Logout")
        builder.setMessage("Apakah Anda yakin ingin logout?")
        builder.setPositiveButton("Ya") { _, _ ->
            auth.signOut()
            clearLoginStatus()
            Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun clearLoginStatus() {
        val sharedPreferences = this.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", false)
            remove("email")
            remove("uid")
            apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var menuInflater = menuInflater
        menuInflater.inflate(R.menu.option_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.itemLogout ->{
                showLogoutConfirmationDialog()
            }
            R.id.itemProfile -> {
                showProfileDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.itemHome -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout, fragDashboard).commit()
                b.frameLayout.setBackgroundColor(
                    Color.argb(255, 255, 255, 255)
                )
                b.frameLayout.visibility = View.VISIBLE
                true
            }
            R.id.itemMaps -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout, fragMap).commit()
                b.frameLayout.setBackgroundColor(
                    Color.argb(255, 255, 255, 255)
                )
                b.frameLayout.visibility = View.VISIBLE
                true
            }
            R.id.itemWebView -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout, fragWebView).commit()
                b.frameLayout.setBackgroundColor(
                    Color.argb(255, 255, 255, 255)
                )
                b.frameLayout.visibility = View.VISIBLE
                true
            }
        }
        return true
    }

    private fun showProfileDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid)

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val encryptedName = snapshot.child("nama").getValue(String::class.java) ?: "Name not set"
                        val encryptedEmail = snapshot.child("email").getValue(String::class.java) ?: "Email not set"

                        val decryptedName = if (encryptedName != "Name not set") {
                            decryptText(encryptedName)
                        } else {
                            "Name not set"
                        }

                        val decryptedEmail = if (encryptedEmail != "Email not set") {
                            decryptText(encryptedEmail)
                        } else {
                            "Email not set"
                        }

                        AlertDialog.Builder(this@BaseActivity)
                            .setTitle("Informasi Profil")
                            .setMessage("Name: $decryptedName\nEmail: $decryptedEmail")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(this@BaseActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BaseActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    fun decryptText(encryptedText: String): String {
        val secretKey = "1234567887654321"

        return try {
            val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

            val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)

            val iv = encryptedData.copyOfRange(0, 16)
            val encryptedBytes = encryptedData.copyOfRange(16, encryptedData.size)

            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}