package com.polinema.mi.app_maps.fragment

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityTambahLaporanBinding
import java.util.Calendar

class TambahLaporanActivity : Fragment(), View.OnClickListener {

    private lateinit var b : ActivityTambahLaporanBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityTambahLaporanBinding.inflate(layoutInflater)
        v = b.root

        b.btnKembali.setOnClickListener(this)
        b.btnPilihFoto.setOnClickListener(this)
        b.btnDatePicker.setOnClickListener(this)

        setupPtSpinner()

        return v
    }

    private fun setupPtSpinner() {
        val categories = listOf("PT. GARUDA", "PT. ELANG")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spNamaPt.adapter = adapter
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
}