package com.bignerdranch.android.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import java.util.*

private const val ARG_DATE = "date"
private const val REQUEST_DATE = "RequestDate"
private const val BUNDLE_DATE = "BundleDate"

class DatePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialYear = calendar[Calendar.YEAR]
        val initialMonth = calendar[Calendar.MONTH]
        val initialDay = calendar[Calendar.DAY_OF_MONTH]
        val initialHourOfDay = calendar[Calendar.HOUR_OF_DAY]
        val initialMinute = calendar[Calendar.MINUTE]

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val resultDate: Date =
                GregorianCalendar(year, month, day, initialHourOfDay, initialMinute).time
            val bundle = Bundle().apply {
                putSerializable(BUNDLE_DATE, resultDate)
            }
            setFragmentResult(REQUEST_DATE, bundle)
        }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay
        )
    }

    companion object {
        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }
}