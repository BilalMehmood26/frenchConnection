package com.usaclean.frenchconnectionuser.model

import android.os.Parcelable
import android.view.ViewDebug.IntToString
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class Prices(
    val double: Map<String, Int> = emptyMap(),
    val doubleOver: Map<String, Int> = emptyMap(),
    val endHour: Int = 0,
    val iRide: IRide = IRide(),
    val single: Map<String, Int> = emptyMap(),
    val singleOver: Map<String, Int> = emptyMap(),
    val startHour: Int = 0
): Parcelable {
    @Parcelize
    data class IRide(
        val costOfVehicle: String = "",
        val initialFee: String = "",
        val isActive: Boolean = false,
        val name: String = "",
        val pricePerMile: String = "",
        val pricePerMin: String = ""
    ): Parcelable
}

