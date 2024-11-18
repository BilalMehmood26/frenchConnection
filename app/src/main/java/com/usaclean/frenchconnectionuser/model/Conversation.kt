package com.usaclean.frenchconnectionuser.model

data class Conversation(
    var content: String = "",
    var fromID: String = "",
    var messageId: String = "",
    var fileName: String = "",
    var duration: String = "",
    var timestamp: Long = 0,
    var toID: String = "",
    var type: String = ""
)
