package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_PATTERN = "EEEE, dd MMMM yyyy"
private const val TIME_PATTERN = "HH:mm"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_DATE = "RequestDate"
private const val BUNDLE_DATE = "BundleDate"

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedSwitch: SwitchCompat
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }

    private val resultLauncherPickerContracts =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when {
                it.resultCode != Activity.RESULT_OK -> return@registerForActivityResult

                it.data != null -> {
                    val contactUri: Uri? = it.data!!.data
                    val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                    val cursor = contactUri?.let { conUri ->
                        requireActivity().contentResolver
                            .query(conUri, queryFields, null, null, null)
                    }
                    cursor?.use { curs ->
                        if (curs.count == 0) {
                            return@registerForActivityResult
                        }

                        curs.moveToFirst()
                        val suspect = curs.getString(0).replace("\n", " ")
                        crime.suspect = suspect
                        crimeDetailViewModel.saveCrime(crime)
                        suspectButton.text = suspect
                    }
                }
            }
        }

    private val resultLauncherCaptureImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when {
                it.resultCode != Activity.RESULT_OK -> return@registerForActivityResult

                it.data != null -> {
                    requireActivity().revokeUriPermission(photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    updatePhotoView()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)

        setFragmentResultListener(REQUEST_DATE) { _, bundle ->
            val date = bundle.getSerializable(BUNDLE_DATE) as Date
            crime.date = date
            updateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title_edit_text) as EditText
        dateButton = view.findViewById(R.id.crime_date_button) as Button
        timeButton = view.findViewById(R.id.crime_time_button) as Button
        solvedSwitch = view.findViewById(R.id.crime_solved_switch) as SwitchCompat
        reportButton = view.findViewById(R.id.crime_report_button) as Button
        suspectButton = view.findViewById(R.id.crime_suspect_button) as Button
        photoButton = view.findViewById(R.id.crime_camera_image_Button) as ImageButton
        photoView = view.findViewById(R.id.crime_photo_image_view) as ImageView

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner
        ) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile)
                updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {

            }

            override fun onTextChanged(
                p0: CharSequence?,
                p1: Int,
                p2: Int,
                p3: Int
            ) {
                crime.title = p0.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                reportButton.isEnabled = crime.title.isNotBlank()
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.apply {
            isEnabled = crime.title.isNotBlank()
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                resultLauncherPickerContracts.launch(pickContactIntent)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                resultLauncherCaptureImage.launch(captureImage)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (crime.title.isBlank()) {
            crimeDetailViewModel.deleteCrime(crime)
        } else {
            crimeDetailViewModel.saveCrime(crime)
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text =
            SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(crime.date)
        timeButton.text =
            SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(crime.date)
        solvedSwitch.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription =
                getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription =
                getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString =
            SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(crime.date)
        val timeString =
            SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(crime.date)

        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report,
            crime.title, dateString, timeString, solvedString, suspect)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}