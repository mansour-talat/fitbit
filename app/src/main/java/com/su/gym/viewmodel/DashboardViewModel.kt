package com.su.gym.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import android.util.Log
import com.su.gym.models.ActivityStats

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _activityStats = MutableLiveData<ActivityStats>()
    val activityStats: LiveData<ActivityStats> = _activityStats
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun refreshActivityStats() {
        _isLoading.value = true
        _error.value = null
        
        Log.d("DashboardViewModel", "Refreshing activity stats...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // For now, just set default values
                _activityStats.postValue(ActivityStats(0, 0, 0f, 0))
                _isLoading.postValue(false)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error refreshing activity stats", e)
                _error.postValue("Error refreshing activity stats: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }
} 