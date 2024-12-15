package com.polinema.mi.app_maps.fragment

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Laporan(
    @PropertyName("kodeLaporan")
    var kodeLaporan: String = "",

    @PropertyName("namaPt")
    var namaPt: String = "",

    @PropertyName("tanggal")
    var tanggal: String = "",

    @PropertyName("userId")
    var userId: String = "",

    @PropertyName("status")
    var status: String? = null,

    @PropertyName("ritase")
    var ritase: Long? = null,

    @PropertyName("kubikasi")
    var kubikasi: Long? = null,

    @PropertyName("foto_surat_jalan")
    var foto_surat_jalan: String? = null
) {
    // Convert status to Int if it's a string or number
    val statusInt: Int
        get() = when (status) {
            is String -> (status as String).toIntOrNull() ?: 0
            else -> 0
        }

    // Converter methods for ritase and kubikasi
    val ritaseInt: Int
        get() = ritase?.toInt() ?: 0

    val kubikasiInt: Int
        get() = kubikasi?.toInt() ?: 0
}