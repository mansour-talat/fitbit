package com.su.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.su.gym.databinding.FragmentBmiCalculatorBinding
import java.text.DecimalFormat

class BMICalculatorFragment : Fragment() {

    private var _binding: FragmentBmiCalculatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBmiCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateButton.setOnClickListener {
            calculateBMI()
        }
    }

    private fun calculateBMI() {
        val weightStr = binding.weightInput.text.toString()
        val heightStr = binding.heightInput.text.toString()

        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both weight and height", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val weight = weightStr.toDouble()
            val height = heightStr.toDouble() / 100 // Convert cm to m

            if (weight <= 0 || height <= 0) {
                Toast.makeText(requireContext(), "Please enter valid values", Toast.LENGTH_SHORT).show()
                return
            }

            val bmi = weight / (height * height)
            val decimalFormat = DecimalFormat("#.##")
            val formattedBMI = decimalFormat.format(bmi)

            binding.resultText.text = "Your BMI: $formattedBMI"
            binding.resultText.visibility = View.VISIBLE

            val (category, range) = when {
                bmi < 18.5 -> Pair("Underweight", "< 18.5")
                bmi < 25 -> Pair("Normal weight", "18.5 - 24.9")
                bmi < 30 -> Pair("Overweight", "25 - 29.9")
                else -> Pair("Obese", "≥ 30")
            }

            binding.categoryText.text = "Category: $category"
            binding.categoryText.visibility = View.VISIBLE

            binding.rangeText.text = "Range: $range"
            binding.rangeText.visibility = View.VISIBLE

            // Make the result card visible
            binding.resultCard.visibility = View.VISIBLE

        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 