package com.polinema.mi.app_maps.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityDashboardBinding

class DashboardActivity : Fragment() {

    private lateinit var b : ActivityDashboardBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityDashboardBinding.inflate(layoutInflater)
        v = b.root

        b.btnTambahLaporan.setOnClickListener {
            val fragment = TambahLaporanActivity()
            val fragmentManager = parentFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.frameLayout, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return v
    }
}