package com.usaclean.frenchconnectionuser.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapsResults(
    @SerializedName("geocoded_waypoints") val geocodedWayPoints: List<GeocodedWaypoint>,
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("status") val status: String
) : Parcelable {
    @Parcelize
    data class GeocodedWaypoint(
        val geocoder_status: String,
        val place_id: String,
        val types: List<String>
    ) : Parcelable

    @Parcelize
    data class Route(
        val bounds: Bounds,
        val copyrights: String,
        val legs: List<Leg>,
        val overview_polyline: OverviewPolyline,
        val summary: String
    ) : Parcelable {
        @Parcelize
        data class Bounds(
            val northeast: Northeast,
            val southwest: Southwest
        ) : Parcelable

        @Parcelize
        data class Leg(
            val distance: Distance,
            val duration: Duration,
            val end_address: String,
            val end_location: EndLocation,
            val start_address: String,
            val start_location: StartLocation,
            val steps: List<Step>
        ) : Parcelable

        @Parcelize
        data class Step(
            val distance: Distance,
            val duration: Duration,
            val end_location: EndLocation,
            val html_instructions: String,
            val maneuver: String,
            val polyline: Polyline,
            val start_location: StartLocation,
            val travel_mode: String
        ) : Parcelable

        @Parcelize
        data class Polyline(
            val points: String
        ) : Parcelable

        @Parcelize
        data class OverviewPolyline(
            val points: String
        ) : Parcelable

        @Parcelize
        data class StartLocation(
            val lat: Double,
            val lng: Double
        ) : Parcelable

        @Parcelize
        data class Northeast(
            val lat: Double,
            val lng: Double
        ) : Parcelable

        @Parcelize
        data class Southwest(
            val lat: Double,
            val lng: Double
        ) : Parcelable

        @Parcelize
        data class Distance(
            val text: String,
            val value: Int
        ) : Parcelable

        @Parcelize
        data class EndLocation(
            val lat: Double,
            val lng: Double
        ) : Parcelable

        @Parcelize
        data class Duration(
            val text: String,
            val value: Int
        ) : Parcelable
    }
}
