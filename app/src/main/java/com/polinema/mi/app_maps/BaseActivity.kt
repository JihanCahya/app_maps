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

class BaseActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var b: ActivityBaseBinding
    private lateinit var auth: FirebaseAuth
    var currentUser: FirebaseUser? = null
    lateinit var fragMap : MapActivity
    lateinit var fragDashboard : DashboardActivity
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
                Toast.makeText(this, "Anda memilih menu profile", Toast.LENGTH_SHORT).show()
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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/barokahjayamulia?igsh=ODVscW1ncjdncmN0"))
                startActivity(intent)
            }
        }
        return true
    }
}