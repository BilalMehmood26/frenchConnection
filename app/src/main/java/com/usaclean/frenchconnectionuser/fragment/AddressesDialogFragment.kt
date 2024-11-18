package com.usaclean.frenchconnectionuser.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.button.MaterialButton
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.utils.LocationUtility
import java.util.Locale

class AddressesDialogFragment(val addressClick: (String, String, String, LatLng) -> Unit) :
    DialogFragment(), OnMapReadyCallback {

    private var myGoogleMap: GoogleMap? = null
    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment
    private lateinit var locationUtility: LocationUtility
    val REQUEST_CODE = 1000
    private var userLat = 0.0
    private var userLng = 0.0
    private lateinit var confirmBtn :TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_addresses_dialog, container, false)

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_CODE
            )
        }
        confirmBtn = view.findViewById(R.id.confirm_btn)
        locationUtility = LocationUtility(requireContext())

        Places.initialize(requireContext(), getString(R.string.api_key))
        autocompleteSupportFragment =
            (childFragmentManager.findFragmentById(R.id.auto_complete_places) as AutocompleteSupportFragment)!!
        autocompleteSupportFragment.apply {
            setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
                )
            )
            setCountries(listOf("US"))
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onError(p0: Status) {
                    Toast.makeText(
                        requireContext(),
                        p0.statusMessage.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("statusMsg", "onError: ${p0.statusMessage}")
                }

                override fun onPlaceSelected(p0: Place) {

                    // Extracting additional address components
                    val addressComponents = p0.addressComponents?.asList() ?: emptyList()
                    var streetAddress = ""
                    var city = ""
                    var postalCode = ""

                    for (component in addressComponents) {
                        when {
                            component.types.contains("street_number") -> {
                                streetAddress = component.name
                            }

                            component.types.contains("route") -> {
                                streetAddress = if (streetAddress.isEmpty()) {
                                    component.name
                                } else {
                                    "$streetAddress ${component.name}"
                                }
                            }

                            component.types.contains("locality") -> {
                                city = component.name
                            }

                            component.types.contains("postal_code") -> {
                                postalCode = component.name
                            }
                        }
                    }
                    myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.latLng, 14f))
                }
            })
        }

        location()
        return view
    }

    private fun location() {
        locationUtility.requestLocationUpdates { currentLocation ->
            userLat = currentLocation.latitude
            userLng = currentLocation.longitude
            if (isAdded){
                val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
                mapFragment?.getMapAsync(this)
            }
            locationUtility.removeLocationUpdates()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onMapReady(googelMaps: GoogleMap) {
        myGoogleMap = googelMaps
        val latLng = LatLng(userLat, userLng)
        myGoogleMap?.setOnMapClickListener { latLng ->
            // Clear previous markers
            myGoogleMap?.clear()
            myGoogleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val fullAddress = address.getAddressLine(0) // Full address
                    val city = address.locality
                    val postalCode = address.postalCode // Postal code
                    val latitude = address.latitude
                    val longitude = address.longitude
                    if(city.equals("New Orleans")){
                        confirmBtn.setOnClickListener {
                            addressClick.invoke(fullAddress, city, postalCode,latLng)
                            dismiss()
                        }
                    }else{
                        Toast.makeText(requireContext(), "State not matched", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
    }
}