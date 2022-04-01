package com.bignerdranch.android.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.*

class DatePickerFragment : DialogFragment() {

    private val args: DatePickerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val date = Date(args.crimeDate)
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
                putSerializable(CrimeFragment.BUNDLE_DATE, resultDate)
            }
            setFragmentResult(CrimeFragment.REQUEST_DATE, bundle)
        }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay
        )
    }
}