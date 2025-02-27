package com.gorunjinian.dbst.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorunjinian.dbst.R
import com.gorunjinian.dbst.adapters.DatabaseAdapter
import com.gorunjinian.dbst.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabasesFragment : Fragment() {

    private lateinit var database: EntryDatabase
    private lateinit var entryDao: EntryDao
    private lateinit var tableSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DatabaseAdapter
    private var availableTables: List<String> = emptyList()
    private var currentTable: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_databases, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide FAB in MainActivity
        requireActivity().findViewById<View>(R.id.fab_popup)?.visibility = View.GONE

        // Initialize database and DAO
        database = EntryDatabase.getDatabase(requireContext())
        entryDao = database.entryDao()

        // Initialize UI elements
        tableSpinner = view.findViewById(R.id.table_spinner)
        recyclerView = view.findViewById(R.id.recycler_view)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DatabaseAdapter()
        recyclerView.adapter = adapter

        // Load table names dynamically
        loadTableNames()
    }

    private fun loadTableNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            // ✅ Execute raw SQL query to get table names, excluding system tables
            val rawQuery = SimpleSQLiteQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence'")
            availableTables = entryDao.getAllTableNames(rawQuery).filter { it != "sqlite_sequence" }

            withContext(Dispatchers.Main) {
                if (availableTables.isEmpty()) {
                    Toast.makeText(requireContext(), "No tables found!", Toast.LENGTH_SHORT).show()
                } else {
                    setupTableSpinner()
                }
            }
        }
    }

    private fun setupTableSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableTables)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        tableSpinner.adapter = spinnerAdapter
        tableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTable = availableTables[position]
                loadTableData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTableData() {
        currentTable?.let { table ->
            lifecycleScope.launch(Dispatchers.IO) {
                val records = when (table) {
                    "DBT" -> entryDao.getAllIncome()
                    "DST" -> entryDao.getAllExpense()
                    else -> emptyList()
                }

                withContext(Dispatchers.Main) {
                    if (records.isEmpty()) {
                        Toast.makeText(requireContext(), "No records found in $table!", Toast.LENGTH_SHORT).show()
                    }
                    adapter.updateData(table, records)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show FAB back when leaving the fragment
        requireActivity().findViewById<View>(R.id.fab_popup)?.visibility = View.VISIBLE
    }
}
