package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.adapter.ClientsAdapter
import com.su.gym.databinding.FragmentTrainerClientsBinding
import com.su.gym.model.Client

class TrainerClientsFragment : Fragment() {

    private var _binding: FragmentTrainerClientsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var clientsAdapter: ClientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerClientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        setupRecyclerView()

        // Load clients
        loadClients()

        // Setup FAB
        binding.fabAddClient.setOnClickListener {
            // TODO: Implement add client functionality
            Toast.makeText(requireContext(), "Add client functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        clientsAdapter = ClientsAdapter(
            onClientClick = { client ->
                // TODO: Implement client details view
                Toast.makeText(requireContext(), "View ${client.name}'s details", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { client, view ->
                // TODO: Show popup menu with options
                Toast.makeText(requireContext(), "More options for ${client.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewClients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clientsAdapter
        }
    }

    private fun loadClients() {
        binding.progressBar.visibility = View.VISIBLE

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("trainers").document(user.uid)
                .collection("clients")
                .get()
                .addOnSuccessListener { documents ->
                    val clients = documents.mapNotNull { doc ->
                        val client = doc.toObject(Client::class.java)
                        client.copy(id = doc.id)
                    }
                    clientsAdapter.submitList(clients)
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading clients: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 