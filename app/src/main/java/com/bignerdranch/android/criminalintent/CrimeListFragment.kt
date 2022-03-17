package com.bignerdranch.android.criminalintent

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeListFragment"
private const val DATE_PATTERN = "EEEE, dd MMMM yyyy HH:mm"

class CrimeListFragment : Fragment(), ActionMode.Callback {

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private lateinit var tracker: SelectionTracker<String>
    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = CrimeAdapter()
    private var callbacks: Callbacks? = null
    private var actionMode: ActionMode? = null


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

        tracker = SelectionTracker.Builder(
            "my_tracker",
            crimeRecyclerView,
            ItemsKeyProvider(adapter!!),
            ItemsDetailsLookup(crimeRecyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        tracker.addObserver(
            object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()

                    if (actionMode == null) {
                        val currentActivity = activity as MainActivity
                        actionMode = currentActivity.startSupportActionMode(this@CrimeListFragment)
                    }

                    val items = tracker.selection.size()
                    if (items > 0) {
                        actionMode?.title = getString(R.string.action_selected, items)
                    } else {
                        actionMode?.finish()
                    }
                }
            })

        adapter?.tracker = tracker

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
        inflater.inflate(R.menu.fragment_crime_list_add, menu)
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
        adapter?.submitList(crimes)
    }

    class ItemsDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<String>() {

        override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
            val view = recyclerView.findChildViewUnder(event.x, event.y)
            if (view != null) {
                return (recyclerView.getChildViewHolder(view) as CrimeAdapter.CrimeHolder).getItem()
            }
            return null
        }
    }

    private class ItemsKeyProvider(private val adapter: CrimeAdapter) : ItemKeyProvider<String>(SCOPE_CACHED) {

        override fun getKey(position: Int): String =
            adapter.currentList[position].id.toString()

        override fun getPosition(key: String): Int =
            adapter.currentList.indexOfFirst { it.id.toString() == key }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime) =
            oldItem == newItem
    }

    private inner class CrimeAdapter : ListAdapter<Crime, CrimeAdapter.CrimeHolder>(DiffCallback()) {

        var tracker: SelectionTracker<String>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = currentList[position]
            holder.bind(crime)
        }

        override fun getItemCount(): Int = currentList.size

        inner class CrimeHolder(view: View)
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

                tracker?.let {
                    if (it.isSelected(crime.id.toString())) {
                        itemView.setBackgroundColor(Color.LTGRAY)
                    } else {
                        itemView.background = null
                    }
                }
            }

            fun getItem(): ItemDetailsLookup.ItemDetails<String> =
                object : ItemDetailsLookup.ItemDetails<String>() {
                    override fun getPosition(): Int = bindingAdapterPosition
                    override fun getSelectionKey(): String = getItem(bindingAdapterPosition).id.toString()
                }

            override fun onClick(item: View?) {
                callbacks?.onCrimeSelected(crime.id)
            }

            override fun onLongClick(item: View?): Boolean {

                return true
            }
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.fragment_crime_list_delete, menu)
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean = true

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_delete_crime -> {
                var selected = adapter?.currentList?.filter {
                    tracker.selection.contains(it.id.toString())
                }

                val crimes = adapter?.currentList?.toMutableList()
                if (crimes?.get(0) == selected?.get(0)) {
                    selected = selected?.subList(1, selected.size)
                }

                selected?.let {
                    crimes?.removeAll(selected)
                }

                crimes?.let {
                    updateUI(crimes)
                }
                actionMode?.finish()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onDestroyActionMode(p0: ActionMode?) {
        tracker.clearSelection()
        actionMode = null
    }
}