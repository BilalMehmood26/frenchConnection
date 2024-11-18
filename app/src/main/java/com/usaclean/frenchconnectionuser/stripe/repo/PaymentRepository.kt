package com.usaclean.frenchconnectionuser.stripe.repo

import com.usaclean.frenchconnectionuser.model.AccountPaymentRequest
import com.usaclean.frenchconnectionuser.model.AttachPaymentToCustomerResponse
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentRequest
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentResponse
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import com.usaclean.frenchconnectionuser.stripe.Controller
import okhttp3.ResponseBody

class PaymentRepository {
  /*  suspend fun customerCardList(cus_id: String): Result<PaymentMethodsResponse> {
        val body = mapOf("cus_id" to cus_id)
        return try {
            val response = Controller.instance.cardList(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Customer Creating Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    /*suspend fun removeCard(pm_id: String): Result<PaymentIntentResponse> {
        val body = mapOf("pm_id" to pm_id)
        return try {
            val response = Controller.instance.removeCard(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Customer Creating Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    /*suspend fun createCustomer(email: String, userId: String): Result<CustomerResponse> {
        val body = mapOf("email" to email, "userId" to userId)
        return try {
            val response = Controller.instance.createCustomer(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Customer Creating Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

   /* suspend fun createFirstStep(customer_key: String): Result<FirstStepResponse> {
        val body = mapOf("cus_id" to customer_key)
        return try {
            val response = Controller.instance.getFirstStep(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("FirstStep Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    suspend fun attachCard(customer_key: String, paymentMethodId: String): Result<AttachPaymentToCustomerResponse> {
        val body = mapOf("cus_id" to customer_key, "pm_id" to paymentMethodId)
        return try {
            val response = Controller.instance.attachCustomer(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("attachCard Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /*suspend fun createPaymentIntent(amount: Int, customerId: String, paymentMethodId: String): Result<PaymentIntentResponse> {
        //val body = mapOf("amount" to amount, "customerId" to customerId, "paymentMethodId" to paymentMethodId)
        val body = AccountPaymentRequest(customerId, amount.toString(), paymentMethodId)
        return try {
            val response = Controller.instance.createPaymentIntent(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Payment New Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    suspend fun confirmPaymentIntent(paymentMethodId: String): Result<ConfirmPaymentResponse> {
        //val body = mapOf("pi_id" to paymentMethodId)
        val body = ConfirmPaymentRequest(paymentMethodId)
        return try {
            val response = Controller.instance.confirmPayment(body)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Confirm Payment Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectAccount(map : Map<String, Any>): Result<ResponseBody> {
        return try {
            val response = Controller.instance.connectAccount(map)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Connect Fail: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}