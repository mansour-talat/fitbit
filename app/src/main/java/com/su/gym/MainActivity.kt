package com.su.gym

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.su.gym.databinding.ActivityMainBinding
import com.su.gym.databinding.NavHeaderMainBinding
import com.su.gym.databinding.NavHeaderTrainerMainBinding
import androidx.navigation.NavOptions
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import kotlinx.coroutines.launch
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.ViewGroup
import com.su.gym.WelcomeFragment
import android.content.res.Configuration

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController
    private var navHeaderBinding: NavHeaderMainBinding? = null
    private var navHeaderTrainerBinding: NavHeaderTrainerMainBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var hasNavigatedToLogin = false
    private var isTrainer = false
    private var isCheckingAuth = false
    private lateinit var drawerToggle: ActionBarDrawerToggle

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up bottom navigation
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.mainScreenFragment, false) // Use your graph's root
                .build()
            try {
                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (e: Exception) {
                false
            }
        }

        // Set up the app bar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainScreenFragment,
                R.id.workoutDietFragment,
                R.id.trainerListFragment,
                R.id.trainerChatFragment,
                R.id.nav_dashboard,
                R.id.navigation_food_history,
                R.id.activityHistoryFragment,
                R.id.feedbackFragment,
                R.id.updateProfileFragment,
                R.id.nav_report_conflict,
                R.id.nav_home,
                R.id.nav_profile,
                R.id.nav_chat,
                R.id.plansContainerFragment,
                R.id.welcomeFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.nav_trainer_registration,
                R.id.nav_bmi_calculator,
                R.id.nav_trainer_chat_list,
                R.id.nutritionPlansFragment,
                R.id.nutritionPlanDetailsFragment,
                R.id.exerciseDetailsFragment,
                R.id.workoutPlansFragment,
                R.id.plansContainerFragment,
                R.id.navigation_chatbot,
                R.id.chatFragment
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        setupDrawerContent()
        
        // Check auth state
        checkAuthState()
        
        // Check user type
        checkUserType()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
        // Re-sync drawer state after configuration change
        binding.root.post {
            drawerToggle.syncState()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure we check auth state when app returns to foreground
        checkAuthState()
    }

    private fun setupDrawerContent() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // Configure the app bar
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_profile,
                R.id.nav_chat,
                R.id.nav_report_conflict,
                R.id.plansContainerFragment
            ),
            drawerLayout
        )

        // Set up the ActionBar with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up the NavigationView with NavController
        navView.setupWithNavController(navController)

        // Setup drawer toggle
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Initialize header views
        initializeHeaderViews()
    }

    private fun initializeHeaderViews() {
        try {
            val headerView = binding.navView.getHeaderView(0)
            navHeaderBinding = NavHeaderMainBinding.bind(headerView)
            
            val trainerHeaderView = binding.navView.getHeaderView(0)
            navHeaderTrainerBinding = NavHeaderTrainerMainBinding.bind(trainerHeaderView)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing header views", e)
        }
    }

    private fun checkAuthState() {
        // Prevent multiple simultaneous auth checks
        if (isCheckingAuth) return
        isCheckingAuth = true

        try {
            val currentDestination = navController.currentDestination?.id
            val isAuthScreen = currentDestination == R.id.welcomeFragment ||
                             currentDestination == R.id.loginFragment ||
                             currentDestination == R.id.registerFragment ||
                             currentDestination == R.id.nav_trainer_registration

            if (auth.currentUser == null) {
                // User is not logged in, hide all navigation UI
                binding.navView.visibility = View.GONE
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.toolbar.visibility = View.GONE
                binding.bottomNavView.visibility = View.GONE
                supportActionBar?.hide()
                
                if (!isAuthScreen) {
                    // Only navigate to welcome if we're not already on an auth screen
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                    navController.navigate(R.id.welcomeFragment, null, navOptions)
                }
            } else if (currentDestination == R.id.welcomeFragment) {
                // User is logged in but we're on welcome screen, navigate to main screen
                Log.d(TAG, "User is logged in, navigating to main screen")
                binding.toolbar.visibility = View.VISIBLE
                binding.bottomNavView.visibility = View.VISIBLE
                supportActionBar?.show()
                replaceFragment(MainScreenFragment())
                
                // Check user type and update drawer
                updateDrawerVisibility()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAuthState", e)
            // Hide navigation UI in case of error
            binding.navView.visibility = View.GONE
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.toolbar.visibility = View.GONE
            supportActionBar?.hide()
        } finally {
            isCheckingAuth = false
        }
    }

    private fun checkUserAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null && !hasNavigatedToLogin) {
            hasNavigatedToLogin = true
            Log.d(TAG, "No user logged in, navigating to login")
            // Hide bottom navigation when not logged in
            binding.bottomNavView.visibility = View.INVISIBLE
            binding.root.post {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                navController.navigate(R.id.loginFragment, null, navOptions)
            }
        } else if (currentUser != null) {
            // Reset drawer state before checking user type
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.navView.visibility = View.GONE
            
            // Show bottom navigation for logged-in users
            binding.bottomNavView.visibility = View.VISIBLE
            
            // Check user type and update drawer
            checkUserType()
        }
    }

    private fun checkUserType() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // No user logged in, navigate to welcome screen
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            navController.navigate(R.id.welcomeFragment, null, navOptions)
            return
        }

        // Check if user is a trainer
        db.collection("trainer")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User is a trainer, launch TrainerMainActivity
                    val intent = Intent(this, TrainerMainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // User is not a trainer, navigate to main screen
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                    navController.navigate(R.id.mainScreenFragment, null, navOptions)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user type", e)
                // Navigate to main screen in case of error
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                navController.navigate(R.id.mainScreenFragment, null, navOptions)
            }
    }

    fun handlePostLogin() {
        try {
            // Show toolbar first
            binding.toolbar.visibility = View.VISIBLE
            supportActionBar?.show()

            // Clear and reinitialize the drawer
            binding.navView.menu.clear()
            binding.navView.inflateMenu(R.menu.drawer_menu)
            
            // Setup drawer toggle
            drawerToggle = ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            binding.drawerLayout.addDrawerListener(drawerToggle)
            drawerToggle.syncState()

            // Enable drawer indicator
            drawerToggle.isDrawerIndicatorEnabled = true
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeButtonEnabled(true)

            // Show navigation views
            binding.bottomNavView.visibility = View.VISIBLE
            binding.navView.visibility = View.VISIBLE
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            // Setup navigation
            binding.navView.setupWithNavController(navController)
            binding.navView.setNavigationItemSelectedListener(this)

            // Update drawer header and check user type
            updateDrawerHeader()
            updateDrawerVisibility()

            // Force layout update
            binding.root.requestLayout()
            binding.drawerLayout.requestLayout()
            binding.navView.requestLayout()

            // Post delayed action to ensure visibility
            binding.root.postDelayed({
                binding.navView.visibility = View.VISIBLE
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                drawerToggle.syncState()
                // Open drawer briefly to ensure it's initialized
                binding.drawerLayout.openDrawer(GravityCompat.START)
                binding.drawerLayout.postDelayed({
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }, 100)
            }, 100)

        } catch (e: Exception) {
            Log.e(TAG, "Error in handlePostLogin", e)
        }
    }

    fun updateDrawerVisibility() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.navView.visibility = View.GONE
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.toolbar.visibility = View.GONE
            supportActionBar?.hide()
            return
        }

        // User is logged in, check if they are a trainer
        db.collection("trainer")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                try {
                    if (document.exists()) {
                        // User is a trainer, show trainer menu
                        binding.navView.menu.clear()
                        binding.navView.inflateMenu(R.menu.trainer_drawer_menu)
                    } else {
                        // User is a regular user, show user menu
                        binding.navView.menu.clear()
                        binding.navView.inflateMenu(R.menu.drawer_menu)
                    }

                    // Common setup for both user types
                    binding.toolbar.visibility = View.VISIBLE
                    supportActionBar?.show()
                    binding.navView.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    
                    // Update header and setup navigation
                    updateDrawerHeader()
                    binding.navView.setupWithNavController(navController)
                    binding.navView.setNavigationItemSelectedListener(this)
                    
                    // Force layout updates
                    binding.root.requestLayout()
                    binding.drawerLayout.requestLayout()
                    binding.navView.requestLayout()
                    
                    // Ensure drawer toggle is properly set up
                    drawerToggle.isDrawerIndicatorEnabled = true
                    drawerToggle.syncState()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up drawer", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking trainer status", e)
                // Show regular user menu in case of error
                try {
                    binding.navView.menu.clear()
                    binding.navView.inflateMenu(R.menu.drawer_menu)
                    binding.navView.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    updateDrawerHeader()
                    binding.navView.setupWithNavController(navController)
                    binding.navView.setNavigationItemSelectedListener(this)
                    drawerToggle.syncState()
                } catch (e2: Exception) {
                    Log.e(TAG, "Error in fallback drawer setup", e2)
                }
            }
    }

    fun setDrawerVisibility(visible: Boolean) {
        try {
            if (visible) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                binding.navView.visibility = View.VISIBLE
                // Force update the drawer layout
                binding.drawerLayout.invalidate()
                binding.navView.invalidate()
                // Post a delayed action to ensure drawer is visible
                binding.root.post {
                    binding.navView.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                binding.navView.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting drawer visibility", e)
        }
    }

    fun updateDrawerHeader() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            try {
                // Get user data from Firestore
                db.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val name = document.getString("name") ?: "User"
                            val email = user.email ?: ""
                            
                            if (isTrainer) {
                                navHeaderTrainerBinding?.let { binding ->
                                    binding.textViewName.text = name
                                    binding.textViewEmail.text = email
                                }
                            } else {
                                navHeaderBinding?.let { binding ->
                                    binding.textViewName.text = name
                                    binding.textViewEmail.text = email
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading user data", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating drawer header", e)
            }
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_logout -> {
                    handleLogout()
                    return true
                }
                else -> {
                    try {
                        val navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.mainScreenFragment, false) // Use your graph's root
                            .build()
                        navController.navigate(item.itemId, null, navOptions)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to menu item", e)
                        navController.navigate(R.id.mainScreenFragment)
                    }
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling navigation item selection", e)
            return false
        }
    }

    private fun handleLogout() {
        try {
            auth.signOut()
            // Hide navigation UI
            binding.navView.visibility = View.GONE
            binding.bottomNavView.visibility = View.GONE
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.toolbar.visibility = View.GONE
            supportActionBar?.hide()
            
            // Navigate to login fragment
            navController.navigate(R.id.loginFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(this, "Error during logout", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Instead of completely resetting navigation, just update drawer visibility
            updateDrawerVisibility()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToLogin() {
        try {
            // Clear back stack and navigate to login
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            navController.navigate(R.id.loginFragment, null, navOptions)
        } catch (e: Exception) {
            // If navigation fails, restart activity
            finish()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    fun navigateToFragment(destinationId: Int, args: Bundle? = null) {
        try {
            Log.d(TAG, "Navigating to fragment: ${resources.getResourceName(destinationId)}")
            navController.navigate(destinationId, args)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to fragment", e)
            Toast.makeText(this, "Error navigating to screen", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToNutritionPlanDetails(planId: String) {
        try {
            val bundle = Bundle().apply {
                putString("planId", planId)
            }
            navController.navigate(R.id.nutritionPlanDetailsFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to nutrition plan details", e)
            Toast.makeText(this, "Error opening plan details", Toast.LENGTH_SHORT).show()
        }
    }
} 