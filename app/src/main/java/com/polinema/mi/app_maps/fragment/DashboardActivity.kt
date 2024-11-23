package com.polinema.mi.app_maps.fragment

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
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

        return v
    }
}