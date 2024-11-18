package com.usaclean.frenchconnectionuser.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.PolyUtil
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ActivityDashboardBinding
import com.usaclean.frenchconnectionuser.databinding.ActivityYourRideBinding
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.model.MapsResults
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.RetrofitClient
import com.usaclean.frenchconnectionuser.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class YourRideActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityYourRideBinding by lazy {
        ActivityYourRideBinding.inflate(layoutInflater)
    }


    private var rideID: String? = ""
    private var riderUID: String? = ""
    private val db = Firebase.firestore
    private lateinit var mMap: GoogleMap

    private var pickUpAddress = ""
    private var myGoogleMap: GoogleMap? = null
    private var pickUpLat = 0.0
    private var pickUpLng = 0.0
    private var price = 0.0
    private var dropOffAddress = ""
    private var driverPhoneNumber = ""
    private var driverId: String? = ""
    private var driverName = "--"
    private var driverVehicle: String? = "--"
    private var driverRating = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        binding.apply {

            pickUpAddress = intent.getStringExtra("pickUpAddress")!!
            dropOffAddress = intent.getStringExtra("dropOffAddress")!!
            pickUpLat = intent.getDoubleExtra("pickUpLat", 0.0)
            pickUpLng = intent.getDoubleExtra("pickUpLng", 0.0)
            rideID = intent.getStringExtra("rideID")
            driverVehicle = intent.getStringExtra("carType")

            pickUpTv.text = pickUpAddress
            dropOffTv.text = dropOffAddress

            getOrders()

            markCompleteBtn.setOnClickListener {
                updateStatus(rideID!!, "dispute")
            }

            msgBtn.setOnClickListener {
                startMessage()
            }

            phoneBtn.setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:$driverPhoneNumber")
                if (driverPhoneNumber.isNotEmpty()) {
                    startActivity(dialIntent)
                } else {
                    Toast.makeText(
                        this@YourRideActivity,
                        "Phone Number Not Available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startMessage() {
        val timeStamp = System.currentTimeMillis()

        val messageMap = hashMapOf(
            "fromID" to UserSession.user.id,
            "toID" to driverId,
            "messageId" to rideID,
            "timestamp" to timeStamp,
            "type" to "text"
        )

        val participants = hashMapOf(
            Firebase.auth.currentUser!!.uid to true,
            driverId to true
        )

        val lastMessageMap = hashMapOf(
            "lastMessage" to messageMap,
            "participants" to participants,
            "chatType" to "one",
            "carType" to driverVehicle
        )

        binding.progressBar.visibility = View.VISIBLE
        FirebaseFirestore.getInstance().collection("Chat").document(rideID!!).set(lastMessageMap)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val conversationID = UUID.randomUUID().toString()
                    FirebaseFirestore.getInstance().collection("Chat").document(rideID!!)
                        .collection("Conversation").document(conversationID).set(messageMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.progressBar.visibility = View.GONE
                                val intent = Intent(this, ConversationActivity::class.java)
                                intent.putExtra("driverID", driverId)
                                intent.putExtra("messageId", rideID)
                                startActivity(intent)
                                overridePendingTransition(
                                    androidx.appcompat.R.anim.abc_fade_in,
                                    androidx.appcompat.R.anim.abc_fade_out
                                )
                            } else {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(
                                    this,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(this, it.exception!!.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    binding.progressBar.visibility = View.GONE
                }
            }
    }

    private fun getRoute(currentLatLng: LatLng, destLatLng: LatLng) {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.getDirection(
            "driving",
            "${currentLatLng.latitude},${currentLatLng.longitude}",
            "${destLatLng.latitude},${destLatLng.longitude}",
            getString(R.string.GOOGLE_BROWSER_API_KEY)
        ).enqueue(object : Callback<MapsResults> {
            override fun onResponse(p0: Call<MapsResults>, response: Response<MapsResults>) {
                if (response.isSuccessful) {
                    binding.progressBar.visibility = View.GONE
                    val route = response.body()!!.routes.firstOrNull()
                    val polylinePoints = route?.overview_polyline?.points ?: return
                    val decodedPath = PolyUtil.decode(polylinePoints)
                    val polylineOptions = PolylineOptions()
                        .addAll(decodedPath)
                        .width(7f)
                        .color(
                            ContextCompat.getColor(
                                this@YourRideActivity,
                                R.color.gradient_3
                            )
                        )
                    mMap.addPolyline(polylineOptions)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@YourRideActivity,
                        "esle ${response.body()?.status}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(p0: Call<MapsResults>, p1: Throwable) {
                Toast.makeText(
                    this@YourRideActivity,
                    p1.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setIcon(context: Activity, drawableID: Int): BitmapDescriptor {

        val drawable = ActivityCompat.getDrawable(context, drawableID)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getOrders() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("Bookings").document(rideID!!).addSnapshotListener { value, error ->
            if (error != null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@YourRideActivity,
                    error.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val ride = value?.toObject(Booking::class.java)
            if (ride != null) {
                binding.apply {
                    when (ride.status) {
                        "driverAccepted", "driverReached", "rideStarted" -> {
                            markCompleteBtn.visibility = View.GONE
                            destinationLayout.visibility = View.VISIBLE
                            driverId = ride.driverId
                            Log.d("driverID", "onCreate: $driverId $rideID")
                            getDriverInfo(ride.driverId!!)
                        }

                        "booked" -> {
                            binding.progressBar.visibility = View.GONE
                            destinationLayout.visibility = View.GONE
                            markCompleteBtn.visibility = View.VISIBLE
                        }

                        "rideCompleted" -> {
                            val intent =
                                Intent(this@YourRideActivity, YourDestinationActivity::class.java)
                            intent.putExtra("price", price)
                            intent.putExtra("bookingDate", formatDateTime(ride.bookingDate!!))
                            intent.putExtra("status", ride.status)
                            intent.putExtra("driverName", driverName)
                            intent.putExtra("driverRating", driverRating)
                            intent.putExtra("rideID", rideID)
                            startActivity(intent)
                            overridePendingTransition(
                                androidx.appcompat.R.anim.abc_fade_in,
                                androidx.appcompat.R.anim.abc_fade_out
                            )

                        }
                    }

                    price = ride.price!!.toDouble()
                    timeDateTv.text = formatDateTime(ride.bookingDate!!)
                    priceTv.text = "$ ${ride.price}"
                    statusTv.text = ride.status
                    driverName = "---"
                    driverRating = "0"
                    nameTv.text = driverName
                    ratingTv.text = driverRating
                    /*  lat = ride.driverLat
                      lng = ride.driverLng*/
                }
            } else {
                binding.markCompleteBtn.visibility = View.GONE
            }
        }
    }

    private fun getDriverInfo(userID: String) {
        db.collection("Users").document(userID).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.progressBar.visibility = View.GONE
                val user = task.result.toObject(User::class.java)
                binding.nameTv.text = user!!.userName
                if (user.image!!.isNotEmpty()&& !isDestroyed) {
                    Glide.with(this).load(user.image).into(binding.profileIV)
                } else {
                    binding.profileIV.setImageResource(R.drawable.main_logo)
                }
                binding.nameTv.text = user.userName
                driverName = user.userName!!
                driverPhoneNumber = user.phoneNumber!!
                driverRating = user.totalRating.toString()
                binding.ratingTv.text = user.totalRating.toString()
                riderUID = user.id
            } else {
                Log.d("LOGGER", "is Fail")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    task.exception!!.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateStatus(rideID: String, status: String) {
        binding.progressBar.visibility = View.VISIBLE
        val updateStatus = hashMapOf(
            "status" to status
        )

        db.collection("Bookings").document(rideID).update(updateStatus as Map<String, Any>)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                if (status.equals("dispute")) {
                    finish()
                }
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@YourRideActivity,
                    it.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }

        if (status.equals("rideCompleted")) {
            binding.progressBar.visibility = View.VISIBLE
            val payout = hashMapOf(
                "amount" to price,
                "completionTimeStamp" to System.currentTimeMillis(),
                "driverId" to price,
                "orderId" to rideID,
                "status" to riderUID,
                "type" to "order",
            )
            db.collection("Payouts").document().set(payout).addOnSuccessListener {
                Toast.makeText(this, "Ride Completed", Toast.LENGTH_SHORT).show()
                updateNotification("Ride Complete","Thank you for booking with French connection!","rideCompleted")
                binding.progressBar.visibility = View.GONE
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googelMaps: GoogleMap) {
        myGoogleMap = googelMaps
        //mMap = googelMaps
        val latLng = LatLng(pickUpLat, pickUpLng)

        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        myGoogleMap?.addMarker(
            MarkerOptions().icon(setIcon(this, R.drawable.ic_car)).position(latLng)
        )
        /* myGoogleMap?.setOnMapClickListener { latLng ->
             myGoogleMap?.clear()
             myGoogleMap?.addMarker(MarkerOptions().icon(setIcon(this, R.drawable.ic_car)).position(latLng).title("Selected Location"))
         }*/
        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        //mMap = googleMaps
        /*  if (ActivityCompat.checkSelfPermission(
                  this,
                  Manifest.permission.ACCESS_FINE_LOCATION
              ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                  this,
                  Manifest.permission.ACCESS_COARSE_LOCATION
              ) != PackageManager.PERMISSION_GRANTED
          ) {
              return
          }
          mMap.isMyLocationEnabled = true
          val latLng = LatLng(pickUpLat!!, pickUpLng!!)
          val zoomLevel = 15f
          mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
          mMap.addMarker(MarkerOptions().icon(setIcon(this, R.drawable.ic_mark)).position(latLng))*/
    }

    private fun updateNotification(title: String, msg: String, status: String) {
        binding.progressBar.visibility = View.VISIBLE
        val isRead = hashMapOf(
            Firebase.auth.currentUser!!.uid to false,
            driverId to false,
        )
        val notification = hashMapOf(
            "driverId" to driverId,
            "message" to msg,
            "orderId" to rideID,
            "isRead" to isRead,
            "timestamp" to System.currentTimeMillis().toString(),
            "title" to title,
            "type" to status,
            "userId" to Firebase.auth.currentUser!!.uid,
        )
        db.collection("Notification").document().set(notification).addOnSuccessListener {
            binding.progressBar.visibility = View.GONE
            finish()
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH)
        val date = Date(millis)
        return formatter.format(date)
    }
}