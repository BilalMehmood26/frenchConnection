package com.usaclean.frenchconnectionuser.model

data class Notification(
    val driverId :String? ="",
    val message :String? ="",
    val orderId :String? ="",
    val timestamp :Long? = 0,
    val title :String? ="",
    val type :String? ="",
    val userId :String? =""
)
