package com.usaclean.frenchconnectionuser.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ActivityDashboardBinding
import com.usaclean.frenchconnectionuser.fragment.ChatFragment
import com.usaclean.frenchconnectionuser.fragment.HomeFragment
import com.usaclean.frenchconnectionuser.fragment.ProfileFragment
import com.usaclean.frenchconnectionuser.fragment.SchduleRideFragment
import com.usaclean.frenchconnectionuser.fragment.WalletFragment
import com.usaclean.frenchconnectionuser.utils.UserSession
import org.json.JSONArray
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {


    private val binding : ActivityDashboardBinding by lazy {
        ActivityDashboardBinding.inflate(layoutInflater)
    }

    private val mAuth = Firebase.auth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        clearAllTab()
        setHomeTab()
        setListener()

        askNotificationPermission()
        binding.apply {
            logoutIV.setOnClickListener {
                logout()
            }
        }
    }


    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun logout(){
        val userUpdate = hashMapOf(
            "isOnline" to false
        ) as Map<String, Any>

        db.collection("Users").document(mAuth.currentUser!!.uid).update(userUpdate)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Firebase.auth.signOut()
                startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                overridePendingTransition(androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
                finish()

            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    it.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    private fun setListener() {

        binding.homeTab.setOnClickListener {
            clearAllTab()
            setHomeTab()
        }

        binding.rideTab.setOnClickListener {
            clearAllTab()
            setRideTab()
        }

        binding.chatTab.setOnClickListener {
            clearAllTab()
            setChatTab()
        }

        binding.walletTab.setOnClickListener {
            clearAllTab()
            setWalletTab()
        }

        binding.profileTab.setOnClickListener {
            clearAllTab()
            setProfileTab()
        }
    }

    private fun setHomeTab() {
        binding.homeTab.setImageResource(R.drawable.bottom_home_selected)
        binding.titleTV.text = "Home"
        loadFragment(HomeFragment())
    }

    private fun setRideTab() {
        binding.rideTab.setImageResource(R.drawable.bottom_ride_selected)
        binding.titleTV.text = "Schedule a Ride"
        loadFragment(SchduleRideFragment())
    }

    private fun setChatTab() {
        binding.chatTab.setImageResource(R.drawable.bottom_chat_selected)
        binding.titleTV.text = "Messages"
        loadFragment(ChatFragment())
    }

    private fun setWalletTab() {
        binding.walletTab.setImageResource(R.drawable.bottom_wallet_selected)
        binding.titleTV.text = "Wallet"
        loadFragment(WalletFragment())
    }

    private fun setProfileTab() {
        binding.profileTab.setImageResource(R.drawable.bottom_profile_selected)
        binding.logoutIV.visibility = View.VISIBLE
        binding.titleTV.text = "Profile"
        loadFragment(ProfileFragment())
    }

    private fun clearAllTab() {
        binding.logoutIV.visibility = View.GONE
        binding.homeTab.setImageResource(R.drawable.bottom_home_unselected)
        binding.rideTab.setImageResource(R.drawable.bottom_ride_unselected)
        binding.chatTab.setImageResource(R.drawable.bottom_chat_unselected)
        binding.walletTab.setImageResource(R.drawable.bottom_wallet_unselected)
        binding.profileTab.setImageResource(R.drawable.bottom_profile_unselected)
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(androidx.appcompat.R.anim.abc_fade_in, com.google.android.material.R.anim.abc_fade_out)
        transaction.replace(binding.container.id, fragment)
        transaction.commit()
    }
}