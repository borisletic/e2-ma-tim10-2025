package com.example.ma2025.ui.tasks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ma2025.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TasksOverviewFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TasksPagerAdapter pagerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks_overview, container, false);

        initViews(view);
        setupViewPager();

        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        pagerAdapter = new TasksPagerAdapter(getActivity());
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("📅 Kalendar");
                    break;
                case 1:
                    tab.setText("📋 Lista");
                    break;
            }
        }).attach();
    }

    private static class TasksPagerAdapter extends FragmentStateAdapter {

        public TasksPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new CalendarTasksFragment();
                case 1:
                    return new TasksListFragment();
                default:
                    return new CalendarTasksFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}