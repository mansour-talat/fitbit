package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.FragmentTrainerChatListBinding

class TrainerChatListFragment : Fragment() {

    private var _binding: FragmentTrainerChatListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userAdapter: ChatUserAdapter
    
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Check if user is authenticated
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to view chats", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load chat users
        loadChatUsers()
    }
    
    private fun setupRecyclerView() {
        userAdapter = ChatUserAdapter { userId ->
            // Navigate to chat with selected user
            val action = TrainerChatListFragmentDirections
                .actionTrainerChatListToTrainerChat(userId)
            findNavController().navigate(action)
        }
        
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }
    }
    
    private fun loadChatUsers() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    binding.textViewNoUsers.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                val chatUsers = mutableListOf<ChatUser>()
                for (document in documents) {
                    val participants = document.get("participants") as? List<*>
                    val otherUserId = participants?.firstOrNull { it != currentUserId } as? String ?: continue
                    val lastMessage = document.getString("lastMessage") ?: ""
                    
                    // Handle different timestamp formats
                    val lastMessageTime = when (val timestamp = document.get("lastMessageTime")) {
                        is com.google.firebase.Timestamp -> timestamp.toDate().time
                        is Long -> timestamp
                        else -> System.currentTimeMillis() // Default to current time if format is unknown
                    }

                    db.collection("users")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val userName = userDoc.getString("name") ?: "User"
                            val userEmail = userDoc.getString("email") ?: ""
                            chatUsers.add(
                                ChatUser(
                                    id = otherUserId,
                                    name = userName,
                                    email = userEmail,
                                    lastMessage = lastMessage,
                                    lastMessageTime = lastMessageTime
                                )
                            )
                            updateUI(chatUsers)
                        }
                        .addOnFailureListener { e ->
                            updateUI(chatUsers)
                            Toast.makeText(context, "Error loading user details: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(chatUsers: List<ChatUser>) {
        if (chatUsers.isEmpty()) {
            binding.textViewNoUsers.visibility = View.VISIBLE
        } else {
            binding.textViewNoUsers.visibility = View.GONE
            // Sort users by last message time
            val sortedUsers = chatUsers.sortedByDescending { it.lastMessageTime }
            userAdapter.submitList(sortedUsers)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    data class ChatUser(
        val id: String,
        val name: String,
        val email: String,
        val lastMessage: String,
        val lastMessageTime: Long
    )
    
    inner class ChatUserAdapter(
        private val onUserClick: (String) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ChatUserAdapter.ChatUserViewHolder>() {
        
        private val users = mutableListOf<ChatUser>()
        
        fun submitList(newUsers: List<ChatUser>) {
            users.clear()
            users.addAll(newUsers)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_user, parent, false)
            return ChatUserViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ChatUserViewHolder, position: Int) {
            val user = users[position]
            holder.bind(user)
        }
        
        override fun getItemCount(): Int = users.size
        
        inner class ChatUserViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val nameTextView: android.widget.TextView = itemView.findViewById(R.id.textViewUserName)
            private val lastMessageTextView: android.widget.TextView = itemView.findViewById(R.id.textViewLastMessage)
            private val timeTextView: android.widget.TextView = itemView.findViewById(R.id.textViewLastMessageTime)
            
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onUserClick(users[position].id)
                    }
                }
            }
            
            fun bind(user: ChatUser) {
                nameTextView.text = user.name
                lastMessageTextView.text = user.lastMessage
                
                // Format the timestamp
                val dateFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                timeTextView.text = dateFormat.format(java.util.Date(user.lastMessageTime))
            }
        }
    }
} 