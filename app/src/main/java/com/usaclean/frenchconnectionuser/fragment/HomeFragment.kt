package com.usaclean.frenchconnectionuser.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.consumePurchase
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.contract.TaskResultContracts
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
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentRequest
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentResponse
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.model.RideStatus
import com.usaclean.frenchconnectionuser.stripe.Controller
import com.usaclean.frenchconnectionuser.stripe.repo.PaymentViewModel
import com.usaclean.frenchconnectionuser.utils.GooglePaymentsUtil
import com.usaclean.frenchconnectionuser.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt


class HomeFragment : Fragment(){


    private val binding: FragmentHomeBinding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }

    private val memberList = arrayListOf("1", "2", "3", "4", "5", "6", "7")

    private val productMap = mapOf(
        "OneWay_1" to "one_way_1",
        "OneWay_2" to "one_way_2",
        "OneWay_3" to "one_way_3",
        "OneWay_4" to "one_way_4",
        "OneWay_5" to "one_way_5",
        "OneWay_6" to "one_way_6",
        "OneWay_7" to "one_way_7",
        "Return_1" to "two_way_1",
        "Return_2" to "two_way_2",
        "Return_3" to "two_way_3",
        "Return_4" to "two_way_4",
        "Return_5" to "two_way_5",
        "Return_6" to "two_way_6",
        "Return_7" to "two_way_7"
    )

    private var selectedProduct = ""
    private var selectedIndex = 0

    private lateinit var mBillingClient: BillingClient

    private var numberOfMembers = 1
    private var totalFair = 10
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
    lateinit var stripe: Stripe


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.apply {

            yourProfileBtn.setOnClickListener {
                Log.d("logger", "onCreateView: ")
            }
            //Google_Pay
            GooglePaymentsUtil.createPaymentsClient(requireActivity())

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
                selectedIndex = newIndex + 1
                parajeID.text = pharmacy
                updateSelectedProduct()

                Log.d("LOGGER", "Selected Product Id: $selectedProduct")
            }
            stripe = Stripe(
                fragmentContext,
                "pk_live_51PgBExCo08Oa4W8HPCTXBEdse6nqn39Rdz4qMDcVGdJdVWMOV5zY5lxQjHJy2H7QJ8RlpgfsFzzWWduSJEiG3b8O00qcKyXeUe"
            )
            PaymentConfiguration.init(
                fragmentContext,
                "pk_live_51PgBExCo08Oa4W8HPCTXBEdse6nqn39Rdz4qMDcVGdJdVWMOV5zY5lxQjHJy2H7QJ8RlpgfsFzzWWduSJEiG3b8O00qcKyXeUe"
            )
            createFirstStep(UserSession.user.stripeCustid!!)
            if (UserSession.user.stripeCustid == "") {
                createCustomer(UserSession.user.email!!, UserSession.user.id!!)
            } else {
                Log.d("Logger", "onCreateView: ${UserSession.user.stripeCustid}")
                createFirstStep(UserSession.user.stripeCustid!!)
            }

            getBooking()
            setListener()
            getCardList(UserSession.user.stripeCustid!!)
            initBillingClient()


        }
        return binding.root
    }

    private fun updateSelectedProduct() {
        val key = "${returnWay}_$selectedIndex"
        selectedProduct = productMap[key] ?: ""
    }

    private fun initBillingClient() {
        mBillingClient = BillingClient.newBuilder(fragmentContext)
            .enablePendingPurchases()
            .setListener { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    list.forEach { purchase ->
                        Log.d("LOGGER", "Verify successful")
                        val transactionId = purchase.orderId?: UUID.randomUUID().toString()
                        Log.d("LOGGER", "transiction ID : $transactionId")
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                consumePurchase(purchase)
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
                            } catch (e: Exception) {
                                Log.e("LOGGER", "Error in addBooking", e)
                            }
                        }
                    }
                }else{
                    Log.d("LOGGER", "responseCode is null ")
                }
            }.build()

        establishConnection()
    }

    private fun establishConnection() {
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    //Use any of function below to get details upon successful connection
                    Log.d("LOGGER", "Connection Established")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d("LOGGER", "Connection NOT Established")
                establishConnection()
            }
        })
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken) // Set the purchase token (required)
            .build()

        lifecycleScope.launch {
            mBillingClient.consumePurchase(consumeParams)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(fragmentContext, "hello", Toast.LENGTH_SHORT).show()
        when (requestCode) {
            1000 -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val paymentData = PaymentData.getFromIntent(data!!)
                        Log.d("LOGGER", "onActivityResult:${paymentData!!.toJson()} ")
                    }

                    Activity.RESULT_CANCELED -> {
                        Toast.makeText(fragmentContext, "Canceled", Toast.LENGTH_SHORT).show()
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            Toast.makeText(fragmentContext, "${it.statusCode}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
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

        binding.progressBar.visibility = View.VISIBLE
        val cardParams = CardParams(
            cardNumber,
            expiryMonth.toInt(),
            expiryYear.toInt(),
            cvc,
            "${UserSession.user.userName}"
        )

        Log.d("LOGGER", "Secrate : $secrat")
        if (secrat.isNotEmpty()) {
            val paymentMethodCreateParams = PaymentMethodCreateParams.createCard(cardParams)
            val paymentIntentParams =
                ConfirmSetupIntentParams.create(paymentMethodCreateParams, secrat)
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
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(fragmentContext, "Secret Key is empty", Toast.LENGTH_SHORT).show()
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
                        confirmPayment(response.body()!!.transferinfo)
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

    private fun confirmPayment(pmID: String) {
        binding.progressBar.visibility = View.VISIBLE
        val pm_ID = ConfirmPaymentRequest(pmID)
        Controller.instance.confirmPayment(pm_ID)
            .enqueue(object : Callback<ConfirmPaymentResponse> {
                override fun onResponse(
                    call: Call<ConfirmPaymentResponse>,
                    response: Response<ConfirmPaymentResponse>
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
                    } else {
                        Toast.makeText(
                            fragmentContext,
                            response.errorBody()!!.string(),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ConfirmPaymentResponse>, t: Throwable) {
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createRide() {
        val fareMap = mapOf(
            "OneWay" to listOf(
                1.00,
                10.00,
                18.00,
                24.00,
                28.00,
                40.00,
                50.00,
                54.00
            ).map { it.toInt() },
            "Return" to listOf(
                1.00,
                25.00,
                30.00,
                40.00,
                50.00,
                60.00,
                90.00,
                100.00
            ).map { it.toInt() }
        )
        totalFair = fareMap[returnWay]?.getOrNull(numberOfMembers) ?: 0

        showCustomDialog()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setListener() {

        binding.runningRide.setOnClickListener {
            binding.runningRide.setBackgroundResource(R.drawable.gradient_button)
            binding.completeRide.setBackgroundResource(0)
            returnWay = "OneWay"
            updateSelectedProduct()
        }

        binding.completeRide.setOnClickListener {
            binding.completeRide.setBackgroundResource(R.drawable.gradient_button)
            binding.runningRide.setBackgroundResource(0)
            returnWay = "Return"
            updateSelectedProduct()
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

    @RequiresApi(Build.VERSION_CODES.S)
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
            dialog.dismiss()
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
        // Update progress bar visibility on the main thread
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
        }

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
            "distance" to "$distance miles", // Ensure 'distance' is properly initialized
            "id" to id,
            "price" to price,
            "destinations" to destinationsList,
            "status" to RideStatus.booked,
            "pickUp" to pickUp,
            "rideType" to "zero",
            "userId" to Firebase.auth.currentUser!!.uid
        )

        Log.d("LOGGER", "in Booking method")
        val pickUpModel = Booking.PickUp(address, lat, lng)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Perform the database operation in the background
                db.collection("Bookings").document(id).set(booking).addOnCompleteListener { task ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (task.isSuccessful) {
                            Toast.makeText(fragmentContext, "Order Created", Toast.LENGTH_SHORT).show()
                            isBooked = true
                            Log.d("LOGGER", "Booking success")
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
                            Log.d("LOGGER", "Booking failed")
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(fragmentContext, task.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LOGGER", "Error in addBooking", e)
                // Handle error
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
        Log.d("LOGGER", "in Notification Method")
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
            Log.d("LOGGER", "in Notificiation success")
            /*val intent = Intent(requireActivity(), YourRideActivity::class.java)
            intent.putExtra("pickUpLat", pickUpLat)
            intent.putExtra("pickUpLng", pickUpLng)
            intent.putExtra("pickUpAddress", pickUpAddress)
            intent.putExtra("dropOffAddress", dropOffAddress)
            intent.putExtra("driverID", "")
            intent.putExtra("carType", carType)
            intent.putExtra("rideID", orderID)
            startActivity(intent)*/

        }.addOnFailureListener {
            Log.d("LOGGER", "in Notificaiton Fail")
            binding.progressBar.visibility = View.GONE
            Toast.makeText(fragmentContext, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
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
        fair.text = "$ $totalFair"
        dialogView.findViewById<TextView>(R.id.your_proceed_btn).setOnClickListener {
            if (cardTypeRadio.isChecked) {
                cardType = "Card"
                showCardDialog(carType, cardType)
            } else {
                cardType = "GooglePay"
                getSubPurchases()
            }
            Log.d("LOGGER", "showCustomDialog: ${totalFair.toString()}")
            dialog.dismiss()
        }
    }

    private fun getSubPurchases() {
        val productList = java.util.ArrayList<QueryProductDetailsParams.Product>()

        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(selectedProduct)
                .setProductType(BillingClient.SkuType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        mBillingClient.queryProductDetailsAsync(params) { billingResult, list ->
            Log.d("LOGGER", "list Size: ${list.size}")
            Log.d("LOGGER", "list: ${list}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list.isNotEmpty()) {
                launchSubPurchase(list[0])
                Log.d("LOGGER", "Product Price: ${list[0].oneTimePurchaseOfferDetails?.formattedPrice}")
            }else{
                Log.d("LOGGER", "Product List is Empty")
            }
        }

    }

    private fun launchSubPurchase(productDetails: ProductDetails) {
        Log.d("LOGGER", "in launchSubPurchase method")
        val productList = mutableListOf<BillingFlowParams.ProductDetailsParams>()
        productList.add(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        mBillingClient.launchBillingFlow(requireActivity(), billingFlowParams)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }
}