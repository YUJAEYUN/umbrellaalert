package com.example.umbrellaalert.ui.location;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.data.database.DatabaseHelper;
import com.example.umbrellaalert.data.database.LocationDao;
import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.databinding.ActivityLocationBinding;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationActivity extends AppCompatActivity implements LocationAdapter.LocationListener {

    private ActivityLocationBinding binding;
    private LocationDao locationDao;
    private LocationAdapter adapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // DAO 및 서비스 초기화
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        locationDao = new LocationDao(dbHelper);
        executorService = Executors.newSingleThreadExecutor();

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 위치 추가 버튼
        binding.fabAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddLocationDialog();
            }
        });

        // RecyclerView 설정
        binding.recyclerLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(this);
        binding.recyclerLocations.setAdapter(adapter);

        // 위치 데이터 로드
        loadLocations();
    }

    private void loadLocations() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final List<Location> locations = locationDao.getAllLocations();

                // UI 스레드에서 어댑터 업데이트
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setLocations(locations);
                        binding.emptyView.setVisibility(locations.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
    }

    private void showAddLocationDialog() {
        AddLocationDialog dialog = new AddLocationDialog();
        dialog.setLocationAddedListener(new AddLocationDialog.LocationAddedListener() {
            @Override
            public void onLocationAdded(final Location location) {
                // 새 위치 저장
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        long id = locationDao.insertLocation(location);
                        if (id > 0) {
                            // 위치 목록 다시 로드
                            loadLocations();
                        }
                    }
                });
            }
        });
        dialog.show(getSupportFragmentManager(), "AddLocationDialog");
    }

    @Override
    public void onLocationDelete(final Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int result = locationDao.deleteLocation(location.getId());
                if (result > 0) {
                    loadLocations();
                }
            }
        });
    }

    @Override
    public void onLocationToggleNotification(final Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // 알림 설정 토글
                location.setNotificationEnabled(!location.isNotificationEnabled());
                int result = locationDao.updateLocation(location);

                if (result > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = location.isNotificationEnabled() ?
                                    "알림이 활성화되었습니다" : "알림이 비활성화되었습니다";
                            Toast.makeText(LocationActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}