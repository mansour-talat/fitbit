package com.su.gym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.su.gym.adapter.ChatAdapter
import com.su.gym.databinding.FragmentChatBinding
import com.su.gym.model.ChatMessage
import com.google.firebase.firestore.ListenerRegistration

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val args by navArgs<ChatFragmentArgs>()
    private lateinit var chatAdapter: ChatAdapter
    private var listenerRegistration: ListenerRegistration? = null

    companion object {
        private const val TAG = "ChatFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Ensure the soft input mode doesn't push content off screen
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        
        // Log the trainer ID from args
        Log.d(TAG, "Trainer ID from args: ${args.trainerId}")
        
        // Ensure the input area is visible
        binding.layoutMessageInput.visibility = View.VISIBLE
        binding.editTextMessage.visibility = View.VISIBLE
        binding.buttonSend.visibility = View.VISIBLE
        
        setupRecyclerView()
        setupMessageInput()
        loadMessages()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupMessageInput() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text?.toString()?.trim()
            if (!messageText.isNullOrEmpty()) {
                sendMessage(messageText)
                binding.editTextMessage.text?.clear()
            }
        }
    }

    private fun loadMessages() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to view messages.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid
        val otherUserId = args.trainerId

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(context, "Cannot load messages: recipient not specified.", Toast.LENGTH_SHORT).show()
            return
        }

        val chatId = getChatId(currentUserId, otherUserId)
        Log.d(TAG, "loadMessages: currentUserId = $currentUserId")
        Log.d(TAG, "loadMessages: otherUserId = $otherUserId")
        Log.d(TAG, "loadMessages: generated chatId = $chatId")

        _binding?.progressBar?.visibility = View.VISIBLE

        // Create initial chat document if needed
        val chatRef = db.collection("chats").document(chatId)
        val initialChatData = hashMapOf(
            "participants" to listOf(currentUserId, otherUserId),
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        chatRef.set(initialChatData, SetOptions.merge())
            .addOnSuccessListener {
                // Set up message listener after ensuring chat document exists
                setupMessageListener(chatId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error initializing chat", e)
                _binding?.progressBar?.visibility = View.GONE
                // Try to set up message listener anyway
                setupMessageListener(chatId)
            }
    }

    private fun setupMessageListener(chatId: String) {
        // Remove any existing listener
        listenerRegistration?.remove()
        
        // Set up the messages listener
        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                _binding?.progressBar?.visibility = View.GONE
                
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e)
                    if (e.message?.contains("permission") == true) {
                        // Handle permission error gracefully
                        chatAdapter.submitList(emptyList())
                    } else {
                        Toast.makeText(context, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
                        val messageText = doc.getString("message") ?: return@mapNotNull null
                        val timestamp = doc.get("timestamp")
                        val timestampMillis = when (timestamp) {
                            is com.google.firebase.Timestamp -> timestamp.seconds * 1000
                            is Long -> timestamp
                            else -> return@mapNotNull null
                        }
                        ChatMessage(senderId, messageText, timestampMillis)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to ChatMessage", e)
                        null
                    }
                }?.sortedBy { it.timestamp } ?: emptyList()

                chatAdapter.submitList(messages) {
                    if (messages.isNotEmpty() && _binding != null) {
                        _binding?.recyclerViewMessages?.post {
                            _binding?.recyclerViewMessages?.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
    }

    private fun sendMessage(messageText: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to send messages.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid
        val otherUserId = args.trainerId

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(context, "Cannot send message: recipient not specified.", Toast.LENGTH_SHORT).show()
            return
        }

        val chatId = getChatId(currentUserId, otherUserId)
        val timestamp = com.google.firebase.Timestamp.now()

        Log.d(TAG, "sendMessage: currentUserId = $currentUserId")
        Log.d(TAG, "sendMessage: otherUserId = $otherUserId")
        Log.d(TAG, "sendMessage: generated chatId = $chatId")
        val participantsList = listOf(currentUserId, otherUserId)
        Log.d(TAG, "sendMessage: participantsList = $participantsList")

        // Create initial chat document if it doesn't exist
        val chatRef = db.collection("chats").document(chatId)
        val initialChatData = hashMapOf(
            "participants" to participantsList,
            "createdBy" to currentUserId,
            "createdAt" to timestamp,
            "lastMessage" to messageText,
            "lastMessageTime" to timestamp
        )

        // Message data
        val messageData = hashMapOf(
            "senderId" to currentUserId,
            "message" to messageText,
            "timestamp" to timestamp,
            "read" to false
        )

        // Create a batch operation
        val batch = db.batch()
        
        // Set the chat document with merge option
        batch.set(chatRef, initialChatData, SetOptions.merge())
        
        // Add the message to the messages subcollection
        val messageRef = chatRef.collection("messages").document()
        batch.set(messageRef, messageData)

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")
                binding.editTextMessage.text?.clear()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending message", e)
                Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }

    private fun getChatId(uid1: String, uid2: String): String {
        if (uid1.isEmpty() || uid2.isEmpty()) {
            throw IllegalArgumentException("UIDs cannot be empty for chat ID generation")
        }
        val chatId = if (uid1 < uid2) {
            "${uid1}_${uid2}"
        } else {
            "${uid2}_${uid1}"
        }
        Log.d(TAG, "Generated chat ID: $chatId")
        return chatId
    }
} 