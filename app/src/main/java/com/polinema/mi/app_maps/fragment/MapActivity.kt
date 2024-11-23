package com.polinema.mi.app_maps.fragment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.polinema.mi.app_maps.BaseActivity
import com.polinema.mi.app_maps.R
import com.polinema.mi.app_maps.databinding.ActivityDashboardBinding
import com.polinema.mi.app_maps.databinding.ActivityMapBinding

class MapActivity : Fragment() {
    private lateinit var b : ActivityMapBinding
    lateinit var thisParent: BaseActivity
    lateinit var v: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisParent = activity as BaseActivity
        b = ActivityMapBinding.inflate(layoutInflater)
        v = b.root

        return v
    }
}