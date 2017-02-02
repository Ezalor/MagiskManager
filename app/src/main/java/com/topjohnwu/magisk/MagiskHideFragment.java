package com.topjohnwu.magisk;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.topjohnwu.magisk.adapters.ApplicationAdapter;
import com.topjohnwu.magisk.utils.Async;
import com.topjohnwu.magisk.utils.CallbackHandler;
import com.topjohnwu.magisk.utils.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MagiskHideFragment extends Fragment implements CallbackHandler.EventListener {

    private Unbinder unbinder;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recyclerView) RecyclerView recyclerView;

    private ApplicationAdapter appAdapter;

    private SearchView.OnQueryTextListener searchListener;
    private String lastFilter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_magisk_hide, container, false);
        unbinder = ButterKnife.bind(this, view);

        PackageManager packageManager = getActivity().getPackageManager();

        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(() -> new Async.LoadApps(packageManager).exec());

        appAdapter = new ApplicationAdapter(packageManager);
        recyclerView.setAdapter(appAdapter);

        searchListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                lastFilter = query;
                appAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastFilter = newText;
                appAdapter.filter(newText);
                return false;
            }
        };

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_magiskhide, menu);
        SearchView search = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.app_search));
        search.setOnQueryTextListener(searchListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.magiskhide);
        CallbackHandler.register(Global.Events.packageLoadDone, this);
        if (Global.Events.packageLoadDone.isTriggered) {
            onTrigger(Global.Events.packageLoadDone);
        }
    }

    @Override
    public void onStop() {
        CallbackHandler.unRegister(Global.Events.packageLoadDone, this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onTrigger(CallbackHandler.Event event) {
        Logger.dev("MagiskHideFragment: UI refresh");
        appAdapter.setLists(Global.Data.appList, Global.Data.magiskHideList);
        mSwipeRefreshLayout.setRefreshing(false);
        if (!TextUtils.isEmpty(lastFilter)) {
            appAdapter.filter(lastFilter);
        }
    }
}
