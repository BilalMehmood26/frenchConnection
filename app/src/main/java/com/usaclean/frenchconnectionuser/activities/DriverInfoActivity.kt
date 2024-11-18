package com.usaclean.frenchconnectionuser.activities

import android.app.AlertDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ActivityDriverInfoBinding

class DriverInfoActivity : AppCompatActivity() {

    private val binding: ActivityDriverInfoBinding by lazy {
        ActivityDriverInfoBinding.inflate(layoutInflater)
    }

    private lateinit var fragmentContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            backBtn.setOnClickListener {
                finish()
            }

            saveDriverBtn.setOnClickListener {
                showCustomDialog()
            }
        }
    }

    private fun showCustomDialog() {
        val dialogView =
            LayoutInflater.from(this@DriverInfoActivity).inflate(R.layout.dialog_save_driver, null)
        val dialogBuilder = AlertDialog.Builder(this@DriverInfoActivity)
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<TextView>(R.id.set_btn).setOnClickListener {
            dialog.dismiss()

        }
    }
}