package com.usaclean.frenchconnectionuser.stripe

import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.AttachPaymentToCustomerResponse
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentRequest
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentResponse
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    @POST("/widgets/firststep")
    @JvmSuppressWildcards
    fun getFirstStep(@Body body: Map<String, Any>): Call<FirstStepResponse>

        @POST("/widgets/checkcustidexistornot")
        @JvmSuppressWildcards
        fun createCustomer(@Body body: Map<String, Any>): Call<CustomerResponse>

    @POST("/widgets/paymentMethods")
    @JvmSuppressWildcards
    fun cardList(@Body body: Map<String, Any>): Call<PaymentMethodsResponse>

    @POST("/widgets/detatch")
    @JvmSuppressWildcards
    fun removeCard(@Body body: Map<String, Any>): Call<PaymentIntentResponse>

    @POST("/widgets/attachPaymentToCustomer")
    @JvmSuppressWildcards
    suspend fun attachCustomer(@Body body: Map<String, Any>): Response<AttachPaymentToCustomerResponse>

    @POST("/widgets/accoutpaymentnew")
    @JvmSuppressWildcards
    fun createPaymentIntent(@Body requestBody: AccountPaymentRequest): Call<PaymentIntentResponse>

    @POST("widgets/confirmPayment")
    @JvmSuppressWildcards
    fun confirmPayment(@Body requestBody: ConfirmPaymentRequest): Call<ConfirmPaymentResponse>

    @PUT("widgets/updatestripeaccount")
    @JvmSuppressWildcards
    suspend fun connectAccount(@Body body: Map<String, Any>): Response<ResponseBody>

    @POST("widgets/sendEmail")
    @JvmSuppressWildcards
    fun sendEmail(@Body body: Map<String, Any>): Call<ResponseBody>

}