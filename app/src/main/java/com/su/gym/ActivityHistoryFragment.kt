package com.su.gym

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.su.gym.databinding.FragmentActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.su.gym.utils.formatDuration

// Move ActivityHistoryItem outside the fragment class
data class ActivityHistoryItem(
    val date: String = "",
    val steps: Long = 0,
    val caloriesBurned: Double = 0.0,
    val activityTime: Long = 0,
    val distance: Double = 0.0
)

class ActivityHistoryFragment : Fragment() {

    private var _binding: FragmentActivityHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var activityAdapter: ActivityHistoryAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        setupRecyclerView()
        loadActivityHistory()
    }
    
    private fun setupRecyclerView() {
        activityAdapter = ActivityHistoryAdapter()
        binding.activityRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = activityAdapter
        }
    }
    
    private fun loadActivityHistory() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        db.collection("activity")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                activityAdapter.submitList(emptyList())
                val activities = mutableListOf<ActivityHistoryItem>()
                for (document in documents) {
                    val activity = document.toObject(ActivityHistoryItem::class.java)
                    activities.add(activity)
                }
                activityAdapter.submitList(activities)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading activity history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    inner class ActivityHistoryAdapter : RecyclerView.Adapter<ActivityHistoryAdapter.ActivityViewHolder>() {
        
        private var activities = listOf<ActivityHistoryItem>()
        
        fun submitList(newActivities: List<ActivityHistoryItem>) {
            activities = newActivities
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_activity_history, parent, false)
            return ActivityViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
            val activity = activities[position]
            holder.bind(activity)
        }
        
        override fun getItemCount() = activities.size
        
        inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dateText: TextView = itemView.findViewById(R.id.historyDateText)
            private val stepsText: TextView = itemView.findViewById(R.id.historyStepsText)
            private val caloriesText: TextView = itemView.findViewById(R.id.historyCaloriesText)
            private val distanceText: TextView = itemView.findViewById(R.id.historyDistanceText)
            private val durationText: TextView = itemView.findViewById(R.id.historyDurationText)
            
            fun bind(activity: ActivityHistoryItem) {
                // Format date
                val formattedDate = try {
                    val date = dateFormat.parse(activity.date)
                    dateFormat.format(date ?: Date())
                } catch (e: Exception) {
                    activity.date
                }
                dateText.text = formattedDate
                stepsText.text = "${activity.steps} steps"
                caloriesText.text = "${activity.caloriesBurned} kcal"
                distanceText.text = String.format("%.2f km", activity.distance)
                durationText.text = formatDuration(activity.activityTime * 1000)
            }
        }
    }
} 