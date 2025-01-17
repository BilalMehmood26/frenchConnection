package com.usaclean.frenchconnectionuser.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.CommonStatusCodes
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
import com.usaclean.frenchconnectionuser.activities.DashboardActivity
import com.usaclean.frenchconnectionuser.activities.YourRideActivity
import com.usaclean.frenchconnectionuser.adapter.CalendarAdapter
import com.usaclean.frenchconnectionuser.adapter.CardListAdapter
import com.usaclean.frenchconnectionuser.databinding.FragmentHomeBinding
import com.usaclean.frenchconnectionuser.databinding.FragmentSchduleRideBinding
import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.model.RideStatus
import com.usaclean.frenchconnectionuser.stripe.Controller
import com.usaclean.frenchconnectionuser.utils.GooglePaymentsUtil
import com.usaclean.frenchconnectionuser.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

class SchduleRideFragment : Fragment() {

    private val binding: FragmentSchduleRideBinding by lazy {
        FragmentSchduleRideBinding.inflate(layoutInflater)
    }

    private var destAddress: String = ""
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var distance: Double = 0.0
    private var schduleDateInMillis: Long = 0
    private var pickUpAddress: String = "0"
    private var pickUpLat: Double = 0.0
    private var pickUpLng: Double = 0.0
    private var carType: String = ""
    private var cardType: String = ""
    private var time: Long = 0
    private val db = Firebase.firestore

    private var cardList: ArrayList<PaymentMethodsResponse.PaymentMethod> = ArrayList()

    private var secrat = ""
    private var paymentMethod = ""
    lateinit var stripe: Stripe

    private lateinit var selectedDate: LocalDate

    private val memberList = arrayListOf("1", "2", "3", "4", "5", "6", "7")
    private var numberOfMembers = 1
    private var totalFair = 10
    private var returnWay = "OneWay"

    private lateinit var fragmentContext: Context
    private lateinit var addressesDialogFragment: AddressesDialogFragment

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding.apply {

            stripe = Stripe(
                fragmentContext,
                "pk_test_51PgBExCo08Oa4W8HRRlISwH7IOZRW42joDX0KpJRo7RK4tZhrz29Cout7tSsBEWCeODsr7IhT8jQGNiUrMIwwR0h00jZcUoUkr"
            )
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


            createFirstStep(UserSession.user.stripeCustid!!)
            getCardList(UserSession.user.stripeCustid!!)
            /*
                        pendingCalender.setOnDateChangeListener { view, year, month, dayOfMonth ->
                            val calendar = Calendar.getInstance()
                            calendar.set(year, month, dayOfMonth)
                            schduleDateInMillis = calendar.timeInMillis

                        }*/

            runningRide.setOnClickListener {
                binding.runningRide.setBackgroundResource(R.drawable.gradient_button)
                binding.completeRide.setBackgroundResource(0)
                returnWay = "OneWay"

            }

            completeRide.setOnClickListener {
                binding.completeRide.setBackgroundResource(R.drawable.gradient_button)
                binding.runningRide.setBackgroundResource(0)
                returnWay = "Return"
            }

            parajeID.setItems(memberList)
            parajeID.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newText ->
                val pharmacy = memberList[newIndex]
                numberOfMembers = newIndex
                parajeID.setText(pharmacy)
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

            binding.prevIV.setOnClickListener {
                selectedDate = selectedDate.minusMonths(1)
                setMonthView()
            }

            binding.nextIV.setOnClickListener {
                selectedDate = selectedDate.plusMonths(1)
                setMonthView()
            }

            binding.timePicker.setOnClickListener {
                showTimePicker()
            }

        }
        setCalendar()
        return binding.root
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
            cal.set(android.icu.util.Calendar.HOUR_OF_DAY, hour)
            cal.set(android.icu.util.Calendar.MINUTE, minute)
            time = cal.timeInMillis
            binding.timeTv.setText(SimpleDateFormat("HH:mm").format(cal.time))
        }

        TimePickerDialog(
            fragmentContext,
            timeSetListener,
            cal.get(android.icu.util.Calendar.HOUR_OF_DAY),
            cal.get(android.icu.util.Calendar.MINUTE),
            true
        ).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCalendar() {
        selectedDate = LocalDate.now()
        setMonthView()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setMonthView() {
        binding.apply {
            monthYearTV.text = monthYearFromDate(selectedDate)
            val daysInMonth = daysInMonthArray(selectedDate)

            val calendarAdapter =
                CalendarAdapter(daysInMonth, selectedDate) { dateinText, dateStamp ->
                    schduleDateInMillis = dateStamp
                    Log.d("Logger", "setMonthView: $dateinText")
                }
            val layoutManager = GridLayoutManager(requireActivity(), 7)
            calendarRecyclerView.layoutManager = layoutManager
            calendarRecyclerView.adapter = calendarAdapter
        }
    }

    private fun mergeDateAndTimeMillis(dateMillis: Long, timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }

        val timeCalendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND))
        }
        return calendar.timeInMillis
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstOfMonth = selectedDate.withDayOfMonth(1)
        val dayOfWeek = firstOfMonth.dayOfWeek.value

        for (i in 1..42) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
            }
        }

        return daysInMonthArray
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun monthYearFromDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }

    private fun isWithinFiveMiles(
        pickupLat: Double, pickupLng: Double,
        dropOffLat: Double, dropOffLng: Double
    ): Boolean {
        val distanceInMiles = calculateDistanceInMiles(pickupLat, pickupLng, dropOffLat, dropOffLng)
        distance = distanceInMiles
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

    private fun createRide() {

        val fareMap = mapOf(
            "OneWay" to listOf(10.00, 18.00, 24.00, 28.00, 40.00, 50.00, 54.00).map { it.toInt() },
            "Return" to listOf(25.00, 30.00, 40.00, 50.00, 60.00, 90.00, 100.00).map { it.toInt() }
        )
        totalFair = fareMap[returnWay]!!.getOrNull(numberOfMembers) ?: 0
        showCustomDialog()
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

    private fun scheduleBooking(
        carType: String,
        price: String,
        destAddress: String,
        destLat: Double,
        destLng: Double,
        pickUpAddress: String,
        pickUpLat: Double,
        pickUpLng: Double
    ) {
        binding.progressBar.visibility = View.VISIBLE
        val destinations = hashMapOf<String, Any>(
            "address" to destAddress,
            "lat" to destLat,
            "lng" to destLng
        )
        val destinationsList = arrayListOf<HashMap<String, Any>>()
        destinationsList.add(destinations)

        val pickUp = hashMapOf(
            "address" to pickUpAddress,
            "lat" to pickUpLat,
            "lng" to pickUpLng
        )

        val bookingTime = mergeDateAndTimeMillis(schduleDateInMillis,time)
        val id = UUID.randomUUID().toString()
        val booking = hashMapOf(
            "bookingDate" to bookingTime,
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

        db.collection("ScheduledRides").document(id).set(booking).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(fragmentContext, "Order Scheduled", Toast.LENGTH_SHORT).show()
                /* val intent = Intent(requireActivity(), YourRideActivity::class.java)
                 intent.putExtra("pickUpLat", pickUpLat)
                 intent.putExtra("pickUpLng", pickUpLng)
                 intent.putExtra("pickUpAddress", pickUpAddress)
                 intent.putExtra("dropOffAddress", destAddress)
                 intent.putExtra("driverID", "")
                 intent.putExtra("rideID", id)
                 startActivity(intent)*/
                binding.apply {
                    yourLocation.setText("")
                    dropOffTv.setText("")
                    timeTv.setText("")
                }

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
            scheduleBooking(
                carType,
                totalFair.toString(),
                destAddress,
                destLat,
                destLng,
                pickUpAddress,
                pickUpLat,
                pickUpLng
            )
            dialog.dismiss()
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
        showCardDialog(carType,"")
        dialogView.findViewById<TextView>(R.id.your_proceed_btn).setOnClickListener {
            if (cardTypeRadio.isChecked) {
                showCardDialog(carType,"")
            } else {
                requestPaymentGooglePay(totalFair.toString())
            }
            Log.d("LOGGER", "showCustomDialog: $carType")
            dialog.dismiss()
        }
    }

    fun requestPaymentGooglePay(priceUSD: String) {
        val task = GooglePaymentsUtil.getLoadPaymentDataTask(priceUSD)
        task?.addOnCompleteListener(paymentDataLauncher::launch)
    }

    private val paymentDataLauncher =
        registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    taskResult.result?.let { token ->
                        scheduleBooking(
                            carType,
                            totalFair.toString(),
                            destAddress,
                            destLat,
                            destLng,
                            pickUpAddress,
                            pickUpLat,
                            pickUpLng
                        )
                    }
                }

                CommonStatusCodes.CANCELED -> {}
                AutoResolveHelper.RESULT_ERROR -> {}
                CommonStatusCodes.INTERNAL_ERROR -> {}
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
                        scheduleBooking(
                            carType,
                            totalFair.toString(),
                            destAddress,
                            destLat,
                            destLng,
                            pickUpAddress,
                            pickUpLat,
                            pickUpLng
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }
}