package com.su.gym

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.su.gym.adapters.ChatMessageAdapter
import com.su.gym.databinding.FragmentChatbotBinding
import com.su.gym.models.ChatMessage
import com.su.gym.services.GeminiService
import kotlinx.coroutines.launch
import java.util.UUID

class ChatbotFragment : Fragment() {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatMessageAdapter
    private val geminiService = GeminiService.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMessageInput()
        
        // Add initial greeting
        lifecycleScope.launch {
            val greeting = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Hello! I'm your AI fitness assistant. I can help you with:\n" +
                      "• Workout plans and routines\n" +
                      "• Exercise techniques and form\n" +
                      "• Nutrition advice\n" +
                      "• Fitness goals and progress\n" +
                      "How can I assist you today?",
                isFromUser = false
            )
            adapter.submitList(listOf(greeting))
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = this@ChatbotFragment.adapter
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        // Enable/disable send button based on input
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.sendButton.isEnabled = !s.isNullOrBlank()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initially disable send button
        binding.sendButton.isEnabled = false
    }

    private fun sendMessage(text: String) {
        // Disable input while processing
        setInputEnabled(false)

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )

        // Add user message to chat
        val currentList = adapter.currentList.toMutableList()
        currentList.add(userMessage)
        adapter.submitList(currentList)
        
        // Clear input
        binding.messageInput.text?.clear()
        binding.recyclerView.smoothScrollToPosition(adapter.itemCount)

        // Show typing indicator
        showTypingIndicator(true)

        // Get AI response
        lifecycleScope.launch {
            try {
                val prompt = """
                    Act as a knowledgeable fitness assistant. You are helping users with their fitness and gym-related questions.
                    Keep responses concise, practical, and focused on fitness, exercise, and health.
                    If the user asks about something unrelated to fitness, politely redirect them to fitness topics.
                    
                    User message: $text
                """.trimIndent()

                val response = geminiService.generateResponse(prompt)
                
                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = response,
                    isFromUser = false
                )

                // Add AI response to chat
                val updatedList = adapter.currentList.toMutableList()
                updatedList.add(aiMessage)
                adapter.submitList(updatedList)
                binding.recyclerView.smoothScrollToPosition(adapter.itemCount)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to get response: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showTypingIndicator(false)
                setInputEnabled(true)
            }
        }
    }

    private fun showTypingIndicator(show: Boolean) {
        binding.typingIndicator.isVisible = show
    }

    private fun setInputEnabled(enabled: Boolean) {
        binding.messageInput.isEnabled = enabled
        binding.sendButton.isEnabled = enabled && !binding.messageInput.text.isNullOrBlank()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 