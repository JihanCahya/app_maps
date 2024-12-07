package com.polinema.mi.app_maps.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityMapsBinding
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.*
import mumayank.com.airlocationlibrary.AirLocation
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

class maps : AppCompatActivity(),
    View.OnClickListener,
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPoiClickListener,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnPolygonClickListener {

    private lateinit var b: ActivityMapsBinding
    private lateinit var gMap: GoogleMap
    private val arrayMarker = ArrayList<Marker>()
    private val arrayPoly1 = ArrayList<LatLng>()
    private lateinit var poly1: Polygon
    private var nomorLokasi = 1
    private lateinit var markerId: String

    // Location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocationMarker: Marker? = null
    private var currentLocation: Location? = null

    // Route
    private var currentRoute: Polyline? = null

    // Drawing polygon
    private var isDrawingPolygon = false
    private val polygonPoints = ArrayList<LatLng>()
    private var currentPolygon: Polygon? = null
    private val polygonsList = ArrayList<Polygon>()
    private val markerList = mutableListOf<Marker>()

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tambangRef: DatabaseReference

    private var lastUpdatedLocation: LatLng? = null

    private val polygonMarkers = mutableMapOf<String, MutableList<Marker>>()

    var liveUpdate = true
    lateinit var ll : LatLng
    lateinit var airLoc : AirLocation
    var marker : Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupLocationServices()
        setupUI()
        setupFirebase()
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

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationUI(location)
                }
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        currentLocation = location
        val latLng = LatLng(location.latitude, location.longitude)

        currentLocationMarker?.remove()
        currentLocationMarker = gMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Lokasi Saya")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun setupUI() {
        b.apply {
            fabMap1.setOnClickListener(this@maps)
            fabMap2.setOnClickListener(this@maps)
            fabMap3.setOnClickListener(this@maps)
            fabMapDrawPolygon.setOnClickListener(this@maps)
            fabMapCrudPolygon.setOnClickListener(this@maps)
            chip.setOnClickListener(this@maps)
            fab.setOnClickListener(this@maps)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        b.editText.setOnEditorActionListener { _, _, _ ->
            searchLocation(b.editText.text.toString().trim())
            true
        }

        b.chip.isChecked = true
    }

    private fun setupFirebase() {
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://pml-sem-5-default-rtdb.firebaseio.com/")
        tambangRef = database.getReference("bidangTambang")
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
            R.id.fabMapDrawPolygon -> {
                drawPolygon()
                moveCameraToPolygon()
            }
            R.id.fabMapCrudPolygon -> startPolygonDrawing()
            R.id.chip -> {
                liveUpdate = b.chip.isChecked
            }
            R.id.fab -> {
                b.chip.isChecked = false
                liveUpdate = b.chip.isChecked
                gMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(ll,16.0f))
                b.editText.setText(
                    "Posisi saya : LAT=${ll.latitude}, LNG=${ll.longitude}")
            }
        }
    }

    private fun searchLocation(query: String) {
        when (query.lowercase()) {
            "jakarta" -> moveCameraToLocation(LatLng(-6.2088, 106.8456))
            "surabaya" -> moveCameraToLocation(LatLng(-7.2756, 112.6410))
            "bandung" -> moveCameraToLocation(LatLng(-6.9175, 107.6191))
            else -> {
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
        gMap.setOnMarkerClickListener(this)
        gMap.setOnInfoWindowClickListener(this)
        gMap.setOnPolygonClickListener(this)

        gMap.setOnMapClickListener { latLng ->
            if (isDrawingPolygon) {
                addPolygonPoint(latLng)
            } else {
                b.editText.setText("${latLng}")
            }
        }


        gMap.setOnMapLongClickListener { latLng ->
            if (isDrawingPolygon && polygonPoints.size >= 3) {
                completePolygon()
            }
        }

        airLoc = AirLocation(this, object : AirLocation.Callback {
            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                Toast.makeText(
                    this@maps,
                    "Gagal mendapatkan posisi saat ini",
                    Toast.LENGTH_SHORT
                ).show()
                b.editText.setText("Gagal mendapatkan posisi saat ini")
            }

            override fun onSuccess(location: ArrayList<Location>) {
                if (liveUpdate) {
                    marker?.remove()
                    val location = location.last()
                    currentLocation = location
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
                }
            }
        })
        airLoc.start()

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

//        polygonMarkers[tambangId]?.forEach { marker ->
//            marker.isVisible = !marker.isVisible
//        }

        tambangRef.child(tambangId).get().addOnSuccessListener { snapshot ->
            val tambang = snapshot.getValue(BidangTambang::class.java)
            tambang?.let {
                AlertDialog.Builder(this)
                    .setTitle("Info Tambang")
                    .setMessage("Nama: ${it.namaTambang}")
                    .setPositiveButton("Edit") { _, _ -> showEditOptions(polygon, tambangId) }
                    .setNegativeButton("Hapus") { _, _ -> deleteTambang(tambangId, polygon) }
                    .setNeutralButton("Rute") { _, _ ->
                        if (currentLocation != null) {
                            val origin = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                            val destination = tambang.polygonPoints.first().toLatLng()
                            fetchOSRMRoute(origin, destination)
                        } else {
                            airLoc.start()
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (currentLocation != null) {
                                    val origin = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                                    val destination = tambang.polygonPoints.first().toLatLng()
                                    fetchOSRMRoute(origin, destination)
                                } else {
                                    Toast.makeText(this, "Mohon tunggu, sedang mendapatkan lokasi...", Toast.LENGTH_SHORT).show()
                                }
                            }, 2000)
                        }
                    }
                    .show()
            }
        }
    }
    private fun showEditOptions(polygon: Polygon, tambangId: String) {
        val options = arrayOf("Edit Nama", "Edit Bentuk", "Sembunyikan Titik")
        AlertDialog.Builder(this)
            .setTitle("Pilih Opsi Edit")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditNameDialog(tambangId)
                    1 -> startPolygonEditing(polygon, tambangId)
                    2 -> hidePolygonVertices(tambangId)
                }
            }
            .show()
    }
    private fun showEditNameDialog(tambangId: String) {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Edit Nama Tambang")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    tambangRef.child(tambangId).child("namaTambang").setValue(newName)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    private fun startPolygonEditing(polygon: Polygon, tambangId: String) {
        polygonMarkers[tambangId]?.forEach { marker ->
            marker.isVisible = true
            marker.isDraggable = true
        }

        Toast.makeText(
            this@maps,
            "Tahan lalu geser pada marker",
            Toast.LENGTH_SHORT
        ).show()

        gMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}

            override fun onMarkerDrag(marker: Marker) {
                updatePolygonShape(polygon, tambangId)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                updatePolygonShape(polygon, tambangId)
                Toast.makeText(
                    this@maps,
                    "Tahan/klik lama untuk menyimpan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        gMap.setOnMapLongClickListener { latLng ->
            val markerInRange = polygonMarkers[tambangId]?.firstOrNull { marker ->
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    latLng.latitude, latLng.longitude,
                    marker.position.latitude, marker.position.longitude,
                    distance
                )
                distance[0] < 50
            }

            if (markerInRange != null) {
                AlertDialog.Builder(this)
                    .setTitle("KONFIRMASI")
                    .setMessage("Apakah anda yakin ingin menyimpan?")
                    .setPositiveButton("OK") { _, _ ->
                        saveUpdatedPolygon(polygon, tambangId)

                        polygonMarkers[tambangId]?.forEach { marker ->
                            marker.isVisible = false
                            marker.isDraggable = false
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }


    private fun updatePolygonShape(polygon: Polygon, tambangId: String) {
        val markers = polygonMarkers[tambangId] ?: return
        polygon.points = markers.map { it.position }
    }

    private fun saveUpdatedPolygon(polygon: Polygon, tambangId: String) {
        val points = polygon.points.map { LatLngData(it.latitude, it.longitude) }
        tambangRef.child(tambangId).child("polygonPoints").setValue(points)
    }

    private fun hidePolygonVertices(tambangId: String) {
        polygonMarkers[tambangId]?.forEach { marker ->
            marker.isVisible = false
            marker.isDraggable = false
        }
    }
    private fun fetchOSRMRoute(origin: LatLng, destination: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Format coordinates for OSRM (note: OSRM uses longitude,latitude order)
                val coordinates = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
                val url = "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=polyline"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonResponse = JSONObject(response.toString())
                val routes = jsonResponse.getJSONArray("routes")

                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.getString("geometry")
                    val distance = route.getDouble("distance")
                    val duration = route.getDouble("duration")

                    withContext(Dispatchers.Main) {
                        // Draw the route and show route information
                        val decodedPath = decodePoly(geometry)
                        drawRoute(decodedPath)

                        // Show route information in a dialog
                        val distanceKm = String.format("%.2f", distance / 1000)
                        val durationMin = String.format("%.0f", duration / 60)

                        AlertDialog.Builder(this@maps)
                            .setTitle("Informasi Rute")
                            .setMessage("Jarak: $distanceKm km\nWaktu tempuh: $durationMin menit")
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Navigasi") { _, _ ->
                                openGoogleMapsNavigation(destination)
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@maps, "Error fetching route: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun drawRoute(decodedPath: List<LatLng>) {
        // Remove existing route if any
        currentRoute?.remove()

        // Draw new route
        currentRoute = gMap.addPolyline(
            PolylineOptions()
                .addAll(decodedPath)
                .width(10f)
                .color(Color.BLUE)
                .geodesic(true)
        )

        // Zoom to show the entire route
        val builder = LatLngBounds.Builder()
        decodedPath.forEach { builder.include(it) }
        val bounds = builder.build()
        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun openGoogleMapsNavigation(destination: LatLng) {
        val intentUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawPolygon() {
        // Polygon 1
        arrayPoly1.apply {
            clear()
            add(LatLng(-7.388939128833573, 112.21985479770308))
            add(LatLng(-7.389507147803895, 112.21987755700154))
            add(LatLng(-7.38943567525839, 112.22051481735828))
            add(LatLng(-7.38876232807788, 112.2204389530301))
            add(LatLng(-7.388939128833573, 112.21985479770308))
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

    private fun moveCameraToLocation(location: LatLng) {
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
    }

    private fun moveCameraToPolygon() {
        val boundsBuilder = LatLngBounds.Builder()
        for (point in arrayPoly1) {
            boundsBuilder.include(point)
        }
        val bounds = boundsBuilder.build()
        gMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
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
        val marker = gMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        marker?.let { markerList.add(it) }

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
            // Hapus semua marker
            markerList.forEach { it.remove() }
            markerList.clear()

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

        // Add markers for each polygon vertex
        val markers = points.map { point ->
            gMap.addMarker(MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .draggable(true)
                .visible(false)) // Initially hidden
        }.filterNotNull().toMutableList()

        polygon.tag = tambang.id
        polygonsList.add(polygon)
        polygonMarkers[tambang.id] = markers
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
    private fun showRoute(origin: LatLng, destination: LatLng) {
        // Clear existing route
        currentRoute?.remove()

        // Draw a simple direct route
        currentRoute = gMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(5f)
                .color(Color.BLUE)
                .geodesic(true)
        )

        // Open Google Maps for navigation
        val intentUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

        private fun LatLngData.toLatLng(): LatLng {
            return LatLng(this.latitude, this.longitude)
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