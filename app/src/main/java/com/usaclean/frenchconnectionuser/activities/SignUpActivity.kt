package com.usaclean.frenchconnectionuser.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.usaclean.frenchconnectionuser.databinding.ActivitySignUpBinding
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.LocationUtility
import com.usaclean.frenchconnectionuser.utils.UserSession

class SignUpActivity : AppCompatActivity() {

    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }


    private lateinit var locationUtility: LocationUtility
    val REQUEST_CODE = 1000
    private var userLat = 0.0
    private var userLng = 0.0

    private val auth = Firebase.auth
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setListener()
        if (ActivityCompat.checkSelfPermission(
               this@SignUpActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@SignUpActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this@SignUpActivity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_CODE
            )
        }
        locationUtility = LocationUtility(this@SignUpActivity)

        binding.apply {
            signUpTV.setOnClickListener {
                val email = emailEt.text.toString()
                val userName = fulNameEt.text.toString()
                val password = passwordEt.text.toString()

                when {
                    userName.isEmpty() -> fulNameEt.error = "Required"
                    email.isEmpty() -> emailEt.error = "Required"
                    password.isEmpty() -> passwordEt.error = "Required"
                    else -> signUp(userName, email, password)
                }
            }
        }

    }


    private fun location() {
        locationUtility.requestLocationUpdates { currentLocation ->
            userLat = currentLocation.latitude
            userLng = currentLocation.longitude
            locationUtility.removeLocationUpdates()
        }
    }


    private fun setListener() {
        binding.signInTV.setOnClickListener { onBackPressed() }
    }

    private fun signUp(fullName: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                userDetails(fullName, email, password,it)
            }.addOnFailureListener {
                Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
                userDetails(fullName, email, password,"")
            }
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this@SignUpActivity, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun userDetails(fullName: String, email: String, password: String,token:String) {

        locationUtility.requestLocationUpdates { currentLocation ->
            userLat = currentLocation.latitude
            userLng = currentLocation.longitude

            val user = hashMapOf(
                "deviceType" to "Android",
                "email" to email,
                "id" to Firebase.auth.currentUser!!.uid,
                "isOnline" to true,
                "lat" to userLat,
                "lng" to userLng,
                "userName" to fullName,
                "token" to token,
                "userDate" to System.currentTimeMillis(),
                "password" to password,
                "userRole" to "user"
            )

            var userModel = User(
                deviceType = "Android",
                email = email,
                id = Firebase.auth.currentUser!!.uid,
                isOnline = true,
                lat = userLat,
                lng = userLng,
                userName = fullName,
                token = token,
                userDate = System.currentTimeMillis(),
                userRole = "user"
            )
            db.collection("Users").document(Firebase.auth.currentUser!!.uid).set(user)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    UserSession.user = userModel
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(
                        androidx.appcompat.R.anim.abc_fade_in,
                        androidx.appcompat.R.anim.abc_fade_out
                    )
                }.addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@SignUpActivity, it.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            locationUtility.removeLocationUpdates()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            androidx.appcompat.R.anim.abc_fade_in,
            androidx.appcompat.R.anim.abc_fade_out
        )
    }
}