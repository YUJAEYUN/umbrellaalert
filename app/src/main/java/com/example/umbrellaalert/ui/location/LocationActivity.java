package com.example.umbrellaalert.ui.location;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.umbrellaalert.data.model.Location;
import com.example.umbrellaalert.databinding.ActivityLocationBinding;

public class LocationActivity extends AppCompatActivity implements LocationAdapter.LocationListener {

    private ActivityLocationBinding binding;
    private LocationViewModel viewModel;
    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel 초기화
        viewModel = new ViewModelProvider(this).get(LocationViewModel.class);

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener(v -> finish());

        // 위치 추가 버튼
        binding.fabAddLocation.setOnClickListener(v -> showAddLocationDialog());

        // RecyclerView 설정
        binding.recyclerLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(this);
        binding.recyclerLocations.setAdapter(adapter);

        // LiveData 관찰
        observeViewModel();
    }

    private void observeViewModel() {
        // 위치 목록 관찰
        viewModel.getLocations().observe(this, locations -> {
            adapter.setLocations(locations);
            binding.emptyView.setVisibility(locations.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // 토스트 메시지 관찰
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddLocationDialog() {
        AddLocationDialog dialog = new AddLocationDialog();
        dialog.setLocationAddedListener(location -> {
            // 새 위치 저장
            viewModel.addLocation(location);
        });
        dialog.show(getSupportFragmentManager(), "AddLocationDialog");
    }

    @Override
    public void onLocationDelete(Location location) {
        viewModel.deleteLocation(location);
    }

    @Override
    public void onLocationToggleNotification(Location location) {
        viewModel.toggleNotification(location);
    }
}