package com.su.gym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.su.gym.databinding.FragmentTrainerChatBinding
import com.su.gym.databinding.ItemMessageReceivedBinding
import com.su.gym.databinding.ItemMessageSentBinding
import com.su.gym.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class TrainerChatFragment : Fragment() {

    private var _binding: FragmentTrainerChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var chatAdapter: ChatAdapter
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var dateFormat: SimpleDateFormat
    
    private var userId: String? = null

    companion object {
        private const val TAG = "TrainerChatFragment"
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        userId = arguments?.getString("userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Ensure the soft input mode doesn't push content off screen
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Show progress bar while checking authentication
        binding.progressBar.visibility = View.VISIBLE
        
        // Check authentication state
        if (auth.currentUser == null) {
            // Wait for auth state to be ready
            auth.addAuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser != null) {
                    // Auth state is ready, proceed with setup
                    setupUI()
                } else {
                    // Still not authenticated
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Please log in to chat", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_login)
                }
            }
        } else {
            // Already authenticated, proceed with setup
            setupUI()
        }
    }
    
    private fun setupUI() {
        binding.progressBar.visibility = View.GONE
        
        // Ensure message input controls are visible
        binding.layoutMessageInput.visibility = View.VISIBLE
        binding.editTextMessage.visibility = View.VISIBLE
        binding.buttonSend.visibility = View.VISIBLE
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup send button
        setupSendButton()
        
        // Load messages
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
    
    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
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
        val otherUserId = userId

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(context, "Cannot load messages: recipient not specified.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val chatId = getChatId(currentUserId, otherUserId)
        Log.d(TAG, "loadMessages: currentUserId = $currentUserId")
        Log.d(TAG, "loadMessages: otherUserId = $otherUserId")
        Log.d(TAG, "loadMessages: generated chatId = $chatId")

        _binding?.progressBar?.visibility = View.VISIBLE

        // Set up the messages listener directly without checking document existence
        listenerRegistration?.remove() // Remove any existing listener
        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                _binding?.progressBar?.visibility = View.GONE
                
                if (e != null) {
                    if (e.message?.contains("permission") == true) {
                        // This is a new chat, just show empty state
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
                        val timestamp = when (val ts = doc.get("timestamp")) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Long -> ts
                            else -> return@mapNotNull null
                        }
                        ChatMessage(senderId, messageText, timestamp)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to ChatMessage", e)
                        null
                    }
                }?.sortedBy { it.timestamp } ?: emptyList()

                chatAdapter.submitList(messages)
                if (messages.isNotEmpty() && _binding != null) {
                    _binding?.recyclerViewMessages?.post {
                        _binding?.recyclerViewMessages?.scrollToPosition(messages.size - 1)
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
        val otherUserId = userId

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

        // First check trainer status
        checkTrainerStatus(currentUserId) { isTrainer ->
            Log.d(TAG, "sendMessage: trainer status check completed, isTrainer = $isTrainer")
            
            // First ensure the chat document exists
            val chatRef = db.collection("chats").document(chatId)
            chatRef.get()
                .addOnSuccessListener { chatDoc ->
                    if (!chatDoc.exists()) {
                        // Create chat document first
                        val chatData = mapOf(
                            "participants" to participantsList,
                            "createdBy" to currentUserId,
                            "createdAt" to timestamp,
                            "lastMessage" to messageText,
                            "lastMessageTime" to timestamp,
                            "isTrainerChat" to isTrainer
                        )
                        
                        chatRef.set(chatData)
                            .addOnSuccessListener {
                                // Now send the message
                                sendMessageToChat(chatId, currentUserId, otherUserId, messageText, timestamp, isTrainer)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error creating chat document", e)
                                Toast.makeText(context, "Error creating chat: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Chat exists, just send the message
                        sendMessageToChat(chatId, currentUserId, otherUserId, messageText, timestamp, isTrainer)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking chat existence", e)
                    Toast.makeText(context, "Error checking chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkTrainerStatus(userId: String, callback: (Boolean) -> Unit) {
        // Check both trainer collections
        db.collection("trainer").document(userId).get()
            .addOnSuccessListener { trainerDoc ->
                if (trainerDoc.exists()) {
                    Log.d(TAG, "checkTrainerStatus: Found in 'trainer' collection")
                    callback(true)
                } else {
                    // Check trainers collection
                    db.collection("trainers").document(userId).get()
                        .addOnSuccessListener { trainersDoc ->
                            val isTrainer = trainersDoc.exists()
                            Log.d(TAG, "checkTrainerStatus: Found in 'trainers' collection = $isTrainer")
                            callback(isTrainer)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking trainers collection", e)
                            callback(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking trainer collection", e)
                callback(false)
            }
    }

    private fun sendMessageToChat(
        chatId: String,
        senderId: String,
        receiverId: String,
        messageText: String,
        timestamp: com.google.firebase.Timestamp,
        isTrainer: Boolean
    ) {
        val messageData = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "message" to messageText,
            "timestamp" to timestamp,
            "read" to false,
            "isTrainerMessage" to isTrainer
        )

        Log.d(TAG, "sendMessageToChat: preparing to send message with data = $messageData")

        db.collection("chats").document(chatId)
            .collection("messages").add(messageData)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")
                binding.editTextMessage.text?.clear()
                
                // Update chat document with last message info
                db.collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to messageText,
                            "lastMessageTime" to timestamp
                        )
                    )
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating chat last message", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending message", e)
                val errorMessage = when {
                    e.message?.contains("permission") == true -> {
                        Log.e(TAG, "Permission denied error details: isTrainer=$isTrainer, senderId=$senderId")
                        "Permission denied. Please verify your trainer status and try again."
                    }
                    e.message?.contains("offline") == true -> "You are offline. Please check your internet connection."
                    else -> "Failed to send message: ${e.message}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
    }
    
    private fun getChatId(uid1: String, uid2: String): String {
        // Ensure UIDs are not empty before comparison
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the listener when the fragment is destroyed
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        // Also remove listener when fragment is stopped
        listenerRegistration?.remove()
        listenerRegistration = null
    }
    
    inner class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var messages: List<ChatMessage> = emptyList()
        private val currentUserId = auth.currentUser?.uid ?: ""
        
        fun submitList(newMessages: List<ChatMessage>) {
            messages = newMessages
            notifyDataSetChanged()
        }
        
        override fun getItemViewType(position: Int): Int {
            return if (messages[position].senderId == currentUserId) {
                VIEW_TYPE_SENT
            } else {
                VIEW_TYPE_RECEIVED
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_SENT -> {
                    val binding = ItemMessageSentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    SentMessageViewHolder(binding)
                }
                else -> {
                    val binding = ItemMessageReceivedBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ReceivedMessageViewHolder(binding)
                }
            }
        }
        
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            when (holder) {
                is SentMessageViewHolder -> holder.bind(message)
                is ReceivedMessageViewHolder -> holder.bind(message)
            }
        }
        
        override fun getItemCount() = messages.size
        
        inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(message: ChatMessage) {
                binding.textViewMessage.text = message.message
                binding.textViewTime.text = dateFormat.format(Date(message.timestamp))
            }
        }

        inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(message: ChatMessage) {
                binding.textViewMessage.text = message.message
                binding.textViewTime.text = dateFormat.format(Date(message.timestamp))
            }
        }
    }
} 