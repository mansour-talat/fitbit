package com.su.gym.utils
 
/**
 * Extension function to format a Double to a specified number of decimal places
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this) 