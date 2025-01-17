package com.usaclean.frenchconnectionuser.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.adapter.CardListAdapter
import com.usaclean.frenchconnectionuser.databinding.ActivityYourDestinationBinding
import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.stripe.Controller
import com.usaclean.frenchconnectionuser.utils.GooglePaymentsUtil
import com.usaclean.frenchconnectionuser.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.round

class YourDestinationActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding: ActivityYourDestinationBinding by lazy {
        ActivityYourDestinationBinding.inflate(layoutInflater)
    }

    private var driverName = "--"
    private var driverRating = "0"
    private var status = ""
    private var driverId: String? = ""
    private var bookingDate = ""
    private var price = 0.0
    private var tipPrice = 0.0
    private var carType: String = ""
    private var secrat = ""
    lateinit var stripe: Stripe

    private var cardList: ArrayList<PaymentMethodsResponse.PaymentMethod> = ArrayList()
    private var rideID: String? = ""
    private var riderUID: String? = ""
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        price = intent.getDoubleExtra("price", 0.0)
        bookingDate = intent.getStringExtra("bookingDate")!!
        status = intent.getStringExtra("status")!!
        driverName = intent.getStringExtra("driverName")!!
        driverRating = intent.getStringExtra("driverRating")!!
        rideID = intent.getStringExtra("rideID")!!

        binding.apply {
            backBtn.setOnClickListener {
                finish()
            }

            getOrders()
            GooglePaymentsUtil.createPaymentsClient(this@YourDestinationActivity)
            stripe = Stripe(
                this@YourDestinationActivity,
                "pk_test_51PgBExCo08Oa4W8HRRlISwH7IOZRW42joDX0KpJRo7RK4tZhrz29Cout7tSsBEWCeODsr7IhT8jQGNiUrMIwwR0h00jZcUoUkr"
            )

            markCompleteBtn.setOnClickListener {
                showPaymentDialog(carType)
            }

            getCardList(UserSession.user.stripeCustid!!)
            createFirstStep(UserSession.user.stripeCustid!!)
            setListener()
        }
    }

    private val paymentDataLauncher =
        registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    taskResult.result?.let { token ->
                        tipPrice+=1.0
                        updateStatus(rideID!!, "rated",round(binding.ratingBar.rating).toInt(),tipPrice)
                    }
                }

                CommonStatusCodes.CANCELED -> {}
                AutoResolveHelper.RESULT_ERROR -> {}
                CommonStatusCodes.INTERNAL_ERROR -> {}
            }
        }

    fun requestPaymentGooglePay(priceUSD: String) {
        val task = GooglePaymentsUtil.getLoadPaymentDataTask(priceUSD)
        task?.addOnCompleteListener(paymentDataLauncher::launch)
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

                        getCardList(UserSession.user.stripeCustid!!)
                        Log.d("Logger", "TranferInfo: ${response.body()?.transferinfo}")
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@YourDestinationActivity,
                            response.errorBody()!!.string(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                override fun onFailure(call: Call<PaymentIntentResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@YourDestinationActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setListener() {


        binding.fiveTip.setOnClickListener {
            binding.fiveTip.setBackgroundResource(R.drawable.tip_button_selected)
            binding.fiveTip.setTextColor(resources.getColor(R.color.white))
            tipPrice = 5.00

            binding.tenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.fifteenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.twentyTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.tenTip.setTextColor(resources.getColor(R.color.black))
            binding.fifteenTip.setTextColor(resources.getColor(R.color.black))
            binding.twentyTip.setTextColor(resources.getColor(R.color.black))
        }

        binding.tenTip.setOnClickListener {
            binding.tenTip.setBackgroundResource(R.drawable.tip_button_selected)
            binding.tenTip.setTextColor(resources.getColor(R.color.white))
            tipPrice = 10.00

            binding.fiveTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.fifteenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.twentyTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.fiveTip.setTextColor(resources.getColor(R.color.black))
            binding.fifteenTip.setTextColor(resources.getColor(R.color.black))
            binding.twentyTip.setTextColor(resources.getColor(R.color.black))
        }

        binding.fifteenTip.setOnClickListener {
            binding.fifteenTip.setBackgroundResource(R.drawable.tip_button_selected)
            binding.fifteenTip.setTextColor(resources.getColor(R.color.white))
            tipPrice = 15.00


            binding.fiveTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.twentyTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.tenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)

            binding.tenTip.setTextColor(resources.getColor(R.color.black))
            binding.fiveTip.setTextColor(resources.getColor(R.color.black))
            binding.twentyTip.setTextColor(resources.getColor(R.color.black))
        }

        binding.twentyTip.setOnClickListener {
            binding.twentyTip.setBackgroundResource(R.drawable.tip_button_selected)
            binding.twentyTip.setTextColor(resources.getColor(R.color.white))
            tipPrice = 20.00

            binding.fiveTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.tenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.fifteenTip.setBackgroundResource(R.drawable.tip_dutton_unselected)
            binding.fifteenTip.setTextColor(resources.getColor(R.color.black))
            binding.tenTip.setTextColor(resources.getColor(R.color.black))
            binding.fiveTip.setTextColor(resources.getColor(R.color.black))
        }
    }

    private fun updateStatus(rideID: String, status: String, rating: Int, tipPrice: Double) {

        binding.progressBar.visibility = View.VISIBLE
        val updateStatus = hashMapOf(
            "status" to status,
            "rating" to rating,
            "tipPrice" to tipPrice,
        )

        db.collection("Bookings").document(rideID).update(updateStatus as Map<String, Any>)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                startActivity(Intent(this@YourDestinationActivity,DashboardActivity::class.java))
                updateNotification("Tip Awarderd","You have just received a $$tipPrice tip.","$status")
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@YourDestinationActivity,
                    it.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getOrders() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("Bookings").document(rideID!!).addSnapshotListener { value, error ->
            if (error != null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@YourDestinationActivity,
                    error.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val ride = value?.toObject(Booking::class.java)
            if (ride != null) {
                binding.apply {
                    timeDateTv.text = bookingDate
                    priceTv.text = price.toString()
                    nameTv.text = driverName
                    ratingTv.text = driverRating
                    driverId = ride.driverId
                    getDriverInfo(ride.driverId!!)
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
                if (user.image!!.isNotEmpty()) {
                    Glide.with(this).load(user.image).into(binding.profileIV)
                } else {
                    binding.profileIV.setImageResource(R.drawable.main_logo)
                }
                binding.nameTv.text = user.userName
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

    private fun updateNotification(title: String, msg: String, status: String) {
        binding.progressBar.visibility = View.VISIBLE
        val isRead = hashMapOf(
            Firebase.auth.currentUser!!.uid to false,
            driverId to false,
        )
        val notification = hashMapOf(
            "driverId" to Firebase.auth.currentUser!!.uid,
            "message" to msg,
            "orderId" to rideID,
            "isRead" to isRead,
            "timestamp" to System.currentTimeMillis(),
            "title" to title,
            "type" to status,
            "userId" to driverId,
        )
        db.collection("Notification").document().set(notification).addOnSuccessListener {
            binding.progressBar.visibility = View.GONE
            finish()
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showCardDialog(carType: String) {
        val dialogView =
            LayoutInflater.from(this@YourDestinationActivity).inflate(R.layout.dialog_card, null)
        val dialogBuilder = AlertDialog.Builder(this@YourDestinationActivity)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val fair = dialogView.findViewById<TextView>(R.id.fair_tv)
        fair.setText("$ $tipPrice"+"0"+"+ $1.00 Processing Charges" )
        dialogView.findViewById<TextView>(R.id.add_card_btn).setOnClickListener {
            addCardDialog(carType)
            dialog.dismiss()
        }

        val cardListRv = dialogView.findViewById<RecyclerView>(R.id.card_list_rv)

        cardListRv.layoutManager = LinearLayoutManager(this@YourDestinationActivity)
        cardListRv.adapter = CardListAdapter(this@YourDestinationActivity, cardList) {
            tipPrice+=1.0
            updateStatus(rideID!!, "rated",round(binding.ratingBar.rating).toInt(),tipPrice)
            dialog.dismiss()
        }
    }

    private fun addCardDialog(carType: String) {
        val dialogView =
            LayoutInflater.from(this@YourDestinationActivity).inflate(R.layout.dialog_add_card, null)
        val dialogBuilder = AlertDialog.Builder(this@YourDestinationActivity)
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
                Toast.makeText(this@YourDestinationActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
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
                        this@YourDestinationActivity,
                        response.errorBody()!!.string(),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<PaymentMethodsResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@YourDestinationActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })

    }

    @SuppressLint("MissingInflatedId")
    private fun showPaymentDialog(carType: String) {
        val dialogView =
            LayoutInflater.from(this@YourDestinationActivity).inflate(R.layout.dialog_payment, null)
        val dialogBuilder = AlertDialog.Builder(this@YourDestinationActivity)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()


        val cardTypeRadio = dialogView.findViewById<RadioButton>(R.id.visa_radio)
        val fair = dialogView.findViewById<TextView>(R.id.fair_tv)

        val tipText = binding.otherTip.text.toString().trim()
        if (tipText.isNotEmpty()) {
            tipPrice = tipText.toDouble()
        }
        fair.setText("$ $tipPrice"+"0")

        dialogView.findViewById<TextView>(R.id.your_proceed_btn).setOnClickListener {
            if (cardTypeRadio.isChecked) {
                showCardDialog(carType)
            } else {
                //showCardDialog(carType, cardType)
                //googlePayment(totalFair.toString())
                requestPaymentGooglePay(tipPrice.toString())
            }
            Log.d("LOGGER", "showCustomDialog: $carType")
            dialog.dismiss()
        }


    }

    override fun onMapReady(googelMaps: GoogleMap) {
        /*myGoogleMap = googelMaps
        //mMap = googelMaps
        val latLng = LatLng(pickUpLat, pickUpLng)

        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        myGoogleMap?.addMarker(MarkerOptions().position(latLng))
        myGoogleMap?.setOnMapClickListener { latLng ->
            myGoogleMap?.clear()
            myGoogleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }
        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))*/
    }
}