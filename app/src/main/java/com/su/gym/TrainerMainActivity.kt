package com.su.gym

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.ActivityTrainerMainBinding
import com.su.gym.databinding.NavHeaderTrainerMainBinding

class TrainerMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTrainerMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    lateinit var navController: NavController
    private lateinit var navHeaderBinding: NavHeaderTrainerMainBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding = ActivityTrainerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Get the navigation header views
        val headerView = binding.navView.getHeaderView(0)
        navHeaderBinding = NavHeaderTrainerMainBinding.bind(headerView)

        // Initialize the appBarConfiguration for trainer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_trainer_chat_list
            ),
            binding.drawerLayout
        )

        // Setup toolbar and navigation
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.setNavigationItemSelectedListener(this)
        
        // Add a listener to handle navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_login -> {
                    setDrawerVisibility(false)
                }
                else -> {
                    setDrawerVisibility(true)
                    updateNavHeader()
                }
            }
        }

        // Update header initially
        updateNavHeader()
    }

    private fun updateNavHeader() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Update email
            navHeaderBinding.textViewEmail.text = user.email

            // Get trainer data from Firestore
            db.collection("trainer").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Trainer"
                        navHeaderBinding.textViewEmail.text = "$name\n${user.email}"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading trainer data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_trainer_chat_list -> {
                navController.navigate(R.id.nav_trainer_chat_list)
            }
            R.id.nav_trainer_logout -> {
                handleLogout()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleLogout() {
        binding.progressBar.visibility = View.VISIBLE
        try {
            // Sign out from Firebase
            auth.signOut()
            
            // Close the drawer
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            
            // Create a new intent to restart the activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setDrawerVisibility(visible: Boolean) {
        binding.drawerLayout.setDrawerLockMode(if (visible) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }
} 