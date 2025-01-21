package com.usaclean.frenchconnectionuser.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.adapter.ConversationAdapter
import com.usaclean.frenchconnectionuser.databinding.ActivityConversationBinding
import com.usaclean.frenchconnectionuser.model.Conversation
import com.usaclean.frenchconnectionuser.utils.UserSession
import java.util.UUID

class ConversationActivity : AppCompatActivity() {

    private val binding: ActivityConversationBinding by lazy {
        ActivityConversationBinding.inflate(layoutInflater)
    }

    private var conversationModelList: ArrayList<Conversation> = ArrayList()
    private var toID = ""
    private var messageId = ""
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        toID = intent.getStringExtra("driverID")!!
        messageId = intent.getStringExtra("messageId")!!
        setListener()
        getChat()
        binding.sendBtn.setOnClickListener {
            if (!binding.messageTv.text.isEmpty()) {
                sendMessage(binding.messageTv.text.toString())
            }
        }


    }

    private fun setAdapter() {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding.recyclerView.adapter = ConversationAdapter(this, conversationModelList)
    }

    private fun setListener() {
        binding.backIV.setOnClickListener { onBackPressed() }
    }

    private fun getChat() {
        binding.progressBar.visibility = View.VISIBLE
        if (messageId.isEmpty()) {
            db.collection("Chat").document(messageId).get().addOnCompleteListener {

            }
           /* db.collection("Chat").get().addOnSuccessListener { queryDocumentSnapshots ->
                for (document in queryDocumentSnapshots) {
                    binding.progressBar.visibility = View.GONE
                    val participantMap = document.get("participants") as Map<String, Any>?

                    val hasCurrentUser = participantMap?.containsKey(UserSession.user.id) == true
                    val hasOpponent = participantMap?.containsKey(toID) == true

                    if (hasCurrentUser && hasOpponent) {
                        val documentId = document.id
                        toID = documentId
                        Log.d("LOGGER", "ID: $documentId")
                        break
                    }
                }
                addRealtimeListener()
            }*/
        } else {
            addRealtimeListener()
        }
    }

    private fun addRealtimeListener() {
        if (!messageId.isNullOrEmpty()) {
            FirebaseFirestore.getInstance().collection("Chat")
                .document(messageId).collection("Conversation")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { queryDocumentSnapshots, e ->
                    if (e != null) {
                        // Handle the error
                        Toast.makeText(this@ConversationActivity, e.message.toString(), Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        return@addSnapshotListener
                    }

                    conversationModelList.clear()
                    binding.progressBar.visibility = View.GONE
                    for (documentSnapshot in queryDocumentSnapshots ?: emptyList()) {
                        val model = documentSnapshot.toObject(Conversation::class.java)
                        if(model.content.isNotEmpty()){
                            conversationModelList.add(model)
                        }
                    }
                    setAdapter()
                }
        }
    }

    private fun sendMessage(message: String) {
        val id = UUID.randomUUID().toString()
        val timeStamp = System.currentTimeMillis()

        val messageMap = hashMapOf(
            "content" to message,
            "fromID" to Firebase.auth.currentUser!!.uid,
            "toID" to toID,
            "messageId" to messageId,
            "read" to false,
            "timestamp" to timeStamp,
            "type" to "text"
        )

        val participents = hashMapOf(
            UserSession.user.id to true,
            toID to true
        )


        val lastMessageMap = hashMapOf(
            "lastMessage" to messageMap,
            "participants" to participents,
            "chatType" to "one"
        )

        FirebaseFirestore.getInstance().collection("Chat").document(messageId)
            .update(lastMessageMap as Map<String, Any>)
        FirebaseFirestore.getInstance().collection("Chat").document(messageId)
            .collection("Conversation").document(id).set(messageMap)

        val isRead = hashMapOf(
            Firebase.auth.currentUser!!.uid to false,
            toID to false,
        )

        val notification = hashMapOf(
            "message" to message,
            "orderId" to messageId,
            "isRead" to isRead,
            "timestamp" to System.currentTimeMillis(),
            "title" to "New Message",
            "type" to "chat",
            "userId" to Firebase.auth.currentUser!!.uid,
        )
        db.collection("Notification").document().set(notification).addOnSuccessListener {
            binding.progressBar.visibility = View.GONE
            Log.d("LOGGER", "in Notificiation success")
        }.addOnFailureListener {
            Log.d("LOGGER", "in Notificaiton Fail")
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this@ConversationActivity, it.message.toString(), Toast.LENGTH_SHORT).show()
        }
        binding.messageTv.setText("")

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            androidx.appcompat.R.anim.abc_fade_in,
            androidx.appcompat.R.anim.abc_fade_out
        )
    }

}