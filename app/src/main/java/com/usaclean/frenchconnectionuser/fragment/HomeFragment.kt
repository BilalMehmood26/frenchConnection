package com.usaclean.frenchconnectionuser.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.activities.YourRideActivity
import com.usaclean.frenchconnectionuser.adapter.CardListAdapter
import com.usaclean.frenchconnectionuser.databinding.FragmentHomeBinding
import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.model.RideStatus
import com.usaclean.frenchconnectionuser.stripe.Controller
import com.usaclean.frenchconnectionuser.stripe.repo.PaymentViewModel
import com.usaclean.frenchconnectionuser.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt


class HomeFragment : Fragment() {


    private val binding: FragmentHomeBinding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }


    private val memberList = arrayListOf("1", "2", "3", "4", "5", "6", "7")
    private var numberOfMembers = 1
    private var totalFair = 10.00
    private var returnWay = "OneWay"
    private lateinit var fragmentContext: Context
    private var isBooked = false
    private lateinit var addressesDialogFragment: AddressesDialogFragment

    private var cardList: ArrayList<PaymentMethodsResponse.PaymentMethod> = ArrayList()

    private var carType: String = ""
    private var cardType: String = ""
    private var destAddress: String = ""
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var distance: Double = 0.0
    private var destStatus: String = ""
    private var pickUpAddress: String = "0"
    private var pickUpStatus: String = "0"
    private var pickUpLat: Double = 0.0
    private var pickUpLng: Double = 0.0
    private val db = Firebase.firestore

    private var secrat = ""
    private var paymentMethod = ""
    lateinit var stripe: Stripe


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.apply {

            userNameTv.text = UserSession.user.userName
            if (UserSession.user.image.equals("")) {
                profileIv.setImageResource(R.drawable.dummy_profile)
            } else {
                Glide.with(requireActivity()).load(UserSession.user.image).into(profileIv)
            }
            parajeID.setItems(memberList)
            parajeID.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newText ->
                val pharmacy = memberList[newIndex]
                numberOfMembers = newIndex
                parajeID.setText(pharmacy)
            }
            stripe = Stripe(
                fragmentContext,
                "pk_test_51PgBExCo08Oa4W8HRRlISwH7IOZRW42joDX0KpJRo7RK4tZhrz29Cout7tSsBEWCeODsr7IhT8jQGNiUrMIwwR0h00jZcUoUkr"
            )
            PaymentConfiguration.init(
                fragmentContext,
                "pk_test_51PgBExCo08Oa4W8HRRlISwH7IOZRW42joDX0KpJRo7RK4tZhrz29Cout7tSsBEWCeODsr7IhT8jQGNiUrMIwwR0h00jZcUoUkr"
            )

            if (UserSession.user.stripeCustid == "") {
                createCustomer(UserSession.user.email!!, UserSession.user.id!!)
            } else {
                createFirstStep(UserSession.user.stripeCustid!!)
                Log.d("Logger", "onCreateView: ${UserSession.user.stripeCustid}")
            }

            getBooking()
            setListener()
            getCardList(UserSession.user.stripeCustid!!)

        }
        return binding.root
    }

    private fun createCustomer(email: String, id: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("email" to email, "userId" to id)
        Controller.instance.createCustomer(body).enqueue(object : Callback<CustomerResponse> {
            override fun onResponse(
                call: Call<CustomerResponse>,
                response: Response<CustomerResponse>
            ) {
                if (response.isSuccessful) {
                    updateUserDetails(response.body()!!.cust_id, response.body()!!.account_id)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.d("Logger", "onResponse: ${response.errorBody()!!.string()}")
                    Toast.makeText(
                        fragmentContext,
                        response.errorBody()!!.string(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<CustomerResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(fragmentContext, t.message.toString(), Toast.LENGTH_SHORT).show()

            }
        })
    }

    private fun updateUserDetails(custId: String, stripeAccountId: String) {
        Log.d("LOGGER", "Create Response: ${custId.toString()}")
        db.collection("Users").document(UserSession.user.id!!)
            .update("stripeCustid", custId, "stripeaccount_id", stripeAccountId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.progressBar.visibility = View.GONE
                    UserSession.user.stripeCustid = custId
                    UserSession.user.stripeaccount_id = stripeAccountId
                    createFirstStep(custId)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        fragmentContext,
                        task.exception!!.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun createFirstStep(stripeCustId: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("cus_id" to stripeCustId)
        Controller.instance.getFirstStep(body).enqueue(object : Callback<FirstStepResponse> {
            override fun onResponse(
                call: Call<FirstStepResponse>,
                response: Response<FirstStepResponse>
            ) {
                if (response.isSuccessful) {
                    if (response.body()?.key != null) {
                        binding.progressBar.visibility = View.GONE
                        secrat = response.body()!!.key
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.d("logger", "onResponse: ${response.errorBody()!!.string()}")
                }
            }

            override fun onFailure(call: Call<FirstStepResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(fragmentContext, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun addCard(
        carType: String,
        cardNumber: String,
        expiryMonth: String,
        expiryYear: String,
        cvc: String,
        postalCode: String
    ) {

        val cardParams = CardParams(
            cardNumber,
            expiryMonth.toInt(),
            expiryYear.toInt(),
            cvc,
            "${UserSession.user.userName}"
        )

        Log.d("LOGGER", "Secrate : $secrat")
        val paymentMethodCreateParams = PaymentMethodCreateParams.createCard(cardParams)
        val paymentIntentParams = ConfirmSetupIntentParams.create(paymentMethodCreateParams, secrat)
        val setupIntent = stripe.confirmSetupIntentSynchronous(paymentIntentParams)

        if (setupIntent.status == StripeIntent.Status.Succeeded) {
            Log.d("LOGGER", "Customer Key: ${UserSession.user.stripeCustid}")
            Log.d("LOGGER", "Payment Intent Key: ${setupIntent.paymentMethodId.toString()}")
            getPaymentIntent(
                carType,
                1000,
                UserSession.user.stripeCustid,
                setupIntent.paymentMethodId.toString()
            )
        }
    }

    private fun getPaymentIntent(
        carType: String,
        amount: Int,
        stripeCustId: String?,
        paymentMethodId: String
    ) {
        val body = AccountPaymentRequest(stripeCustId!!, amount.toString(), paymentMethodId)

        Controller.instance.createPaymentIntent(body)
            .enqueue(object : Callback<PaymentIntentResponse> {
                override fun onResponse(
                    call: Call<PaymentIntentResponse>,
                    response: Response<PaymentIntentResponse>
                ) {

                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE
                        addBooking(
                            carType,
                            totalFair.toString(),
                            destAddress,
                            destLat,
                            destLng,
                            destStatus,
                            pickUpAddress,
                            pickUpLat,
                            pickUpLng,
                            pickUpStatus
                        )
                        getCardList(UserSession.user.stripeCustid!!)
                        Log.d("Logger", "TranferInfo: ${response.body()?.transferinfo}")
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            fragmentContext,
                            response.errorBody()!!.string(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                override fun onFailure(call: Call<PaymentIntentResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(fragmentContext, t.message.toString(), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getCardList(cusId: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("cus_id" to cusId)
        Controller.instance.cardList(body).enqueue(object : Callback<PaymentMethodsResponse> {
            override fun onResponse(
                call: Call<PaymentMethodsResponse>,
                response: Response<PaymentMethodsResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.paymentMethods?.forEach {
                        cardList.add(it)
                    }
                    Log.d("Logger", "onResponse: ${cardList.size}")
                    binding.progressBar.visibility = View.GONE

                } else {
                    Toast.makeText(
                        fragmentContext,
                        response.errorBody()!!.string(),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<PaymentMethodsResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(fragmentContext, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun createRide() {

        val fareMap = mapOf(
            "OneWay" to listOf(10.00, 18.00, 24.00, 28.00, 40.00, 50.00, 54.00),
            "Return" to listOf(25.00, 30.00, 40.00, 50.00, 60.00, 90.00, 100.00)
        )
        totalFair = fareMap[returnWay]!!.getOrNull(numberOfMembers) ?: 0.0

        showCustomDialog()
    }

    private fun setListener() {

        binding.runningRide.setOnClickListener {
            binding.runningRide.setBackgroundResource(R.drawable.gradient_button)
            binding.completeRide.setBackgroundResource(0)
            returnWay = "OneWay"

        }

        binding.completeRide.setOnClickListener {
            binding.completeRide.setBackgroundResource(R.drawable.gradient_button)
            binding.runningRide.setBackgroundResource(0)
            returnWay = "Return"
        }

        binding.apply {
            pickUpLoc.setOnClickListener {
                addressesDialogFragment =
                    AddressesDialogFragment { address, city, postalCode, latlng ->
                        pickUpAddress = address
                        pickUpLat = latlng.latitude
                        pickUpLng = latlng.longitude
                        yourLocation.setText("$address, $city")
                    }
                addressesDialogFragment.show(childFragmentManager, "Pickup Location")
            }

            dropOffLoc.setOnClickListener {
                addressesDialogFragment =
                    AddressesDialogFragment { address, city, postalCode, latlng ->
                        destAddress = address
                        destLat = latlng.latitude
                        destLng = latlng.longitude
                        dropOffTv.setText("$address, $city")
                    }
                addressesDialogFragment.show(childFragmentManager, "Dropoff Location")
            }

            bookRideBtn.setOnClickListener {
                val destLatLng = isWithinFiveMiles(pickUpLat, pickUpLng, destLat, destLng)
                when {
                    pickUpAddress.isEmpty() -> Toast.makeText(
                        fragmentContext,
                        "Pickup Address is Empty",
                        Toast.LENGTH_SHORT
                    ).show()

                    destAddress.isEmpty() -> Toast.makeText(
                        fragmentContext,
                        "Drop Off Address is Empty",
                        Toast.LENGTH_SHORT
                    ).show()

                    destLatLng.not() -> Toast.makeText(
                        fragmentContext,
                        "Sorry FC does not travel out of a 5 mile radius",
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> createRide()
                }
            }
        }
    }


    private fun getBooking() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("Bookings").addSnapshotListener { value, error ->

            if (error != null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(fragmentContext, error.message.toString(), Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            binding.progressBar.visibility = View.GONE
            value?.forEach {
                val booking = it.toObject(Booking::class.java)
                if (booking.userId.equals(Firebase.auth.currentUser!!.uid)) {

                    when (booking.status) {
                        "booked", "driverAccepted", "driverReached", "rideStarted", "rideCompleted" -> {
                            val destinationsList = mutableListOf<Booking.DropOff>()
                            val destinations = it["destinations"] as? List<HashMap<String, Any>>
                            destinations?.forEach { destinationMap ->
                                val dropOff = Booking.DropOff(
                                    address = destinationMap["address"] as? String ?: "",
                                    lat = destinationMap["lat"] as? Double ?: 0.0,
                                    lng = destinationMap["lng"] as? Double ?: 0.0
                                )
                                destinationsList.add(dropOff)
                            }

                            val updatedBooking = booking.copy(destinations = destinationsList)

                            if (isBooked.not()) {
                                isBooked = true
                                Log.d("booking", "getBooking: $isBooked")
                                val intent = Intent(fragmentContext, YourRideActivity::class.java)
                                intent.putExtra("pickUpLat", updatedBooking.pickUp!!.lat)
                                intent.putExtra("pickUpLng", updatedBooking.pickUp.lng)
                                intent.putExtra("pickUpAddress", updatedBooking.pickUp.address)
                                intent.putExtra("carType", updatedBooking.carType)
                                intent.putExtra(
                                    "dropOffAddress",
                                    updatedBooking.destinations[0].address
                                )
                                intent.putExtra("driverID", updatedBooking.driverId)
                                intent.putExtra("rideID", updatedBooking.id)
                                fragmentContext.startActivity(intent)
                            } else {
                                Log.d("booking", "getBooking: $isBooked")
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showCustomDialog() {
        val dialogView =
            LayoutInflater.from(fragmentContext).inflate(R.layout.dialog_select_car_type, null)
        val dialogBuilder = AlertDialog.Builder(fragmentContext)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val crawFish = dialogView.findViewById<RadioButton>(R.id.craw_fish_radio)

        dialogView.findViewById<TextView>(R.id.your_proceed_btn).setOnClickListener {
            if (crawFish.isChecked) {
                carType = "Muk's CrawFish Cruiser"
            } else {
                carType = "Pirate Black Caesar"
            }

            when {
                carType.isEmpty() -> Toast.makeText(
                    fragmentContext,
                    "Car type is not selected.",
                    Toast.LENGTH_SHORT
                ).show()

                else -> {
                    showPaymentDialog(carType)
                    Log.d("LOGGER", "showCustomDialog: $carType")
                    dialog.dismiss()
                }
            }

        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showCardDialog(carType: String, cardType: String) {
        val dialogView =
            LayoutInflater.from(fragmentContext).inflate(R.layout.dialog_card, null)
        val dialogBuilder = AlertDialog.Builder(fragmentContext)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val fair = dialogView.findViewById<TextView>(R.id.fair_tv)
        fair.setText("$ $totalFair")
        dialogView.findViewById<TextView>(R.id.add_card_btn).setOnClickListener {
            addCardDialog(carType, cardType)
            dialog.dismiss()
        }

        val cardListRv = dialogView.findViewById<RecyclerView>(R.id.card_list_rv)

        cardListRv.layoutManager = LinearLayoutManager(fragmentContext)
        cardListRv.adapter = CardListAdapter(fragmentContext, cardList) {
            addBooking(
                carType,
                totalFair.toString(),
                destAddress,
                destLat,
                destLng,
                destStatus,
                pickUpAddress,
                pickUpLat,
                pickUpLng,
                pickUpStatus
            )
            dialog.dismiss()
        }
    }

    private fun addCardDialog(carType: String, cardType: String) {
        val dialogView =
            LayoutInflater.from(fragmentContext).inflate(R.layout.dialog_add_card, null)
        val dialogBuilder = AlertDialog.Builder(fragmentContext)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<TextView>(R.id.add_card_btn).setOnClickListener {
            dialog.dismiss()
            val cardNumber = dialog.findViewById<EditText>(R.id.card_number_et)
            val month = dialog.findViewById<EditText>(R.id.month_et)
            val year = dialog.findViewById<EditText>(R.id.year_et)
            val cvc = dialog.findViewById<EditText>(R.id.cvc_et)
            val postalCode = dialog.findViewById<EditText>(R.id.post_code_et)

            when {
                cardNumber.text.toString().isEmpty() -> cardNumber.error = "Required"
                month.text.toString().isEmpty() -> month.error = "Required"
                year.text.toString().isEmpty() -> year.error = "Required"
                cvc.text.toString().isEmpty() -> cvc.error = "Required"
                postalCode.text.toString().isEmpty() -> postalCode.error = "Required"
                else -> addCard(
                    carType,
                    cardNumber.text.toString(),
                    month.text.toString(),
                    year.text.toString(),
                    cvc.text.toString(),
                    postalCode.text.toString()
                )
            }
        }
    }

    private fun isWithinFiveMiles(
        pickupLat: Double, pickupLng: Double,
        dropOffLat: Double, dropOffLng: Double
    ): Boolean {
        val distanceInMiles = calculateDistanceInMiles(pickupLat, pickupLng, dropOffLat, dropOffLng)
        distance = round(distanceInMiles)
        return distanceInMiles <= 5.0
    }

    private fun calculateDistanceInMiles(
        pickupLat: Double, pickupLng: Double,
        dropOffLat: Double, dropOffLng: Double
    ): Double {
        val earthRadiusMiles = 3958.8

        val dLat = Math.toRadians(dropOffLat - pickupLat)
        val dLng = Math.toRadians(dropOffLng - pickupLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(pickupLat)) * cos(Math.toRadians(dropOffLat)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMiles * c
    }

    private fun addBooking(
        carType: String,
        price: String,
        destAddress: String,
        destLat: Double,
        destLng: Double,
        destStatus: String,
        address: String,
        lat: Double,
        lng: Double,
        status: String
    ) {

        binding.progressBar.visibility = View.VISIBLE
        val destinations = hashMapOf<String, Any>(
            "address" to destAddress,
            "lat" to destLat,
            "lng" to destLng,
            "status" to destStatus
        )
        val destinationsList = arrayListOf<HashMap<String, Any>>()
        destinationsList.add(destinations)

        val pickUp = hashMapOf(
            "address" to address,
            "lat" to lat,
            "lng" to lng,
            "status" to status
        )
        val id = UUID.randomUUID().toString()
        val booking = hashMapOf(
            "bookingDate" to System.currentTimeMillis(),
            "carType" to carType,
            "distance" to "$distance miles",
            "id" to id,
            "price" to price,
            "destinations" to destinationsList,
            "status" to RideStatus.booked,
            "pickUp" to pickUp,
            "rideType" to "zero",
            "userId" to Firebase.auth.currentUser!!.uid
        )

        val pickUpModel = Booking.PickUp(address, lat, lng)
        db.collection("Bookings").document(id).set(booking).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(fragmentContext, "Order Created", Toast.LENGTH_SHORT).show()
                isBooked = true
                updateNotification(
                    "New Ride Booked",
                    "Your booking was received, we will connect you to a driver soon.",
                    "booked",
                    id,
                    lat,
                    lng,
                    pickUpModel.address!!,
                    destAddress,
                    carType
                )
                binding.progressBar.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    fragmentContext,
                    task.exception!!.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateNotification(
        title: String,
        msg: String,
        status: String,
        orderID: String,
        pickUpLat: Double,
        pickUpLng: Double,
        pickUpAddress: String,
        dropOffAddress: String,
        carType: String
    ) {
        binding.progressBar.visibility = View.VISIBLE
        val isRead = hashMapOf(
            Firebase.auth.currentUser!!.uid to false
        )
        val notification = hashMapOf(
            "message" to msg,
            "orderId" to orderID,
            "isRead" to isRead,
            "timestamp" to System.currentTimeMillis(),
            "title" to title,
            "type" to status,
            "userId" to Firebase.auth.currentUser!!.uid,
        )
        db.collection("Notification").document().set(notification).addOnSuccessListener {
            binding.progressBar.visibility = View.GONE

            val intent = Intent(requireActivity(), YourRideActivity::class.java)
            intent.putExtra("pickUpLat", pickUpLat)
            intent.putExtra("pickUpLng", pickUpLng)
            intent.putExtra("pickUpAddress", pickUpAddress)
            intent.putExtra("dropOffAddress", dropOffAddress)
            intent.putExtra("driverID", "")
            intent.putExtra("carType", carType)
            intent.putExtra("rideID", orderID)
            startActivity(intent)

        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(fragmentContext, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showPaymentDialog(carType: String) {
        val dialogView =
            LayoutInflater.from(fragmentContext).inflate(R.layout.dialog_payment, null)
        val dialogBuilder = AlertDialog.Builder(fragmentContext)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()


        val cardTypeRadio = dialogView.findViewById<RadioButton>(R.id.visa_radio)
        val fair = dialogView.findViewById<TextView>(R.id.fair_tv)
        fair.setText("$ $totalFair")
        dialogView.findViewById<TextView>(R.id.your_proceed_btn).setOnClickListener {
            if (cardTypeRadio.isChecked) {
                cardType = "Visa"
                showCardDialog(carType, cardType)
            } else {
                cardType = "Master"
                showCardDialog(carType, cardType)
            }
            Log.d("LOGGER", "showCustomDialog: $carType")
            dialog.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }
}