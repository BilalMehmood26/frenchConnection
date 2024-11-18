package com.usaclean.frenchconnectionuser.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.usaclean.frenchconnectionuser.R
import com.usaclean.frenchconnectionuser.adapter.ChatAdapter
import com.usaclean.frenchconnectionuser.databinding.FragmentChatBinding
import com.usaclean.frenchconnectionuser.model.Booking
import com.usaclean.frenchconnectionuser.model.ChatModel
import com.usaclean.frenchconnectionuser.model.Conversation
import com.usaclean.frenchconnectionuser.model.User
import com.usaclean.frenchconnectionuser.utils.UserSession

class ChatFragment : Fragment() {


    private val binding: FragmentChatBinding by lazy {
        FragmentChatBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private lateinit var fragmentContext: Context
    private val chatList: ArrayList<ChatModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        getChatUser()

        return binding.root
    }


    private fun getChatUser() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("Chat").get().addOnSuccessListener { queryDocumentSnapshots ->
            for (document in queryDocumentSnapshots) {
                val lastMessage = document.get("lastMessage") as? Map<String, Any>
                val hasCurrentUser = lastMessage?.get("fromID") as? String
                val hasOpponentUser = lastMessage?.get("toID") as? String

                if (hasCurrentUser != null && hasCurrentUser == Firebase.auth.currentUser!!.uid) {
                    val chatModel = ChatModel(hasOpponentUser, hasCurrentUser, document.id)
                    chatList.add(chatModel)
                    Log.d("Logger", "getChatUser: ${chatList.size} ${document.id}")
                }
            }
            binding.progressBar.visibility = View.GONE
            setChatAdapter()
        }


    }

    private fun setChatAdapter() {
        Log.d("Logger", "setChatAdapter: adapter: ${chatList.size}")
        binding.chatRV.layoutManager = LinearLayoutManager(fragmentContext)
        binding.chatRV.adapter = ChatAdapter(fragmentContext, chatList)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }
}