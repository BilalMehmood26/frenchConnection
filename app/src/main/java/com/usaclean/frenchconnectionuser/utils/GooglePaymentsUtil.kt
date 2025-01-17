package com.usaclean.frenchconnectionuser.utils

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object GooglePaymentsUtil {
    private var paymentsClient: PaymentsClient? = null
    private var isGooglePayAvailable = false

    private const val COUNTRY_CODE = "US"

    private val GOOGLE_SUPPORTED_NETWORKS = listOf("VISA", "MASTERCARD")
    private val SUPPORTED_METHODS = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

    private val allowedCardNetworks = JSONArray(GOOGLE_SUPPORTED_NETWORKS)
    private val allowedCardAuthMethods = JSONArray(SUPPORTED_METHODS)

    private val baseCardPaymentMethod = JSONObject().apply {
        put("type", "CARD")
        put("parameters", JSONObject().apply {
            put("allowedCardNetworks", allowedCardNetworks)
            put("allowedAuthMethods", allowedCardAuthMethods)
            put("billingAddressRequired", true)
            put("billingAddressParameters", JSONObject().apply {
                put("format", "FULL")
            })
        })
    }

    private val tokenizationSpecification = JSONObject().apply {
        put("type", "PAYMENT_GATEWAY")
        put("parameters", JSONObject().apply {
            put("gateway", "stripe")
            put("stripe:version", "2024-12-18.acacia")
            put("stripe:publishableKey", "pk_live_51PgBExCo08Oa4W8HPCTXBEdse6nqn39Rdz4qMDcVGdJdVWMOV5zY5lxQjHJy2H7QJ8RlpgfsFzzWWduSJEiG3b8O00qcKyXeUe")
        })
    }


    private val cardPaymentMethod = JSONObject().apply {
        put("type", "CARD")
        put("tokenizationSpecification", tokenizationSpecification)
        put("parameters", JSONObject().apply {
            put("allowedCardNetworks", allowedCardNetworks)
            put("allowedAuthMethods", allowedCardAuthMethods)
            put("billingAddressRequired", true)
            put("billingAddressParameters", JSONObject().apply {
                put("format", "FULL")
            })
        })
    }

    private val googlePayBaseConfiguration = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
        put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
    }

    fun createPaymentsClient(context: Context) {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
            .build()

        paymentsClient = Wallet.getPaymentsClient(context, walletOptions)
    }

    fun isReadyToPayRequest(): JSONObject? {
        return try {
            JSONObject(googlePayBaseConfiguration.toString())
                .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod))
        } catch (e: JSONException) {
            null
        }
    }

    fun getPaymentDataRequest(priceUSD: String): JSONObject {
        val transactionInfo = JSONObject().apply {
            put("totalPrice", priceUSD)
            put("totalPriceStatus", "FINAL")
            put("currencyCode", "USD")
        }

        val merchantInfo = JSONObject().apply {
            put("merchantName", "French Connection Shuttle Services LLC")
            put("merchantId", "BCR2DN4TQO2IHZJS")
        }

        return JSONObject(googlePayBaseConfiguration.toString()).apply {
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)
        }
    }

    fun getLoadPaymentDataTask(priceUSD: String): Task<PaymentData>? {
        val paymentDataRequestJson = getPaymentDataRequest(priceUSD)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return paymentsClient?.loadPaymentData(request)
    }
}