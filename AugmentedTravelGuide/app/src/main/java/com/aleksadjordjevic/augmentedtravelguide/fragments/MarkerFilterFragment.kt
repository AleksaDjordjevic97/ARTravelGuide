package com.aleksadjordjevic.augmentedtravelguide.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.fragment.app.DialogFragment
import com.aleksadjordjevic.augmentedtravelguide.R
import com.aleksadjordjevic.augmentedtravelguide.databinding.FragmentFilterMarkersBinding

class MarkerFilterFragment(private var showHistoric:Boolean,
                           private var showEducation:Boolean,
                           private var showCatering:Boolean,
                           private var showEntertainment:Boolean,
                           private var showSports:Boolean,
                           private var distance: Int?,
                           private var filterListener: OnFilterChangeListener) : DialogFragment()
{
    private var _binding: FragmentFilterMarkersBinding? = null
    private val binding get() = _binding!!

//    private var showHistoricOrig = true
//    private var showEducationOrig = true
//    private var showCateringOrig = true
//    private var showEntertainmentOrig = true
//    private var showSportsOrig = true

    interface OnFilterChangeListener
    {
        fun onApplyFilter(showHistoric:Boolean,
                          showEducation:Boolean,
                          showCatering:Boolean,
                          showEntertainment:Boolean,
                          showSports:Boolean,
                          distance:Int?)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        _binding = FragmentFilterMarkersBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart()
    {
        super.onStart()
        val dialog: Dialog? = dialog

        if (dialog != null)
        {
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels*0.85).toInt()
            val height = (displayMetrics.heightPixels*0.85).toInt()
            dialog.window?.setLayout(width,height)
            dialog.window?.setBackgroundDrawableResource(R.drawable.white_bg)
            dialog.window?.setDimAmount(0.5f)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        setupOnClickListeners()
        setupSwitches()
        setupDistance()
    }

    private fun setupDistance()
    {
        if(distance!=null)
            binding.txtFilterDistance.setText(distance.toString())
    }

    private fun setupSwitches()
    {
        binding.switchHistoric.isChecked = showHistoric
        binding.switchEducation.isChecked = showEducation
        binding.switchCatering.isChecked = showCatering
        binding.switchEntertainment.isChecked = showEntertainment
        binding.switchSports.isChecked = showSports

        binding.switchHistoric.setOnCheckedChangeListener { compoundButton, b ->
            showHistoric = b
        }
        binding.switchEducation.setOnCheckedChangeListener { compoundButton, b ->
            showEducation = b
        }
        binding.switchCatering.setOnCheckedChangeListener { compoundButton, b ->
            showCatering = b
        }
        binding.switchEntertainment.setOnCheckedChangeListener { compoundButton, b ->
            showEntertainment = b
        }
        binding.switchSports.setOnCheckedChangeListener { compoundButton, b ->
            showSports = b
        }

    }


    private fun setupOnClickListeners()
    {
        binding.btnFilter.setOnClickListener { applyFilters() }
        binding.btnCloseFilter.setOnClickListener { dismiss() }
    }

    private fun applyFilters()
    {
        val distance = if(binding.txtFilterDistance.text.isEmpty())
            null
        else
            binding.txtFilterDistance.text.toString().toInt()

        filterListener.onApplyFilter(showHistoric,showEducation,showCatering,showEntertainment,showSports,distance)
        dismiss()
    }


}