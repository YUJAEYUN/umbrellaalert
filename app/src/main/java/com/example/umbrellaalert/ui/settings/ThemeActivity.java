package com.example.umbrellaalert.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.umbrellaalert.databinding.ActivityThemeBinding;

public class ThemeActivity extends AppCompatActivity {

    private ActivityThemeBinding binding;
    private SharedPreferences preferences;
    
    private static final String KEY_THEME_MODE = "theme_mode";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        
        setupUI();
        loadCurrentTheme();
    }

    private void setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());
        
        // 라디오 그룹 리스너
        binding.themeRadioGroup.setOnCheckedChangeListener(this::onThemeChanged);
    }
    
    private void loadCurrentTheme() {
        int currentTheme = preferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);
        
        switch (currentTheme) {
            case THEME_LIGHT:
                binding.radioLight.setChecked(true);
                break;
            case THEME_DARK:
                binding.radioDark.setChecked(true);
                break;
            case THEME_SYSTEM:
            default:
                binding.radioSystem.setChecked(true);
                break;
        }
    }
    
    private void onThemeChanged(RadioGroup group, int checkedId) {
        int themeMode;
        int nightMode;
        
        if (checkedId == binding.radioLight.getId()) {
            themeMode = THEME_LIGHT;
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (checkedId == binding.radioDark.getId()) {
            themeMode = THEME_DARK;
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            themeMode = THEME_SYSTEM;
            nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        
        // 설정 저장
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        
        // 테마 적용
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
    
    /**
     * 앱 시작 시 저장된 테마 적용
     */
    public static void applyTheme(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        int themeMode = preferences.getInt(KEY_THEME_MODE, THEME_SYSTEM);

        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
