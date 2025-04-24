package com.gaurav.geomapmarker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
//import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import androidx.annotation.NonNull;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseHelper databaseHelper;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationProviderClient fusedLocationClient;

    private final List<String> categories = new ArrayList<>(Arrays.asList("Favorites", "Friends", "Family", "Add Category"));
    private final HashMap<String, Marker> markerMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showWelcomeDialog();
        databaseHelper = new DatabaseHelper(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if GPS is enabled before requesting location
        checkGpsStatus();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        Button btnShowMarkers = findViewById(R.id.btn_show_markers);
        btnShowMarkers.setOnClickListener(view -> showMarkerList());

        Button btnShowCategories = findViewById(R.id.btn_show_categories);
        btnShowCategories.setOnClickListener(v -> showCategoryDialog());

        Button btnAddMarker = findViewById(R.id.current_loc);
        btnAddMarker.setOnClickListener(view -> moveToUserLocation());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted
            getUserLocation();
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastKnownLocation();
        }
        loadCategories();
        EditText searchMarker = findViewById(R.id.search_marker);

        searchMarker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence query, int i, int i1, int i2) {
                searchForMarker(query.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }
    private void showWelcomeDialog() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean dontShowAgain = prefs.getBoolean("dont_show_welcome", false);

        if (dontShowAgain) return; // Don't show if user checked "Don't Show Again"

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_welcome, null);

        TextView tvMessage = dialogView.findViewById(R.id.tv_welcome_message);
        CheckBox checkBoxDontShow = dialogView.findViewById(R.id.checkbox_dont_show_again);
        Button btnClose = dialogView.findViewById(R.id.btn_close_welcome);

        String welcomeMessage = "🚀 Explore and Manage Your Markers with Ease!\n\n" +
                "📍 Features:\n" +
                "✔️ View and interact with a Google Map\n" +
                "✔️ Add markers even when offline!\n" +
                "✔️ Create and manage custom categories for markers\n" +
                "✔️ Search for markers by name or category\n" +
                "✔️ Filter markers by category on the map\n" +
                "✔️ View your current location anytime\n" +
                "✔️ Edit & delete markers directly from the details dialog\n" +
                "✔️ Share marker details via social media or messaging apps\n" +
                "✔️ Navigate to markers using Google Maps\n\n" +
                "🔹 Important Tips:\n" +
                "- The My Location button brings you back to your current location.\n" +
                "- To add a marker, just tap anywhere on the map.\n" +
                "- The Show Markers button only filters markers on the map, it won’t show a list.\n" +
                "- The Search bar helps find existing markers only, not locations, and does not autocomplete.\n\n" +
                "📌 Enjoy a seamless mapping experience! 🚀";

        tvMessage.setText(welcomeMessage);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> {
            if (checkBoxDontShow.isChecked()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("dont_show_welcome", true);
                editor.apply();
            }
            dialog.dismiss();
        });

        dialog.show();
    }


    private void moveToUserLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng userLocation = new LatLng(latitude, longitude);

                        // Move camera to the user's location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                    } else {
                        Toast.makeText(this, "Location not available. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void filterMarkersByCategory(String category) {
        mMap.clear(); // Remove all markers

        List<String[]> markers = databaseHelper.getMarkers();
        for (String[] markerData : markers) {
            String name = markerData[0];
            double lat = Double.parseDouble(markerData[1]);
            double lng = Double.parseDouble(markerData[2]);
            String markerCategory = markerData[3];

            if (markerCategory.equals(category)) {
                LatLng position = new LatLng(lat, lng);
                mMap.addMarker(new MarkerOptions().position(position).title(name).snippet(markerCategory));
            }
        }

        Toast.makeText(this, "Showing markers for: " + category, Toast.LENGTH_SHORT).show();
    }

    private void searchForMarker(String query) {
        if (query.isEmpty()) {
            return;
        }

        // Find the marker by name
        Marker marker = markerMap.get(query.toLowerCase());
        if (marker != null) {
            LatLng position = marker.getPosition();

            // Zoom in on the found marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));

            // Show an info window
            marker.showInfoWindow();
        } else {
            Toast.makeText(this, "Marker not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGpsStatus() {
        if (isInternetAvailable()) {  // Check if internet is active
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Please enable GPS or Network Location", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }
    }
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));

        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required to show your current location.", Toast.LENGTH_SHORT).show();

                loadLastKnownLocation();
            }
        }
    }
    private void loadLastKnownLocation() {
        SharedPreferences prefs = getSharedPreferences("location_prefs", MODE_PRIVATE);
        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToLongBits(0.0)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToLongBits(0.0)));

        if (latitude != 0.0 && longitude != 0.0) {
            LatLng lastKnownLatLng = new LatLng(latitude, longitude);

            // Move the camera to the last known location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 16));
            mMap.addMarker(new MarkerOptions().position(lastKnownLatLng).title("Last Known Location"));

        } else {
            Toast.makeText(this, "No saved location found. Fetching current location...", Toast.LENGTH_SHORT).show();

            getLastKnownLocation();
        }
    }


    private void getLastKnownLocation() {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Get last known location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
                        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

                        // Save the location to shared preferences for offline use
                        saveLocationToPreferences(location);
                    } else {
                        // Handle location being null
                        loadLocationFromPreferences();
                    }
                })
                .addOnFailureListener(this, e -> {
                    // Handle failure in obtaining location
                    loadLocationFromPreferences();
                });
    }

    private void saveLocationToPreferences(Location location) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_lat", String.valueOf(location.getLatitude()));
        editor.putString("last_lng", String.valueOf(location.getLongitude()));
        editor.apply();
    }

    private void loadLocationFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lat = prefs.getString("last_lat", null);
        String lng = prefs.getString("last_lng", null);

        if (lat != null && lng != null) {
            LatLng savedLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedLatLng, 16));
            mMap.addMarker(new MarkerOptions().position(savedLatLng).title("Last known location"));
        } else {
            // Handle case where no location is saved
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddCategoryDialog(List<String> categories, ArrayAdapter<String> adapter, Spinner categorySpinner) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        final EditText input = new EditText(this);
        input.setHint("Enter category name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCategory = input.getText().toString().trim();
            if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
                categories.add(categories.size() - 1, newCategory); // Add before "Add Category"
                adapter.notifyDataSetChanged();
                categorySpinner.setSelection(categories.indexOf(newCategory));

                saveCategoriesToPreferences(categories);
            } else {
                Toast.makeText(this, "Category already exists or is empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveCategoriesToPreferences(List<String> categories) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringBuilder categoriesString = new StringBuilder();
        for (String category : categories) {
            if (!category.equals("Add Category")) { // Don't save "Add Category" option
                categoriesString.append(category).append(",");
            }
        }

        editor.putString("saved_categories", categoriesString.toString());
        editor.apply();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Load saved markers
        loadMarkersFromDatabase();

        if (isNetworkAvailable()) {
            getLastKnownLocation();
        } else {
            loadLocationFromPreferences();
        }

        mMap.setOnMapClickListener(this::showSaveMarkerDialog);
        mMap.setOnMarkerClickListener(marker -> {
            String markerTitle = marker.getTitle();

            // Retrieve marker details from the database
            List<String[]> markers = databaseHelper.searchMarkers(markerTitle);

            if (!markers.isEmpty()) {
                String[] markerData = markers.get(0);

                String name = markerData[0];
                double lat = Double.parseDouble(markerData[1]);
                double lng = Double.parseDouble(markerData[2]);
                String category = markerData[3];
                String description = markerData.length > 4 ? markerData[4] : "No Description";

                showMarkerDetailsDialog(name, lat, lng, category, description);
            }
            return true;
        });
    }
    @SuppressLint("SetTextI18n")
    private void showMarkerDetailsDialog(String name, double lat, double lng, String category, String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Marker Details");

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_marker_details, null);

        TextView tvName = dialogView.findViewById(R.id.tv_marker_name);
        TextView tvCategory = dialogView.findViewById(R.id.tv_marker_category);
        TextView tvDescription = dialogView.findViewById(R.id.tv_marker_description);
        Button btnNavigate = dialogView.findViewById(R.id.btn_navigate);
        Button btnShare = dialogView.findViewById(R.id.btn_share);
        Button btnEdit = dialogView.findViewById(R.id.btn_edit_marker);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_marker);

        tvName.setText(name);
        tvCategory.setText("Category: " + category);
        tvDescription.setText("Description: " + description);

        LatLng markerLatLng = new LatLng(lat, lng);

        // Navigate Button - Opens Google Maps
        btnNavigate.setOnClickListener(v -> {
            String uri = "google.navigation:q=" + lat + "," + lng;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps"); // Ensures Google Maps is used
            startActivity(intent);
        });

        // Share Button - Opens Share Dialog
        btnShare.setOnClickListener(v -> {
            String shareText = "📍 Marker: " + name + "\n" +
                    "📍 Location: " + lat + ", " + lng + "\n" +
                    "📌 Category: " + category + "\n" +
                    "📝 Description: " + description + "\n\n" +
                    "📍 View on Maps: https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share Marker Details"));
        });

        // Edit Button - Opens Edit Marker Dialog
        btnEdit.setOnClickListener(v -> {
            String[] markerData = {name, String.valueOf(lat), String.valueOf(lng), category, description};
            showEditMarkerDialog(markerData);
        });

        // Delete Button - Deletes marker
        btnDelete.setOnClickListener(v -> {
            databaseHelper.deleteMarker(name, markerLatLng);
            mMap.clear(); // Clear the map
            loadMarkersFromDatabase(); // Reload markers
            Toast.makeText(this, "Marker deleted", Toast.LENGTH_SHORT).show();
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false; // No active network

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void getUserLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permission from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return; // Exit the function until permission is granted
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Location is available
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        LatLng userLatLng = new LatLng(latitude, longitude);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));

                        Toast.makeText(this, "Location Found!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Location is null, request an updated location
                        requestNewLocationData();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    private void requestNewLocationData() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 60000) // Request location every 60 seconds
                .setMinUpdateIntervalMillis(2000) // Fastest update interval: 2 seconds
                .setMaxUpdates(1) // Request only once
                .build();


        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        LatLng userLatLng = new LatLng(latitude, longitude);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));

                        Toast.makeText(getApplicationContext(), "Updated Location Found!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, Looper.getMainLooper());
    }

    private void loadMarkersFromDatabase() {
        mMap.clear();
        markerMap.clear();

        List<String[]> markers = databaseHelper.getMarkers();
        for (String[] markerData : markers) {
            String name = markerData[0];
            double lat = Double.parseDouble(markerData[1]);
            double lng = Double.parseDouble(markerData[2]);
            String category = markerData[3];

            LatLng position = new LatLng(lat, lng);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(name)
                    .snippet(category);

            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                markerMap.put(name.toLowerCase(), marker);
            }
        }
    }

    private void showSaveMarkerDialog(LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Marker");

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_save_marker, null);

        EditText inputName = dialogView.findViewById(R.id.input_marker_name);
        EditText inputDescription = dialogView.findViewById(R.id.input_marker_description);
        Spinner categorySpinner = dialogView.findViewById(R.id.category_spinner);

        List<String> categories = loadCategoriesFromPreferences();
        if (!categories.contains("Add Category")) {
            categories.add("Add Category");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (categories.get(position).equals("Add Category")) {
                    showAddCategoryDialog(categories, adapter, categorySpinner);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String markerName = inputName.getText().toString().trim();
            String markerDescription = inputDescription.getText().toString().trim(); // Get description
            String selectedCategory = categorySpinner.getSelectedItem().toString();

            if (!markerName.isEmpty()) {
                // Save to database
                databaseHelper.addMarker(markerName, latLng, selectedCategory, markerDescription);

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(markerName)
                        .snippet(markerDescription));

                Toast.makeText(this, "Marker saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Marker name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showMarkerList() {
        List<String[]> markers = databaseHelper.getMarkers();
        if (markers.isEmpty()) {
            Toast.makeText(this, "No markers to display!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Marker List");

        String[] markerNames = markers.stream().map(marker -> marker[0]).toArray(String[]::new);
        builder.setItems(markerNames, (dialog, which) -> {
            String[] selectedMarker = markers.get(which);
            showMarkerOptionsDialog(selectedMarker);
        });

        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showMarkerOptionsDialog(String[] markerData) {
        String markerName = markerData[0];
        LatLng latLng = new LatLng(Double.parseDouble(markerData[1]), Double.parseDouble(markerData[2]));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(markerName);
        builder.setItems(new String[]{"Edit", "Delete", "Show on Map"}, (dialog, which) -> {
            switch (which) {
                case 0: // Edit
                    showEditMarkerDialog(markerData);
                    break;
                case 1: // Delete
                    deleteMarker(markerName, latLng);
                    break;
                case 2: // Show on Map
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                    break;
            }
        });
        builder.show();
    }

    private void deleteMarker(String name, LatLng latLng) {
        databaseHelper.deleteMarker(name, latLng);
        mMap.clear();
        loadMarkersFromDatabase();
        Toast.makeText(this, "Marker deleted!", Toast.LENGTH_SHORT).show();
    }

    private void showEditMarkerDialog(String[] markerData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Marker");

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_save_marker, null);

        EditText inputName = dialogView.findViewById(R.id.input_marker_name);
        EditText inputDescription = dialogView.findViewById(R.id.input_marker_description); // New Description Field
        Spinner categorySpinner = dialogView.findViewById(R.id.category_spinner);

        inputName.setText(markerData[0]);
        inputDescription.setText(markerData[4]);

        List<String> categories = loadCategoriesFromPreferences();

        if (!categories.contains("Add Category")) {
            categories.add("Add Category");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        if (categories.contains(markerData[3])) {
            categorySpinner.setSelection(categories.indexOf(markerData[3]));
        } else {
            categories.add(categories.size() - 1, markerData[3]); // Add custom category before "Add Category"
            adapter.notifyDataSetChanged();
            categorySpinner.setSelection(categories.indexOf(markerData[3]));
        }

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (categories.get(position).equals("Add Category")) {
                    showAddCategoryDialog(categories, adapter, categorySpinner);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newDescription = inputDescription.getText().toString().trim(); // Get updated description
            String newCategory = categorySpinner.getSelectedItem().toString();

            if (!newName.isEmpty()) {
                LatLng latLng = new LatLng(Double.parseDouble(markerData[1]), Double.parseDouble(markerData[2]));
                databaseHelper.updateMarker(markerData[0], newName, latLng, newCategory, newDescription); // Update description

                mMap.clear();
                loadMarkersFromDatabase();
                Toast.makeText(this, "Marker updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Marker name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private List<String> loadCategoriesFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedCategories = sharedPreferences.getString("saved_categories", "Favorites,Friends,Family"); // Default categories

        return new ArrayList<>(Arrays.asList(savedCategories.split(",")));
    }


    private void showCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Category");

        List<String> allCategories = new ArrayList<>();
        allCategories.add("Show All Markers");
        allCategories.addAll(databaseHelper.getAllCategories());
        allCategories.add("Add Category");

        // Convert list to array for dialog
        String[] categoryArray = allCategories.toArray(new String[0]);

        builder.setItems(categoryArray, (dialog, which) -> {
            String selectedCategory = allCategories.get(which);

            if (selectedCategory.equals("Add Category")) {
                showAddCategoryDialog();
            } else if (selectedCategory.equals("Show All Markers")) {
                showAllMarkers();
            } else {
                filterMarkersByCategory(selectedCategory);
            }
        });

        builder.show();
    }
    private void showAllMarkers() {
        mMap.clear();
        loadMarkersFromDatabase();
        Toast.makeText(this, "Showing all markers", Toast.LENGTH_SHORT).show();
    }
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        EditText input = new EditText(this);
        input.setHint("Enter category name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCategory = input.getText().toString().trim();

            if (!newCategory.isEmpty() && !databaseHelper.getAllCategories().contains(newCategory)) {
                databaseHelper.addCategory(newCategory); // Save category in DB
                Toast.makeText(this, "Category added: " + newCategory, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Category already exists or is empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
    private void loadCategories() {
        SharedPreferences prefs = getSharedPreferences("CategoryPrefs", MODE_PRIVATE);
        Set<String> categorySet = prefs.getStringSet("user_categories", new HashSet<>(Arrays.asList("Favorites", "Friends", "Family", "Add Category")));

        categories.clear();
        categories.addAll(categorySet);
        if (!categories.contains("Add Category")) {
            categories.add("Add Category");
        }
    }
}