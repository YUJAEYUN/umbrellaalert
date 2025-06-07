package com.example.umbrellaalert.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.example.umbrellaalert.widget.WeatherWidgetProvider;
import com.example.umbrellaalert.service.PersistentNotificationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

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

        // 위젯 업데이트 (앱 시작 시)
        WeatherWidgetProvider.forceUpdateAllWidgets(this);

        // 알림 권한 요청 (Android 13+)
        requestNotificationPermission();

        // 알림 서비스 시작 (설정이 활성화되어 있다면)
        if (PersistentNotificationService.isEnabled(this)) {
            PersistentNotificationService.setEnabled(this, true);
        }
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
     * 알림 권한 요청 (Android 13+)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 알림 권한이 허용됨
                // 필요한 경우 알림 서비스 재시작
                if (PersistentNotificationService.isEnabled(this)) {
                    PersistentNotificationService.setEnabled(this, true);
                }
            } else {
                // 알림 권한이 거부됨
                // 사용자에게 알림 기능이 제한됨을 알릴 수 있음
            }
        }
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
