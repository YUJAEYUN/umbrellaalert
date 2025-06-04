package com.example.umbrellaalert.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.umbrellaalert.R;
import com.example.umbrellaalert.databinding.ActivityMainBinding;
import com.example.umbrellaalert.ui.fragments.HomeFragment;
import com.example.umbrellaalert.ui.fragments.WeatherFragment;
import com.example.umbrellaalert.ui.fragments.BusFragment;
import com.example.umbrellaalert.ui.fragments.SettingsFragment;
import com.example.umbrellaalert.ui.home.WeatherViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FragmentManager fragmentManager;
    private WeatherViewModel sharedWeatherViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fragmentManager = getSupportFragmentManager();

        // Activity 범위의 공유 ViewModel 초기화
        sharedWeatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        // 초기 Fragment 설정 (홈)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // 하단 네비게이션 설정
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_weather) {
                selectedFragment = new WeatherFragment();
            } else if (itemId == R.id.nav_bus) {
                selectedFragment = new BusFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Fragment에서 공유 ViewModel에 접근할 수 있도록 제공
     */
    public WeatherViewModel getSharedWeatherViewModel() {
        return sharedWeatherViewModel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
