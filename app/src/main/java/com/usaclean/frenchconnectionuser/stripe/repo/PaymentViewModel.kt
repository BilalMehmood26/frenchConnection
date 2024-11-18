package com.usaclean.frenchconnectionuser.stripe.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usaclean.frenchconnectionuser.model.AttachPaymentToCustomerResponse
import com.usaclean.frenchconnectionuser.model.ConfirmPaymentResponse
import com.usaclean.frenchconnectionuser.model.CustomerResponse
import com.usaclean.frenchconnectionuser.model.FirstStepResponse
import com.usaclean.frenchconnectionuser.model.PaymentIntentResponse
import com.usaclean.frenchconnectionuser.model.PaymentMethodsResponse
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class PaymentViewModel(): ViewModel() {

    private val repository = PaymentRepository()

    private val _customerCards = MutableLiveData<PaymentMethodsResponse?>()
    val customerCards: LiveData<PaymentMethodsResponse?> get() = _customerCards

    private val _createCustomer = MutableLiveData<CustomerResponse?>()
    val createCustomer: LiveData<CustomerResponse?> get() = _createCustomer

    private val _createFirstStep = MutableLiveData<FirstStepResponse?>()
    val createFirstStep: LiveData<FirstStepResponse?> get() = _createFirstStep

    private val _attachCard = MutableLiveData<AttachPaymentToCustomerResponse?>()
    val attachCard: LiveData<AttachPaymentToCustomerResponse?> get() = _attachCard

    private val _removeCards = MutableLiveData<PaymentIntentResponse?>()
    val removeCards: LiveData<PaymentIntentResponse?> get() = _removeCards

    private val _getPaymentIntent = MutableLiveData<PaymentIntentResponse?>()
    val getPaymentIntent: LiveData<PaymentIntentResponse?> get() = _getPaymentIntent

    private val _confirmPayment = MutableLiveData<ConfirmPaymentResponse?>()
    val confirmPayment: LiveData<ConfirmPaymentResponse?> get() = _confirmPayment

    private val _connectAccount = MutableLiveData<ResponseBody?>()
    val connectAccount: LiveData<ResponseBody?> get() = _connectAccount

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
/*
    fun cardList(custId: String) {
        viewModelScope.launch {
            val result = repository.customerCardList(custId)
            result.onSuccess { matcher ->
                _customerCards.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }*/

 /*   fun removeCard(pm_id: String) {
        viewModelScope.launch {
            val result = repository.removeCard(pm_id)
            result.onSuccess { matcher ->
                _removeCards.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }*/

  /*  fun createFirstStep(customerKey: String) {
        viewModelScope.launch {
            val result = repository.createFirstStep(customerKey)
            result.onSuccess { matcher ->
                _createFirstStep.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }*/

    fun attachCard(customerID: String, paymentMethodId: String) {
        viewModelScope.launch {
            val result = repository.attachCard(customerID, paymentMethodId)
            result.onSuccess { matcher ->
                _attachCard.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }

   /* fun getPaymentIntent(amount: Int, customerId: String, paymentMethodId: String) {
        viewModelScope.launch {
            val result = repository.createPaymentIntent(amount, customerId, paymentMethodId)
            result.onSuccess { matcher ->
                _getPaymentIntent.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }*/

    fun confirmPayment(paymentMethodId: String) {
        viewModelScope.launch {
            val result = repository.confirmPaymentIntent(paymentMethodId)
            result.onSuccess { matcher ->
                _confirmPayment.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }

    fun setConnectWork(map : HashMap<String, Any>) {
        viewModelScope.launch {
            val result = repository.connectAccount(map)
            result.onSuccess { matcher ->
                _connectAccount.value = matcher
            }.onFailure { error ->
                _error.value = error.message
            }
        }
    }

}