package com.kodelink.glide;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class DashboardDriverActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String PREFS_NAME = "DriverPrefs";
    private static final String KEY_DRIVER_STATUS = "driver_status";
    
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private MaterialButton btnMenu;
    private SwitchMaterial switchAvailability;
    private TextView tvAvailabilityStatus;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_driver);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        btnMenu = findViewById(R.id.btnMenu);
        switchAvailability = findViewById(R.id.switchAvailability);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Initialize preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize map
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Set up navigation drawer
        navigationView.setNavigationItemSelectedListener(this);
        
        // Set up button listeners
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Set up availability toggle
        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Available" : "Offline";
            int color = isChecked ? getResources().getColor(R.color.green) : getResources().getColor(R.color.red);
            
            tvAvailabilityStatus.setText(status);
            tvAvailabilityStatus.setTextColor(color);
            
            // Save status to SharedPreferences
            prefs.edit().putBoolean(KEY_DRIVER_STATUS, isChecked).apply();
            
            Toast.makeText(this, "Status updated: " + status, Toast.LENGTH_SHORT).show();
        });

        // Load saved availability status
        boolean isAvailable = prefs.getBoolean(KEY_DRIVER_STATUS, false);
        switchAvailability.setChecked(isAvailable);
        String status = isAvailable ? "Available" : "Offline";
        int color = isAvailable ? getResources().getColor(R.color.green) : getResources().getColor(R.color.red);
        tvAvailabilityStatus.setText(status);
        tvAvailabilityStatus.setTextColor(color);

        // Update header with user role
        updateNavigationHeader();

        // Request location permission
        requestLocationPermission();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        // Enable location button
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    googleMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Add marker for current location
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Your Location"));
                            
                            // Move camera to current location
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateNavigationHeader() {
        SharedPreferences authPrefs = getSharedPreferences("MockAuth", MODE_PRIVATE);
        String currentUserPhone = authPrefs.getString("current_user_phone", "");
        String role = authPrefs.getString(currentUserPhone + "_role", "driver");
        
        TextView tvUserRole = navigationView.getHeaderView(0).findViewById(R.id.tvUserRole);
        if (tvUserRole != null) {
            tvUserRole.setText(role.equals("driver") ? "Driver" : "Commuter");
        }
    }
}
