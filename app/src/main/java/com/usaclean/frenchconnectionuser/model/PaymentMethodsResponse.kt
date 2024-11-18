package com.usaclean.frenchconnectionuser.model


data class PaymentMethodsResponse(
    val success: Int,
    val paymentMethods: List<PaymentMethod>
){
    data class PaymentMethod(
        val id: String,
        val brand: String,
        val exp_month: Int,
        val exp_year: Int,
        val fingerprint: String,
        val last4: String
    )
}
