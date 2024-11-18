package com.usaclean.frenchconnectionuser.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.databinding.ActivityEditProfileBinding
import com.usaclean.frenchconnectionuser.utils.UserSession

class EditProfileActivity : AppCompatActivity() {

    private val binding: ActivityEditProfileBinding by lazy {
        ActivityEditProfileBinding.inflate(layoutInflater)
    }

    private var imageURI: Uri? = null
    private val mAuth = Firebase.auth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            userNameEt.setText(UserSession.user.userName)
            emailEt.setText(UserSession.user.email)
            passwordEt.setText(UserSession.user.password)
            phoneEt.setText(UserSession.user.phoneNumber)
            if (UserSession.user.image.equals("")) {
                profileIV.setImageResource(R.drawable.chat_dummy)
            } else {
                Glide.with(this@EditProfileActivity).load(UserSession.user.image).into(profileIV)
            }

            backBtn.setOnClickListener {
                finish()
            }
            profileIV.setOnClickListener {
                openGalleryOrFilePicker()
            }

            saveBtn.setOnClickListener {
                saveInfo(
                    userNameEt.text.toString(),
                    passwordEt.text.toString(),
                    emailEt.text.toString(),
                    phoneEt.text.toString()
                )
            }
        }
    }

    private fun openGalleryOrFilePicker() {
        ImagePicker.with(this)
            .cropSquare()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Glide.with(this).load(data?.data).into(binding.profileIV)
            imageURI = data!!.data
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveInfo(
        userName: String,
        password: String,
        email: String,
        phone: String
    ) {

        binding.progressBar.visibility = View.VISIBLE
        if (imageURI != null) {
            val storage =
                FirebaseStorage.getInstance().reference.child("Users/${mAuth.currentUser!!.uid}.jpg")

            var uploadTask = storage.putFile(imageURI!!)
            uploadTask.addOnSuccessListener {
                storage.downloadUrl.addOnSuccessListener {
                    val userUpdate = hashMapOf(
                        "userName" to userName,
                        "password" to password,
                        "email" to email,
                        "phoneNumber" to phone,
                        "image" to it.toString()
                    ) as Map<String, Any>
                    UserSession.user.image = it.toString()

                    db.collection("Users").document(mAuth.currentUser!!.uid).update(userUpdate)
                        .addOnSuccessListener {
                            UserSession.user.userName = userName
                            UserSession.user.email = email
                            UserSession.user.phoneNumber = phone
                            UserSession.user.password = password
                            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT)
                                .show()
                            if(password.isEmpty()){
                                updatePassword(password)
                            }
                        }.addOnFailureListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this,
                                it.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }.addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } else {
            val userUpdate = hashMapOf(
                "firstName" to userName,
                "password" to password,
                "email" to email,
                "phoneNumber" to phone
            ) as Map<String, Any>
            db.collection("Users").document(mAuth.currentUser!!.uid).update(userUpdate)
                .addOnSuccessListener {
                    UserSession.user.userName = userName
                    UserSession.user.email = email
                    UserSession.user.phoneNumber = phone
                    UserSession.user.password = password
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT)
                        .show()
                    if(password.isNotEmpty()){
                        updatePassword(password)
                    }
                }.addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        it.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }

    private fun updatePassword(password: String) {
        binding.progressBar.visibility = View.VISIBLE
        val credential =
            EmailAuthProvider.getCredential(UserSession.user.email!!, UserSession.user.password!!)
        Firebase.auth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    Firebase.auth.currentUser!!.updatePassword(password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.progressBar.visibility = View.GONE
                                Log.d("LOGGER!", "Password updated successfully.")
                            } else {
                                binding.progressBar.visibility = View.GONE
                                Log.e(
                                    "LOGGER!",
                                    "Error updating password: ${task.exception?.message}"
                                )
                                Toast.makeText(
                                    this,
                                    "${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "${reauthTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("LOGGER!", "Re-authentication failed: ${reauthTask.exception?.message}")
                }
            }
    }
}