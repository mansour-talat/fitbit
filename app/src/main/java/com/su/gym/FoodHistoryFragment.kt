package com.su.gym

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.su.gym.models.FoodItem
import com.su.gym.services.NutritionService
import com.su.gym.utils.format
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FoodHistoryFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val nutritionService = NutritionService()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private lateinit var todayFoodsRecyclerView: RecyclerView
    private lateinit var pastDaysRecyclerView: RecyclerView
    private lateinit var addFoodButton: MaterialButton
    
    private val todayFoods = mutableListOf<FoodItem>()
    private val pastDays = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_food_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        todayFoodsRecyclerView = view.findViewById(R.id.todayFoodsRecyclerView)
        pastDaysRecyclerView = view.findViewById(R.id.pastDaysRecyclerView)
        addFoodButton = view.findViewById(R.id.addFoodButton)
        
        setupRecyclerViews()
        loadTodayFoods()
        loadPastDays()
        setupAddFoodButton()
    }

    private fun setupAddFoodButton() {
        addFoodButton.setOnClickListener {
            showAddFoodDialog()
        }
    }

    private fun showAddFoodDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_food)
        
        // Set dialog window size
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val foodNameInput = dialog.findViewById<TextInputEditText>(R.id.foodNameInput)
        val quantityInput = dialog.findViewById<TextInputEditText>(R.id.quantityInput)
        val searchButton = dialog.findViewById<MaterialButton>(R.id.searchButton)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val saveButton = dialog.findViewById<MaterialButton>(R.id.saveButton)
        val nutritionInfoText = dialog.findViewById<TextView>(R.id.nutritionInfoText)

        searchButton.setOnClickListener {
            val foodName = foodNameInput.text.toString()
            val quantity = quantityInput.text.toString().toDoubleOrNull() ?: 0.0
            
            if (foodName.isNotEmpty() && quantity > 0) {
                progressBar.visibility = View.VISIBLE
                searchButton.isEnabled = false
                
                lifecycleScope.launch {
                    try {
                        val foods = nutritionService.searchFood(foodName, quantity)
                        if (foods.isNotEmpty()) {
                            val food = foods[0]
                            nutritionInfoText.text = """
                                Calories: ${food.calories.format(1)} kcal
                                Protein: ${food.protein.format(1)}g
                                Carbs: ${food.carbs.format(1)}g
                                Fat: ${food.fat.format(1)}g
                            """.trimIndent()
                            nutritionInfoText.visibility = View.VISIBLE
                            saveButton.isEnabled = true
                        } else {
                            Toast.makeText(context, "No nutrition data found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressBar.visibility = View.GONE
                        searchButton.isEnabled = true
                    }
                }
            } else {
                Toast.makeText(context, "Please enter food name and quantity", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            val foodName = foodNameInput.text.toString()
            val quantity = quantityInput.text.toString().toDoubleOrNull() ?: 0.0
            
            if (foodName.isNotEmpty() && quantity > 0) {
                addFood(foodName, quantity)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter food name and quantity", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addFood(name: String, quantity: Double) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "You must be logged in to add food", Toast.LENGTH_SHORT).show()
            return
        }
        
        val today = dateFormat.format(Date())
        Log.d("FoodHistoryFragment", "Adding food for user: $userId")

        lifecycleScope.launch {
            try {
                val foods = nutritionService.searchFood(name, quantity)
                if (foods.isNotEmpty()) {
                    val foodItem = foods[0]
                    val foodData = hashMapOf(
                        "userId" to userId,
                        "date" to today,
                        "name" to foodItem.name,
                        "quantity" to foodItem.quantity,
                        "calories" to foodItem.calories,
                        "protein" to foodItem.protein,
                        "carbs" to foodItem.carbs,
                        "fat" to foodItem.fat
                    )

                    Log.d("FoodHistoryFragment", "Attempting to save food data: $foodData")

                    db.collection("nutrition")
                        .add(foodData)
                        .addOnSuccessListener { documentReference ->
                            Log.d("FoodHistoryFragment", "Food added successfully with ID: ${documentReference.id}")
                            val newFoodItem = FoodItem(
                                id = documentReference.id,
                                name = foodItem.name,
                                quantity = foodItem.quantity,
                                calories = foodItem.calories,
                                protein = foodItem.protein,
                                carbs = foodItem.carbs,
                                fat = foodItem.fat
                            )
                            todayFoods.add(newFoodItem)
                            todayFoodsRecyclerView.adapter?.notifyDataSetChanged()
                            Toast.makeText(context, "${foodItem.name} added successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FoodHistoryFragment", "Error adding food", e)
                            Log.e("FoodHistoryFragment", "Error details: ${e.message}")
                            Toast.makeText(context, "Failed to add food: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                } else {
                    Toast.makeText(context, "No nutrition data found for $name", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FoodHistoryFragment", "Error in addFood", e)
                Log.e("FoodHistoryFragment", "Error details: ${e.message}")
                Toast.makeText(context, "Error adding food: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun setupRecyclerViews() {
        todayFoodsRecyclerView.layoutManager = LinearLayoutManager(context)
        todayFoodsRecyclerView.adapter = FoodAdapter(todayFoods, { foodItem -> deleteFood(foodItem) })
        
        pastDaysRecyclerView.layoutManager = LinearLayoutManager(context)
        pastDaysRecyclerView.adapter = PastDaysAdapter(pastDays) { pastDay -> showDayDetails(pastDay) }
    }

    private fun loadTodayFoods() {
        val userId = auth.currentUser?.uid ?: return
        val today = dateFormat.format(Date())
        
        db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { documents ->
                todayFoods.clear()
                for (document in documents) {
                    val foodItem = FoodItem(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        quantity = document.getDouble("quantity") ?: 0.0,
                        calories = document.getDouble("calories") ?: 0.0,
                        protein = document.getDouble("protein") ?: 0.0,
                        carbs = document.getDouble("carbs") ?: 0.0,
                        fat = document.getDouble("fat") ?: 0.0
                    )
                    todayFoods.add(foodItem)
                }
                todayFoodsRecyclerView.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load today's foods", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPastDays() {
        val userId = auth.currentUser?.uid ?: return
        val today = dateFormat.format(Date())
        
        // First get all nutrition documents for the user
        db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val daysMap = mutableMapOf<String, String>()
                
                for (document in documents) {
                    val date = document.getString("date") ?: continue
                    val calories = document.getDouble("calories") ?: 0.0
                    
                    // Only include dates before today
                    if (date < today) {
                        daysMap[date] = calories.toString()
                    }
                }
                
                pastDays.clear()
                pastDays.addAll(daysMap.keys.sorted())
                pastDaysRecyclerView.adapter?.notifyDataSetChanged()
                
                if (pastDays.isEmpty()) {
                    Toast.makeText(context, "No past food records found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load past days: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun showEditFoodDialog(foodItem: FoodItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_food, null)
        
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val proteinEditText = dialogView.findViewById<EditText>(R.id.proteinEditText)
        val carbsEditText = dialogView.findViewById<EditText>(R.id.carbsEditText)
        val fatEditText = dialogView.findViewById<EditText>(R.id.fatEditText)
        
        // Set current values
        nameEditText.setText(foodItem.name)
        quantityEditText.setText(foodItem.quantity.toString())
        caloriesEditText.setText(foodItem.calories.toString())
        proteinEditText.setText(foodItem.protein.toString())
        carbsEditText.setText(foodItem.carbs.toString())
        fatEditText.setText(foodItem.fat.toString())
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Food")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val quantity = quantityEditText.text.toString().toDoubleOrNull()
                val calories = caloriesEditText.text.toString().toDoubleOrNull()
                val protein = proteinEditText.text.toString().toDoubleOrNull()
                val carbs = carbsEditText.text.toString().toDoubleOrNull()
                val fat = fatEditText.text.toString().toDoubleOrNull()
                
                if (name.isEmpty() || quantity == null || calories == null || protein == null || carbs == null || fat == null) {
                    Toast.makeText(context, "Please fill in all fields with valid numbers", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val updatedFood = FoodItem(
                    id = foodItem.id,
                    name = name,
                    quantity = quantity,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                )
                updateFood(updatedFood)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFood(foodItem: FoodItem) {
        val userId = auth.currentUser?.uid ?: return
        val today = dateFormat.format(Date())
        
        val foodData = hashMapOf(
            "userId" to userId,
            "date" to today,
            "name" to foodItem.name,
            "quantity" to foodItem.quantity,
            "calories" to foodItem.calories,
            "protein" to foodItem.protein,
            "carbs" to foodItem.carbs,
            "fat" to foodItem.fat,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        // Use set instead of update to replace the entire document
        db.collection("nutrition")
            .document(foodItem.id)
            .set(foodData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // Update the item in the list and refresh UI
                val index = todayFoods.indexOfFirst { it.id == foodItem.id }
                if (index != -1) {
                    todayFoods[index] = foodItem
                    todayFoodsRecyclerView.adapter?.notifyItemChanged(index)
                }
                Toast.makeText(context, "${foodItem.name} updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update food: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun deleteFood(foodItem: FoodItem) {
        val userId = auth.currentUser?.uid ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Food")
            .setMessage("Are you sure you want to delete ${foodItem.name}?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the document directly
                db.collection("nutrition")
                    .document(foodItem.id)
                    .delete()
                    .addOnSuccessListener {
                        // Remove from the list and update UI
                        val position = todayFoods.indexOf(foodItem)
                        if (position != -1) {
                            todayFoods.removeAt(position)
                            todayFoodsRecyclerView.adapter?.notifyItemRemoved(position)
                        }
                        Toast.makeText(context, "${foodItem.name} deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to delete food: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDayDetails(pastDay: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_day_details, null)
        val foodsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.foodsRecyclerView)
        
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("nutrition")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", pastDay)
            .get()
            .addOnSuccessListener { documents ->
                val foods = mutableListOf<FoodItem>()
                for (document in documents) {
                    val foodItem = FoodItem(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        quantity = document.getDouble("quantity") ?: 0.0,
                        calories = document.getDouble("calories") ?: 0.0,
                        protein = document.getDouble("protein") ?: 0.0,
                        carbs = document.getDouble("carbs") ?: 0.0,
                        fat = document.getDouble("fat") ?: 0.0
                    )
                    foods.add(foodItem)
                }
                
                foodsRecyclerView.layoutManager = LinearLayoutManager(context)
                foodsRecyclerView.adapter = FoodAdapter(foods, { foodItem -> deleteFood(foodItem) })
                
                AlertDialog.Builder(requireContext())
                    .setTitle("Foods for $pastDay")
                    .setView(dialogView)
                    .setPositiveButton("Close", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load day details", Toast.LENGTH_SHORT).show()
            }
    }
}

class FoodAdapter(
    private val foods: MutableList<FoodItem>,
    private val onDeleteClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {
    
    class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val quantityTextView: TextView = view.findViewById(R.id.quantityTextView)
        val caloriesTextView: TextView = view.findViewById(R.id.caloriesTextView)
        val macrosTextView: TextView = view.findViewById(R.id.macrosTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        holder.nameTextView.text = food.name
        holder.quantityTextView.text = "Quantity: ${food.quantity}g"
        holder.caloriesTextView.text = "Calories: ${food.calories}"
        holder.macrosTextView.text = "P: ${food.protein}g C: ${food.carbs}g F: ${food.fat}g"
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(food)
        }
    }
    
    override fun getItemCount() = foods.size
}

class PastDaysAdapter(
    private val pastDays: List<String>,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<PastDaysAdapter.PastDayViewHolder>() {
    
    class PastDayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val caloriesTextView: TextView = view.findViewById(R.id.caloriesTextView)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_past_day, parent, false)
        return PastDayViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PastDayViewHolder, position: Int) {
        val pastDay = pastDays[position]
        holder.dateTextView.text = pastDay
        holder.itemView.setOnClickListener { onDayClick(pastDay) }
    }
    
    override fun getItemCount() = pastDays.size
} 