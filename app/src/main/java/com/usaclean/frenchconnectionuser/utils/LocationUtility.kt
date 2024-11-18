package com.usaclean.frenchconnectionuser.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationUtility (private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(callback: (Location) -> Unit) {
        val localCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { currentLocation ->
                    callback(currentLocation)
                }
            }
        }

        locationCallback = localCallback

        if (checkLocationPermission()) {
            val locationRequest = LocationRequest.create()
                .setInterval(1000) // Request updates every 1 second
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            fusedLocationClient.requestLocationUpdates(locationRequest, localCallback, Looper.getMainLooper())
        }
    }


    fun removeLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}

fun postDelay(action: () -> Unit, ms: Long) {
    Handler(Looper.myLooper()!!).postDelayed({
        action.invoke()
    }, ms)
}
