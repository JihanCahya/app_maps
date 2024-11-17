package com.polinema.mi.app_maps

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.polinema.mi.app_maps.auth.LoginActivity
import com.polinema.mi.app_maps.databinding.ActivityMainBinding
import mumayank.com.airlocationlibrary.AirLocation

class MainActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener {

    private lateinit var b: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    var currentUser: FirebaseUser? = null
    private lateinit var mapFragment : SupportMapFragment
    lateinit var airLoc : AirLocation
    var marker : Marker? = null
    var liveUpdate = true
    lateinit var gMap : GoogleMap
    lateinit var ll : LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        mapFragment = supportFragmentManager.findFragmentById(R.id.fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        b.fab.setOnClickListener(this)
        b.chip.isChecked = true
        b.chip.setOnClickListener(this)
        b.btnLogout.setOnClickListener(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLoc.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLoc.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        airLoc = AirLocation(this, object : AirLocation.Callback {
            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                Toast.makeText(
                    this@MainActivity,
                    "Gagal mendapatkan posisi saat ini",
                    Toast.LENGTH_SHORT
                ).show()
                b.editText.setText("Gagal mendapatkan posisi saat ini")
            }

            override fun onSuccess(location: ArrayList<Location>) {
                if (liveUpdate) {
                    marker?.remove()
                    val location = location.last()
                    ll = LatLng(location.latitude, location.longitude)
                    marker = gMap.addMarker(MarkerOptions().position(ll).title("Posisi Saya"))
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 16.0f))
                    b.editText.setText("Posisi saya : LAT=${ll.latitude}, LNG=${ll.longitude}")

                    gMap.setOnMarkerClickListener { clickedMarker ->
                        val clickedPosition = clickedMarker.position
                        b.editText.setText(
                            "Posisi sekarang: LAT=${clickedPosition.latitude}, LNG=${clickedPosition.longitude}"
                        )
                        false
                    }

                    // Add other markers
                    val marker1 = LatLng(-7.800996880848467, 112.00842726690627)
                    gMap.addMarker(MarkerOptions().position(marker1).title("Kampus 1 PSDKU POLINEMA"))

                    val marker2 = LatLng(-7.4310467, 112.6852896)
                    gMap.addMarker(MarkerOptions().position(marker2).title("Rumah Saya"))
                }
            }
        })
        airLoc.start()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.fab -> {
                b.chip.isChecked = false
                liveUpdate = b.chip.isChecked
                gMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(ll,16.0f))
                b.editText.setText(
                    "Posisi saya : LAT=${ll.latitude}, LNG=${ll.longitude}")
            }
            R.id.chip -> {
                liveUpdate = b.chip.isChecked
            }
            R.id.btnLogout -> {
                showLogoutConfirmationDialog()
            }
        }
    }
}