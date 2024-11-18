package com.usaclean.frenchconnectionuser.model

data class AccountPaymentRequest(
    val cus_id: String,
    val amount: String,
    val pm_id: String
)
