package com.usaclean.frenchconnectionuser.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.adapter.ChatAdapter
import com.usaclean.frenchconnectionuser.adapter.CreditCardAdapter
import com.usaclean.frenchconnectionuser.databinding.FragmentHomeBinding
import com.usaclean.frenchconnectionuser.databinding.FragmentWalletBinding
import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.stripe.Controller
import com.usaclean.frenchconnectionuser.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class WalletFragment : Fragment() {

    private var db = Firebase.firestore
    private lateinit var stripe: Stripe
    private lateinit var creditCardAdapter: CreditCardAdapter
    private var secrat = ""

    private val binding: FragmentWalletBinding by lazy {
        FragmentWalletBinding.inflate(layoutInflater)
    }
    private lateinit var fragmentContext: Context

    private var cardList: ArrayList<PaymentMethodsResponse.PaymentMethod> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        stripe = Stripe(
            requireContext(),
            "pk_test_51PgBExCo08Oa4W8HRRlISwH7IOZRW42joDX0KpJRo7RK4tZhrz29Cout7tSsBEWCeODsr7IhT8jQGNiUrMIwwR0h00jZcUoUkr"
        )
        if (UserSession.user.stripeCustid == "") {
            createCustomer(UserSession.user.email!!, UserSession.user.id!!)
        } else {
            getCardList(UserSession.user.stripeCustid!!)
            createFirstStep(UserSession.user.stripeCustid!!)
        }

        binding.addCardBtn.setOnClickListener {
            addCardDialog()
        }
        return binding.root
    }

    private fun createFirstStep(stripeCustId: String) {
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
                        Log.d("logger", "onResponse:mmn $secrat")
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.d("logger", "onResponse: ${response.errorBody()!!.string()}")
                }
            }

            override fun onFailure(call: Call<FirstStepResponse>, t: Throwable) {

            }
        })

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

    private fun getCardList(cusId: String) {

        binding.progressBar.visibility = View.VISIBLE
        cardList.clear()
        val body = mapOf("cus_id" to cusId)
        Controller.instance.cardList(body).enqueue(object : Callback<PaymentMethodsResponse> {
            override fun onResponse(
                call: Call<PaymentMethodsResponse>,
                response: Response<PaymentMethodsResponse>
            ) {
                if (response.isSuccessful) {
                    cardList.addAll(response.body()!!.paymentMethods)
                    binding.progressBar.visibility = View.GONE
                    updateAdapter()

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


    private fun addCard(
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

        val paymentMethodCreateParams = PaymentMethodCreateParams.createCard(cardParams)
        val paymentIntentParams = ConfirmSetupIntentParams.create(paymentMethodCreateParams, secrat)
        val setupIntent = stripe.confirmSetupIntentSynchronous(paymentIntentParams)

        if (setupIntent.status == StripeIntent.Status.Succeeded) {
            Log.d("LOGGER", "Customer Key: ${UserSession.user.stripeCustid}")
            Log.d("LOGGER", "Payment Intent Key: ${setupIntent.paymentMethodId.toString()}")
            getPaymentIntent(
                1000,
                UserSession.user.stripeCustid,
                setupIntent.paymentMethodId.toString()
            )
        }
    }

    private fun addCardDialog() {
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
                    cardNumber.text.toString(),
                    month.text.toString(),
                    year.text.toString(),
                    cvc.text.toString(),
                    postalCode.text.toString()
                )
            }
        }
    }

    private fun getPaymentIntent(amount: Int, stripeCustId: String?, paymentMethodId: String) {

        val body = AccountPaymentRequest(stripeCustId!!, amount.toString(), paymentMethodId)
        Controller.instance.createPaymentIntent(body)
            .enqueue(object : Callback<PaymentIntentResponse> {
                override fun onResponse(
                    call: Call<PaymentIntentResponse>,
                    response: Response<PaymentIntentResponse>
                ) {
                    if (response.isSuccessful) {
                        getCardList(UserSession.user.stripeCustid!!)
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

    private fun updateAdapter() {
        binding.cardListRv.layoutManager = LinearLayoutManager(fragmentContext)
        creditCardAdapter = CreditCardAdapter(fragmentContext, cardList) { pmID, pos ->
            showAlertDialog(pmID, pos)

        }
        binding.cardListRv.adapter = creditCardAdapter
    }

    private fun deleteCard(pmID: String,pos:Int) {
        val body = mapOf("pm_id" to pmID)
        binding.progressBar.visibility = View.VISIBLE
        Controller.instance.removeCard(body).enqueue(object : Callback<PaymentIntentResponse> {
            override fun onResponse(
                call: Call<PaymentIntentResponse>,
                response: Response<PaymentIntentResponse>
            ) {
                if (response.isSuccessful) {
                    binding.progressBar.visibility = View.GONE
                    cardList.clear()
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

            override fun onFailure(call: Call<PaymentIntentResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    fragmentContext,
                    t.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showAlertDialog(pmID: String, pos: Int) {
        val builder = AlertDialog.Builder(fragmentContext)
        builder.setTitle("Delete")
        builder.setMessage("Are you sure you want to delete card.")

        builder.setPositiveButton("Delete") { dialog, which ->
            deleteCard(pmID,pos)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, which ->

            dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }

}