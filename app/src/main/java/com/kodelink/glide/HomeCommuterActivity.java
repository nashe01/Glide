package com.kodelink.glide;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeCommuterActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnMapLongClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private MaterialButton btnMenu;
    private TextInputEditText etSearch;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SharedPreferences prefs;
    
    // Ride request workflow variables
    private LatLng currentLocation;
    private LatLng destinationLocation;
    private Marker destinationMarker;
    private List<Driver> availableDrivers = new ArrayList<>();
    private List<Marker> driverMarkers = new ArrayList<>();
    private FirebaseService firebaseService;
    private String currentUserId;
    private MaterialCardView driverInfoCard;
    private Driver selectedDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_commuter);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        btnMenu = findViewById(R.id.btnMenu);
        etSearch = findViewById(R.id.etSearch);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Initialize preferences
        prefs = getSharedPreferences("MockAuth", MODE_PRIVATE);
        currentUserId = prefs.getString("current_user_phone", "");

        // Initialize Firebase service
        firebaseService = FirebaseService.getInstance();
        
        // Initialize sample data (for testing)
        DataInitializer.initializeSampleData(this);
        
        // Create commuter profile in Firebase
        createCommuterProfile();

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

        // Set up search functionality
        setupSearchFunctionality();

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
        
        // Set up map long click listener for destination selection
        googleMap.setOnMapLongClickListener(this);
        
        // Set up marker click listener for driver selection
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null && marker.getTag() instanceof Driver) {
                selectedDriver = (Driver) marker.getTag();
                showDriverInfoCard(selectedDriver);
            }
            return true;
        });
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
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            
                            // Add marker for current location
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Your Location"));
                            
                            // Move camera to current location
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                            
                            // Update commuter location in Firebase
                            updateCommuterLocationInFirebase();
                        }
                    });
        }
    }
    
    private void updateCommuterLocationInFirebase() {
        if (currentLocation != null) {
            Commuter.LocationData locationData = new Commuter.LocationData(
                    currentLocation.latitude, 
                    currentLocation.longitude, 
                    "Current Location"
            );
            
            firebaseService.updateCommuterLocation(currentUserId, locationData, new FirebaseService.DatabaseCallback() {
                @Override
                public void onSuccess(String message) {
                    // Location updated successfully
                }
                
                @Override
                public void onError(String error) {
                    // Handle error if needed
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
        String currentUserPhone = prefs.getString("current_user_phone", "");
        String role = prefs.getString(currentUserPhone + "_role", "commuter");
        
        TextView tvUserRole = navigationView.getHeaderView(0).findViewById(R.id.tvUserRole);
        if (tvUserRole != null) {
            tvUserRole.setText(role.equals("driver") ? "Driver" : "Commuter");
        }
    }

    // Map long click listener for destination selection
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        setDestination(latLng);
    }

    private void setDestination(LatLng destination) {
        destinationLocation = destination;
        
        // Remove existing destination marker
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        
        // Add new destination marker
        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destination"));
        
        // Move camera to show both current location and destination
        if (currentLocation != null) {
            // Calculate bounds to show both locations
            LatLng southwest = new LatLng(
                    Math.min(currentLocation.latitude, destination.latitude),
                    Math.min(currentLocation.longitude, destination.longitude)
            );
            LatLng northeast = new LatLng(
                    Math.max(currentLocation.latitude, destination.latitude),
                    Math.max(currentLocation.longitude, destination.longitude)
            );
            
            // Move camera to show both locations
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    new com.google.android.gms.maps.model.LatLngBounds(southwest, northeast), 100));
        }
        
        // Show nearby drivers
        showNearbyDrivers();
        
        Toast.makeText(this, "Destination set! Looking for nearby drivers...", Toast.LENGTH_SHORT).show();
    }

    private void showNearbyDrivers() {
        // Clear existing driver markers
        for (Marker marker : driverMarkers) {
            marker.remove();
        }
        driverMarkers.clear();
        availableDrivers.clear();
        
        // Get available drivers from Firebase
        firebaseService.getAvailableDrivers(new FirebaseService.DriversListener() {
            @Override
            public void onDriversReceived(List<Driver> drivers) {
                availableDrivers = drivers;
                displayDriverMarkers();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(HomeCommuterActivity.this, "Error loading drivers: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDriverMarkers() {
        for (Driver driver : availableDrivers) {
            if (driver.currentLocation != null) {
                LatLng driverLocation = new LatLng(driver.currentLocation.lat, driver.currentLocation.lng);
                Marker driverMarker = googleMap.addMarker(new MarkerOptions()
                        .position(driverLocation)
                        .title(driver.name)
                        .snippet("Rating: " + driver.rating + " | Rides: " + driver.completedRides));
                
                // Set driver object as tag for click handling
                driverMarker.setTag(driver);
                driverMarkers.add(driverMarker);
            }
        }
    }

    private void showDriverInfoCard(Driver driver) {
        // This would show a bottom sheet or card with driver info
        // For now, we'll show a simple dialog
        String driverInfo = "Driver: " + driver.name + "\n" +
                "Rating: " + driver.rating + " stars\n" +
                "Completed Rides: " + driver.completedRides + "\n" +
                "Distance: " + calculateDistance(currentLocation, new LatLng(driver.currentLocation.lat, driver.currentLocation.lng)) + " km";
        
        // Create a simple dialog or bottom sheet here
        // For now, we'll use a toast and then proceed to request ride
        Toast.makeText(this, driverInfo, Toast.LENGTH_LONG).show();
        
        // Automatically request ride after showing info
        requestRide(driver);
    }

    private void requestRide(Driver driver) {
        if (currentLocation == null || destinationLocation == null) {
            Toast.makeText(this, "Please set your destination first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create ride request
        String rideId = "ride_" + System.currentTimeMillis();
        RideRequest rideRequest = new RideRequest(
                rideId,
                currentUserId,
                driver.driverId,
                new RideRequest.LocationData(currentLocation.latitude, currentLocation.longitude, "Current Location"),
                new RideRequest.LocationData(destinationLocation.latitude, destinationLocation.longitude, "Destination"),
                "pending"
        );
        
        // Send ride request to Firebase
        firebaseService.createRideRequest(rideRequest, new FirebaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(HomeCommuterActivity.this, "Ride request sent to " + driver.name, Toast.LENGTH_SHORT).show();
                showWaitingScreen(rideId);
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(HomeCommuterActivity.this, "Failed to send ride request: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWaitingScreen(String rideId) {
        // Listen for ride request status updates
        firebaseService.listenToRideRequest(rideId, new FirebaseService.RideRequestListener() {
            @Override
            public void onRideRequestUpdated(RideRequest rideRequest) {
                switch (rideRequest.status) {
                    case "accepted":
                        Toast.makeText(HomeCommuterActivity.this, "Ride accepted! Driver is on the way.", Toast.LENGTH_LONG).show();
                        // Here you would navigate to ride tracking screen
                        break;
                    case "declined":
                        Toast.makeText(HomeCommuterActivity.this, "Ride declined. Looking for another driver...", Toast.LENGTH_LONG).show();
                        // Remove declined driver from available list and show others
                        showNearbyDrivers();
                        break;
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(HomeCommuterActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchFunctionality() {
        // Set up search bar functionality
        etSearch.setOnClickListener(v -> {
            // Here you would implement address search functionality
            // For now, we'll just show a toast
            Toast.makeText(this, "Search functionality will be implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private double calculateDistance(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) return 0;
        
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;
        
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to kilometers
        
        return Math.round(distance * 100.0) / 100.0;
    }
    
    private void createCommuterProfile() {
        String commuterName = prefs.getString(currentUserId + "_name", "Commuter");
        Commuter commuter = new Commuter(
                currentUserId,
                commuterName,
                currentUserId,
                new Commuter.LocationData(0, 0, "Unknown Location")
        );
        
        firebaseService.createCommuter(commuter, new FirebaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d("HomeCommuterActivity", "Commuter profile created: " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.e("HomeCommuterActivity", "Failed to create commuter profile: " + error);
            }
        });
    }
}
