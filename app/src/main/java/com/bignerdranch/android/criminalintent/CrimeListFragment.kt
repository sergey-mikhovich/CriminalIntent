package com.bignerdranch.android.criminalintent

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"
private const val DATE_PATTERN = "EEEE, dd MMMM yyyy HH:mm"

class CrimeListFragment : Fragment() {

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var menuNewCrime: MenuItem
    private lateinit var menuDeleteCrime: MenuItem
    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())
    private var callbacks: Callbacks? = null

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this)[CrimeListViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)

        crimeRecyclerView =
            view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager =
            LinearLayoutManager(context).apply {
                reverseLayout = true
                stackFromEnd = true
            }

        crimeRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner
        ) { crimes ->
            crimes?.let {
                Log.i(TAG, "Got crimes ${crimes.size}")
                updateUI(crimes)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)

        menuNewCrime = menu.findItem(R.id.menu_new_crime)
        menuDeleteCrime = menu.findItem(R.id.menu_delete_crime)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(crimes: List<Crime>) {
        adapter?.let {
            it.crimes = crimes
        } ?: run {
            adapter = CrimeAdapter(crimes)
        }
        crimeRecyclerView.adapter = adapter
    }

    private inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title_text_view)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date_text_view)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved_image_view)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text =
                SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(crime.date)
            solvedImageView.apply {
                if (crime.isSolved) {
                    visibility = View.VISIBLE
                    contentDescription = getString(R.string.crime_handcuffs_solved_description)
                } else {
                    visibility = View.GONE
                    contentDescription = getString(R.string.crime_handcuffs_no_solved_description)
                }
            }
        }

        override fun onClick(item: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }

        override fun onLongClick(item: View?): Boolean {
            return true
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>)
        : RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        override fun getItemCount(): Int = crimes.size
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}