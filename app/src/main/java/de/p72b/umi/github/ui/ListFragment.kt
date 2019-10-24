package de.p72b.umi.github.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import de.p72b.umi.github.App
import de.p72b.umi.github.arch.RepositoryViewModel
import de.p72b.umi.github.services.Repository
import de.p72b.umi.github.utils.Utils
import de.p72b.umi.github.vo.Resource
import de.p72b.umi.github.vo.Status
import kotlinx.android.synthetic.main.fragment_list.*
import android.view.MenuInflater
import androidx.preference.PreferenceManager
import de.p72b.umi.github.R

class ListFragment : Fragment() {
    private lateinit var rootView: View

    private lateinit var repositoriesViewModel: RepositoryViewModel
    private lateinit var adapter: ListAdapter

    private val preferenceManager = PreferenceManager.getDefaultSharedPreferences(App.sInstance)

    companion object {
        val TAG: String = ListFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_list, null)
        setHasOptionsMenu(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()

        repositoriesViewModel = ViewModelProviders.of(this).get(RepositoryViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        getRepositories()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.vSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_query_hint)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                preferenceManager.edit().putString(getString(R.string.pref_key_search), query).apply()

                getRepositories()
                return false
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun initViews() {
        vSwipeRefresh.setOnRefreshListener { getRepositories() }
        adapter =
            ListAdapter(App.sInstance,
                object : ListAdapter.AdapterListener {
                    override fun onRepositoryClicked(repository: Repository) {
                        (activity as RepositoryActivity).showDetails(repository)
                    }
                })
        vRecyclerView.adapter = adapter
        vRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun getRepositories() {
        vSwipeRefresh.isRefreshing = true
        val query = preferenceManager.getString(getString(R.string.pref_key_search), "kotlin")?: "kotlin"
        repositoriesViewModel.searchRepositories(query).observe(this,
            Observer<Resource<List<Repository>>> { resource ->
                vSwipeRefresh.isRefreshing = false
                when (resource.status) {
                    Status.SUCCESS -> resource.data?.let {
                        adapter.setData(it)
                    }
                    Status.ERROR -> resource.message?.let {
                        Utils.showSnackbar(rootView, it)
                    }
                }

            })
    }
}