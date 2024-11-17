package com.polinema.mi.app_maps.map

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityMapsBinding

class maps : AppCompatActivity(),
    View.OnClickListener,
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPoiClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnPolygonClickListener{

    private lateinit var b: ActivityMapsBinding
    private lateinit var gMap: GoogleMap
    private val arrayMarker = ArrayList<Marker>()
    private val arrayLines = ArrayList<LatLng>()
    private val arrayPoly1 = ArrayList<LatLng>()
    private lateinit var poly1: Polygon
    private var nomorLokasi = 1
    private lateinit var markerId: String

    // Variabel untuk pembaruan lokasi live
    private var liveUpdateEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastUpdatedLocation: LatLng? = null // Menyimpan lokasi terbaru

//    gambar polygon
    private var isDrawingPolygon = false
    private val polygonPoints = ArrayList<LatLng>()
    private var currentPolygon: Polygon? = null
    private val polygonsList = ArrayList<Polygon>()

//    save polygon
private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tambangRef: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Set OnClickListeners untuk FloatingActionButton
        b.fabMap1.setOnClickListener(this)
        b.fabMap2.setOnClickListener(this)
        b.fabMap3.setOnClickListener(this)
        b.fabMapDrawPolyline.setOnClickListener(this)
        b.fabMapDrawPolygon.setOnClickListener(this)
        b.fabMapDrawCircle.setOnClickListener(this)
        b.chip.setOnClickListener(this)
        b.fabMapCrudPolygon.setOnClickListener(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Mengatur aksi untuk EditText pencarian
        b.editText.setOnEditorActionListener { _, _, _ ->
            val query = b.editText.text.toString().trim()
            searchLocation(query)
            true
        }
//        save polygon
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://app-maps-91b91-default-rtdb.asia-southeast1.firebasedatabase.app/")
        tambangRef = database.getReference("bidangTambang")
    }

    // Runnable untuk pembaruan lokasi secara live
    private val liveUpdateRunnable = object : Runnable {
        override fun run() {
            if (liveUpdateEnabled) {
                lastUpdatedLocation = LatLng(-7.0, 110.0) // Ganti dengan lokasi yang diperbarui
                handler.postDelayed(this, 1000) // update setiap 1 detik
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabMap1 -> {
                gMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                lastUpdatedLocation?.let { moveCameraToLocation(it) }
            }
            R.id.fabMap2 -> {
                gMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                lastUpdatedLocation?.let { moveCameraToLocation(it) }
            }
            R.id.fabMap3 -> {
                gMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                lastUpdatedLocation?.let { moveCameraToLocation(it) }
            }
            R.id.fabMapDrawPolyline -> {
                drawPolyline()
                moveCameraToPolyline()
            }
            R.id.fabMapDrawPolygon -> {
                drawPolygon()
                moveCameraToPolygon()
            }
            R.id.fabMapDrawCircle -> {
                drawCircle()
                moveCameraToCircle()
            }
            R.id.chip -> liveUpdate(b.chip.isChecked)
            R.id.fabMapCrudPolygon -> startPolygonDrawing()
        }
    }

    private fun searchLocation(query: String) {
        when (query.lowercase()) {
            "jakarta" -> moveCameraToLocation(LatLng(-6.2088, 106.8456))
            "surabaya" -> moveCameraToLocation(LatLng(-7.2756, 112.6410))
            "bandung" -> moveCameraToLocation(LatLng(-6.9175, 107.6191))
            // Tambahkan daerah lain sesuai kebutuhan
            else -> {
                // Tampilkan dialog jika daerah tidak ditemukan
                AlertDialog.Builder(this)
                    .setTitle("Daerah Tidak Ditemukan")
                    .setMessage("Daerah '$query' tidak ditemukan di peta.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.setOnPoiClickListener(this)
        gMap.setOnMapLongClickListener(this)
        gMap.setOnMarkerClickListener(this)
        gMap.setOnInfoWindowClickListener(this)
        gMap.setOnPolygonClickListener(this)

        gMap.setOnMapClickListener { latLng ->
            addPointOfInterest(latLng) // Menangani klik pada peta
        }
        gMap.setOnMapClickListener { latLng ->
            if (isDrawingPolygon) {
                addPolygonPoint(latLng)
            } else {
                addPointOfInterest(latLng) // existing functionality
            }
        }

        gMap.setOnMapLongClickListener { latLng ->
            if (isDrawingPolygon && polygonPoints.size >= 3) {
                completePolygon()
            } else {
                onMapLongClick(latLng) // existing functionality
            }
        }
        val gurah = LatLng(-7.809840, 112.090409)
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gurah, 10f))
        loadTambangData()
    }

    override fun onPoiClick(pe: PointOfInterest) {
        AlertDialog.Builder(this)
            .setTitle(pe.name)
            .setMessage("Lat: ${pe.latLng.latitude}\nLng: ${pe.latLng.longitude}")
            .setNegativeButton("Keluar", null)
            .setPositiveButton("Buka GMaps") { _, _ ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:${pe.latLng.latitude},${pe.latLng.longitude}?q=${pe.name}"))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
            .show()
    }

    override fun onMapLongClick(pe: LatLng) {
        val marker = gMap.addMarker(
            MarkerOptions()
                .position(pe)
                .title("Lokasi ke-$nomorLokasi")
                .snippet("Lat: ${pe.latitude}, Lng: ${pe.longitude}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        arrayMarker.add(marker!!)
        nomorLokasi++
    }

    private fun addPointOfInterest(latLng: LatLng) {
        val marker = gMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("POI ${nomorLokasi++}")
                .snippet("Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
        arrayMarker.add(marker!!)
    }

    override fun onMarkerClick(pe: Marker): Boolean {
        markerId = pe.id
        gMap.animateCamera(CameraUpdateFactory.newLatLng(pe.position))
        pe.showInfoWindow()

        b.editText.setText("Posisi Saya : ${pe.position.latitude}, ${pe.position.longitude}")
        return true
    }

    override fun onInfoWindowClick(marker: Marker) {
        AlertDialog.Builder(this)
            .setTitle("Marker akan dihapus?")
            .setNegativeButton("HAPUS!") { _, _ ->
                arrayMarker.remove(marker)
                marker.remove()
            }
            .setPositiveButton("Cek marker di GMaps") { _, _ ->
                val uri = Uri.parse("geo:0,0?q=${marker.position.latitude},${marker.position.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
            .show()
    }

    override fun onPolygonClick(polygon: Polygon) {
        val tambangId = polygon.tag as? String ?: return

        tambangRef.child(tambangId).get().addOnSuccessListener { snapshot ->
            val tambang = snapshot.getValue(BidangTambang::class.java)
            tambang?.let {
                AlertDialog.Builder(this)
                    .setTitle("Info Tambang")
                    .setMessage("Nama: ${it.namaTambang}")
                    .setPositiveButton("Edit") { _, _ -> editPolygon(polygon) }
                    .setNegativeButton("Hapus") { _, _ -> deleteTambang(tambangId, polygon) }
                    .setNeutralButton("Tutup", null)
                    .show()
            }
        }
    }

    private fun drawPolyline() {
        arrayLines.clear()
        arrayLines.apply {
            add(LatLng(-7.8124227, 112.0116771)) // Kediri
            add(LatLng(-7.5955433, 111.8302694)) // Nganjuk
            add(LatLng(-7.3859208, 112.9490537)) // Malang
        }

        gMap.addPolyline(
            PolylineOptions()
                .addAll(arrayLines)
                .color(Color.RED)
                .width(10f)
        )
    }

    private fun drawPolygon() {
        // Polygon 1
        arrayPoly1.apply {
            clear()
            add(LatLng(-7.809806, 112.090401))
            add(LatLng(-7.809877, 112.090641))
            add(LatLng(-7.810689, 112.090327))
            add(LatLng(-7.810625, 112.090107))
            add(LatLng(-7.809806, 112.090401))
        }

        poly1 = gMap.addPolygon(
            PolygonOptions()
                .addAll(arrayPoly1)
                .fillColor(Color.BLUE)
                .strokeColor(Color.RED)
                .strokeWidth(5f)
                .clickable(true)
        )
        poly1.tag = "Poly 1" // Menandai polygon
    }

    private fun drawCircle() {
        val circleOptions = CircleOptions()
            .center(LatLng(-7.8124227, 112.0116771)) // Kediri
            .radius(10000.0) // Radius dalam meter
            .fillColor(Color.argb(50, 255, 0, 0))
            .strokeColor(Color.RED)
            .strokeWidth(5f)

        gMap.addCircle(circleOptions)
    }

    private fun moveCameraToLocation(location: LatLng) {
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
    }

    private fun moveCameraToPolyline() {
        val boundsBuilder = LatLngBounds.Builder()
        for (point in arrayLines) {
            boundsBuilder.include(point)
        }
        val bounds = boundsBuilder.build()
        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun moveCameraToPolygon() {
        val boundsBuilder = LatLngBounds.Builder()
        for (point in arrayPoly1) {
            boundsBuilder.include(point)
        }
        val bounds = boundsBuilder.build()
        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun moveCameraToCircle() {
        val circleCenter = LatLng(-7.8124227, 112.0116771) // Kediri
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(circleCenter, 12f))
    }

    private fun liveUpdate(enabled: Boolean) {
        liveUpdateEnabled = enabled
        if (enabled) {
            handler.post(liveUpdateRunnable) // Memulai pembaruan lokasi
        } else {
            handler.removeCallbacks(liveUpdateRunnable) // Menghentikan pembaruan lokasi
        }
    }

    override fun onStop() {
        super.onStop()
        liveUpdateEnabled = false
        handler.removeCallbacks(liveUpdateRunnable) // Menghentikan semua pembaruan saat activity berhenti
    }

//    tambah polygon
private fun startPolygonDrawing() {
    isDrawingPolygon = true
    polygonPoints.clear()
    showDrawingInstructions()
}

    private fun showDrawingInstructions() {
        AlertDialog.Builder(this)
            .setTitle("Menggambar Polygon")
            .setMessage("Tap pada peta untuk menambahkan titik polygon.\nTekan lama untuk menyelesaikan polygon.")
            .setPositiveButton("OK", null)
            .show()
    }
    private fun addPolygonPoint(latLng: LatLng) {
        polygonPoints.add(latLng)

        // Tambahkan marker untuk menandai titik polygon
        gMap.addMarker(MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

        // Gambar polygon sementara jika sudah ada minimal 3 titik
        if (polygonPoints.size >= 3) {
            drawTemporaryPolygon()
        }
    }

    private fun drawTemporaryPolygon() {
        currentPolygon?.remove()
        currentPolygon = gMap.addPolygon(PolygonOptions()
            .addAll(polygonPoints)
            .strokeColor(Color.BLUE)
            .fillColor(Color.argb(70, 0, 0, 255))
            .strokeWidth(5f)
            .clickable(true))
    }

    private fun completePolygon() {
        if (polygonPoints.size >= 3) {
            // Tambahkan titik pertama untuk menutup polygon
            polygonPoints.add(polygonPoints[0])

            // Buat polygon final
            val polygon = gMap.addPolygon(PolygonOptions()
                .addAll(polygonPoints)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(70, 0, 0, 255))
                .strokeWidth(5f)
                .clickable(true))

            polygonsList.add(polygon)

            // Tampilkan dialog untuk input nama tambang
            showSaveTambangDialog(polygon)

            // Reset state
            isDrawingPolygon = false
            currentPolygon?.remove()
            currentPolygon = null
        }
    }

    private fun showSaveTambangDialog(polygon: Polygon) {
        val input = EditText(this)
        input.hint = "Masukkan nama tambang"

        AlertDialog.Builder(this)
            .setTitle("Simpan Data Tambang")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val namaTambang = input.text.toString()
                if (namaTambang.isNotEmpty()) {
                    saveTambangToFirebase(namaTambang, polygon)
                } else {
                    Toast.makeText(this, "Nama tambang tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
    private fun showPolygonCompleteDialog(polygon: Polygon) {
        AlertDialog.Builder(this)
            .setTitle("Polygon Selesai")
            .setMessage("Polygon berhasil dibuat! Apa yang ingin Anda lakukan?")
            .setPositiveButton("Edit") { _, _ -> editPolygon(polygon) }
            .setNegativeButton("Hapus") { _, _ -> deletePolygon(polygon) }
            .setNeutralButton("Selesai", null)
            .show()
    }

    private fun editPolygon(polygon: Polygon) {
        // Implementasi edit polygon
        val points = polygon.points
        polygonPoints.clear()
        polygonPoints.addAll(points)
        isDrawingPolygon = true
        currentPolygon = polygon
        showDrawingInstructions()
    }

    private fun deletePolygon(polygon: Polygon) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Polygon")
            .setMessage("Apakah Anda yakin ingin menghapus polygon ini?")
            .setPositiveButton("Ya") { _, _ ->
                polygon.remove()
                polygonsList.remove(polygon)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
//    save polygon tambang
private fun saveTambangToFirebase(namaTambang: String, polygon: Polygon) {
    val currentUser = auth.currentUser
    if (currentUser == null) {
        Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
        return
    }

    // Convert polygon points to LatLngData
    val points = polygon.points.map {
        LatLngData(it.latitude, it.longitude)
    }

    // Generate unique ID for the tambang
    val tambangId = tambangRef.push().key ?: return

    // Create BidangTambang object
    val bidangTambang = BidangTambang(
        id = tambangId,
        namaTambang = namaTambang,
        polygonPoints = points,
        userId = currentUser.uid
    )

    // Save to Firebase
    tambangRef.child(tambangId).setValue(bidangTambang)
        .addOnSuccessListener {
            Toast.makeText(this, "Data tambang berhasil disimpan", Toast.LENGTH_SHORT).show()
            // Save polygon reference locally
            polygon.tag = tambangId
        }
        .addOnFailureListener { e ->
            Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

    // Tambahkan fungsi untuk memuat data tambang saat aplikasi dibuka
    private fun loadTambangData() {
        val currentUser = auth.currentUser ?: return

        tambangRef.orderByChild("userId").equalTo(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear existing polygons
                    polygonsList.forEach { it.remove() }
                    polygonsList.clear()

                    for (tambangSnapshot in snapshot.children) {
                        val tambang = tambangSnapshot.getValue(BidangTambang::class.java)
                        tambang?.let { drawSavedTambang(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@maps, "Gagal memuat data tambang", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun drawSavedTambang(tambang: BidangTambang) {
        val points = tambang.polygonPoints.map {
            LatLng(it.latitude, it.longitude)
        }

        val polygon = gMap.addPolygon(PolygonOptions()
            .addAll(points)
            .strokeColor(Color.BLUE)
            .fillColor(Color.argb(70, 0, 0, 255))
            .strokeWidth(5f)
            .clickable(true))

        polygon.tag = tambang.id
        polygonsList.add(polygon)
    }

    private fun deleteTambang(tambangId: String, polygon: Polygon) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Tambang")
            .setMessage("Apakah Anda yakin ingin menghapus data tambang ini?")
            .setPositiveButton("Ya") { _, _ ->
                tambangRef.child(tambangId).removeValue()
                    .addOnSuccessListener {
                        polygon.remove()
                        polygonsList.remove(polygon)
                        Toast.makeText(this, "Data tambang berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}
data class BidangTambang(
    val id: String = "",
    val namaTambang: String = "",
    val polygonPoints: List<LatLngData> = emptyList(),
    val userId: String = ""  // untuk mengidentifikasi pemilik data
)

data class LatLngData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)