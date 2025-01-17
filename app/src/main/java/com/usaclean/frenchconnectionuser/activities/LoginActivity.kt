package com.usaclean.frenchconnectionuser.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.usaclean.frenchconnectionuser.databinding.ActivityLoginBinding
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.LocationUtility
import com.usaclean.frenchconnectionuser.utils.UserSession

class LoginActivity : AppCompatActivity() {

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    private val auth = Firebase.auth
    val db = Firebase.firestore
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100
    private var token = ""
    val REQUEST_CODE = 1000
    private lateinit var locationUtility: LocationUtility
    private var userLat = 0.0
    private var userLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1076746183604-5umj2odp24s66qtemltoj1d840kj39kt.apps.googleusercontent.com")
            .requestEmail()
            .build()

        if (ActivityCompat.checkSelfPermission(
                this@LoginActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@LoginActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this@LoginActivity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_CODE
            )
        }
        locationUtility = LocationUtility(this@LoginActivity)
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        setListener()

    }
    private fun setListener() {

        binding.signInTV.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            when {
                email.isEmpty() -> binding.emailEt.error = "Required"
                password.isEmpty() -> binding.passwordEt.error = "Required"
                else -> login(email, password)
            }
        }

        binding.signUpTV.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(
                androidx.appcompat.R.anim.abc_fade_in,
                androidx.appcompat.R.anim.abc_fade_out
            )
        }

        binding.googleBtn.setOnClickListener {
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                val signInIntent = mGoogleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            binding.progressBar.visibility = View.VISIBLE
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    val idToken = account.idToken

                    FirebaseMessaging.getInstance().token.addOnCompleteListener {
                        token = it.result
                        Log.d("GoogleSignIn", "token : $token");
                    }
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val userId = auth.currentUser?.uid.toString()

                            Log.d("GoogleSignIn", "Google Auth Success: $userId")
                            db.collection("Users").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        Log.d("GoogleSignIn", "Google Auth User Exists")
                                        getUserDetails(userId)
                                    } else {
                                        Log.d("GoogleSignIn", "Google Auth New User")
                                        signUpDetails(
                                            account.displayName.toString(),
                                            account.email.toString(),
                                            token,
                                            account.idToken!!,
                                            account.photoUrl.toString()
                                        )
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d(
                                        "GoogleSignIn",
                                        "Error fetching user: ${exception.message}"
                                    )
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Failed to fetch user data.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.d("GoogleSignIn", "Google Auth Failed: ${exception.message}")
                            Toast.makeText(
                                this@LoginActivity,
                                "Authentication Failed: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                }
            } catch (e: ApiException) {
                // If sign in fails, display a message to the user.
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                Log.e("GoogleSignIn", "Google sign-in failed with ApiException: ${e.statusCode}")
                e.printStackTrace()
            }
        }
    }

    private fun signUpDetails(
        fullName: String,
        email: String,
        token: String,
        authToken: String,
        image: String
    ) {

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
                "image" to image,
                "token" to token,
                "userDate" to System.currentTimeMillis(),
                "authToken" to authToken,
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
                image =  image,
                token = token,
                authToken = authToken,
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
                    Toast.makeText(this@LoginActivity, it.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            locationUtility.removeLocationUpdates()
        }
    }

    private fun login(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener {
                    updateToken(it, task.result.user!!.uid)
                }.addOnFailureListener {
                    Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
                    getUserDetails(task.result.user!!.uid)
                }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateToken(token: String, userID: String) {
        val user = hashMapOf(
            "token" to token,
            "isOnline" to true
        ) as Map<String, Any>
        db.collection("Users").document(userID).update(user)
            .addOnSuccessListener {
                getUserDetails(userID)
            }.addOnFailureListener {
                Toast.makeText(this@LoginActivity, it.message.toString(), Toast.LENGTH_SHORT)
                    .show()
                getUserDetails(userID)
            }
    }

    private fun getUserDetails(uid: String) {
        db.collection("Users").document(uid).get().addOnSuccessListener { response ->

            if (response.exists()) {
                binding.progressBar.visibility = View.GONE
                val user = response.toObject(User::class.java)
                UserSession.user = user!!
                if (user.userRole.equals("user")) {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(
                        androidx.appcompat.R.anim.abc_fade_in,
                        androidx.appcompat.R.anim.abc_fade_out
                    )
                } else {
                    com.google.firebase.Firebase.auth.signOut()
                    Toast.makeText(this, "Access not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { error ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
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