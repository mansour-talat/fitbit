package com.su.gym.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.su.gym.R
import com.su.gym.databinding.ItemClientBinding
import com.su.gym.model.Client

class ClientsAdapter(
    private val onClientClick: (Client) -> Unit,
    private val onMoreClick: (Client, View) -> Unit
) : ListAdapter<Client, ClientsAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClientViewHolder(
        private val binding: ItemClientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClientClick(getItem(position))
                }
            }

            binding.buttonMore.setOnClickListener { view ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClick(getItem(position), view)
                }
            }
        }

        fun bind(client: Client) {
            binding.apply {
                textViewClientName.text = client.name
                textViewClientEmail.text = client.email
                textViewNextSession.text = "Next session: ${client.membershipStatus}"

                // Load profile image using Glide
                Glide.with(imageViewClientProfile.context)
                    .load(client.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(imageViewClientProfile)
            }
        }
    }

    private class ClientDiffCallback : DiffUtil.ItemCallback<Client>() {
        override fun areItemsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Client, newItem: Client): Boolean {
            return oldItem == newItem
        }
    }
} 