package com.usaclean.frenchconnectiondriver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.usaclean.frenchconnectionuser.activities.LoginActivity
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.activities.DashboardActivity
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.UserSession

class MainActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            val userId = auth.currentUser!!.uid
            db.collection("Users").document(userId)
                .get().addOnSuccessListener { task ->
                    val user = task.toObject(User::class.java)
                    user!!.id = userId
                    UserSession.user = user
                    val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                    intent.putExtra("user","user")
                    startActivity(intent)
                    finish()
                    overridePendingTransition(
                        androidx.appcompat.R.anim.abc_fade_in,
                        androidx.appcompat.R.anim.abc_fade_out
                    )
                }.addOnFailureListener {
                    Handler().postDelayed(Runnable {
                        startActivity(Intent(this, LoginActivity::class.java))
                        overridePendingTransition(androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
                        finish()
                    }, 2000)
                }
        }else{
            Handler().postDelayed(Runnable {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
                finish()
            }, 2000)
        }
    }
}