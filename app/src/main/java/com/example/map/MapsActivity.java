package com.example.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnPolylineClickListener{

    //for map and get the current location on Map
    private GoogleMap mMap;
    private View mapView;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastLocation;
    private LocationCallback mLocationCallback;
    private final float DEFAULT_ZOOM = 15;

    //for search bar
    private MaterialSearchBar materialSearchBar;

    //for Places
    private PlacesClient placesClient;
    private List<AutocompletePrediction> autocompletePredictionsList;
    //for Marker on Map

    private ArrayList<Marker> markersList = new ArrayList<>();

    //for Draw Rout on Map
    private GeoApiContext mGeoApiContext;

    // for Polyline data
    private ArrayList<PolylineData> mPolylineData=new ArrayList<>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        materialSearchBar = findViewById(R.id.searchBar);


        //permission for location
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "permission granted ", Toast.LENGTH_SHORT).show();


        } else {
            Dexter.withActivity(MapsActivity.this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {


                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            if (response.isPermanentlyDenied()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                builder.setTitle("permission Denied")
                                        .setMessage("Permission to access device location is permanently denied. you need to go to setting to allow the permission.")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent settingIntent = new Intent();
                                                settingIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                settingIntent.setData(Uri.fromParts("package", getPackageName(), null));

                                            }
                                        }).create().show();

                            } else {
                                Toast.makeText(MapsActivity.this, "permission denied", Toast.LENGTH_SHORT).show();


                            }


                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();

                        }
                    }).check();

        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //for Search places on  searchBar
        Places.initialize(MapsActivity.this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // initialized google map key to Draw Directions on map
        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_key))
                    .build();
        }

        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //  Log.i("showName", "@@IN SEARCH CONFRIMED:@@ " + text.toString());


                //   startSearch(text.toString(), true, null, true);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    materialSearchBar.disableSearch();


                }

            }
        });

        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        .setCountry("pk")
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setSessionToken(token)
                        .setQuery(s.toString())
                        .build();

                placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {

                        if (task.isSuccessful()) {


                            FindAutocompletePredictionsResponse predictionsResponse = task.getResult();

                            if (predictionsResponse != null) {

                                autocompletePredictionsList = predictionsResponse.getAutocompletePredictions();
                                List<String> suggestionList = new ArrayList<>();
                                for (int i = 0; i < autocompletePredictionsList.size(); i++) {
                                    AutocompletePrediction prediction = autocompletePredictionsList.get(i);
                                    suggestionList.add(prediction.getFullText(null).toString());


                                }
                                materialSearchBar.updateLastSuggestions(suggestionList);

                                if (!materialSearchBar.isSuggestionsVisible()) {
                                    materialSearchBar.showSuggestionsList();

                                }


                            }

                        } else {
                            Log.i("showName", "prediction fetching task unsuccessful");


                        }

                    }
                });


            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    if (materialSearchBar.isSuggestionsVisible()) {
                        materialSearchBar.clearSuggestions();

                    }
                    Log.i("showName", "@@ State is changeed :@@ ");


                }

            }
        });


        materialSearchBar.setSuggstionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if (position > autocompletePredictionsList.size()) {
                    return;
                }
                AutocompletePrediction selectAutocompletePrediction = autocompletePredictionsList.get(position);
                String suggestion = materialSearchBar.getLastSuggestions().get(position).toString();
                materialSearchBar.setText(suggestion);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        materialSearchBar.clearSuggestions();

                    }
                }, 1000);

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(materialSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                final String placeId = selectAutocompletePrediction.getPlaceId();
                List<Place.Field> placeField = Arrays.asList(Place.Field.LAT_LNG);

                FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, placeField).build();
                placesClient.fetchPlace(placeRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();
                        LatLng latLng = place.getLatLng();


                        String name = getLocationName(latLng);


                        if (latLng != null) {
                            removePreviousMarker();


                            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(name));

                            // LatLngBounds lngBounds= createBoundsWithMinDiagonal(marker);
                            //CameraUpdate cameraUpdate=CameraUpdateFactory.newLatLngBounds(lngBounds,0);
                            // mMap.moveCamera(cameraUpdate);
                            // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                           // setCameraView(marker);
                            calculateDirections(marker);
                            markersList.add(marker);


                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();
                            int statusCode = apiException.getStatusCode();
                            Log.i("mytag", "place not found: " + e.getMessage());
                            Log.i("mytag", "status code: " + statusCode);

                        }

                    }
                });

            }

            @Override
            public void OnItemDeleteListener(int position, View v) {

            }
        });


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // to enable the current location button
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnPolylineClickListener(this);

        //make current location button to bottom

        if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            locationButton.setBackgroundColor(getResources().getColor(R.color.colorGray));
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 40, 180);


        }

        //check if GPS is enable or not

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapsActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(MapsActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Toast.makeText(MapsActivity.this, "if gps on", Toast.LENGTH_SHORT).show();

                getDeviceCurrentLocation();


            }
        });


        task.addOnFailureListener(MapsActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                    try {
                        Toast.makeText(MapsActivity.this, "location off", Toast.LENGTH_SHORT).show();
                        resolvableApiException.startResolutionForResult(MapsActivity.this, 30);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }

                }

            }
        });


        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(31.500556, 434.366702);
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(sydney.latitude,sydney.longitude),17));
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (materialSearchBar.isSuggestionsVisible()) {
                    materialSearchBar.clearSuggestions();
                }
                if (materialSearchBar.isSearchEnabled()) {
                    materialSearchBar.disableSearch();

                }

                return false;
            }
        });
    }

    private String getLocationName(LatLng latLng) {
        String locationName = "";
        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        Log.i("mytag", "geocoder data: " + geocoder.toString());


        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
//             String adress=addresses.get(0).getAdminArea();
            locationName = addresses.get(0).getFeatureName();

            Log.i("mytag", "Location Name: " + locationName);
//            Log.i("mytag", "Location Name**: " +adress);


        } catch (IOException e) {
            Log.i("mytag", "error " + e.getMessage());
            e.printStackTrace();
        }


        return locationName;

    }


    // this method is calculate all possible directions
    private void calculateDirections(Marker marker) {

        com.google.maps.model.LatLng destinations = new com.google.maps.model.LatLng(
                marker.getPosition().latitude, marker.getPosition().longitude
        );


        DirectionsApiRequest direction = new DirectionsApiRequest(mGeoApiContext);
        direction.alternatives(true);
        direction.origin(
                new com.google.maps.model.LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())
        );

        Log.d("check", "calculateDirections destinations : " + destinations.toString());

        direction.destination(destinations).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d("check", "onResult: routes:  : " + result.routes[0].toString());
                Log.d("check", "onResult: geocodedWayPoints:   : " + result.geocodedWaypoints[0].toString());
                addPolylinesToMap(result);


            }

            @Override
            public void onFailure(Throwable e) {
                Log.d("check", "onFailure : " + e.getMessage());


            }
        });


    }

    private void addPolylinesToMap(final DirectionsResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("check", "run: result routes: " + result.routes.length);

                //this if check if  this method is called again we have to check and remove the perivouse polyline from map

                if(mPolylineData.size()>0){
                    for(PolylineData polylineData:mPolylineData){
                        polylineData.getPolyline().remove();

                    }
                    mPolylineData.clear();
                    mPolylineData=new ArrayList<>();

                }
                double duration = 999999999;


                for (DirectionsRoute route : result.routes) {
                    Log.d("check", "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for (com.google.maps.model.LatLng latLng : decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }

                    zoomRoute(mMap,newDecodedPath);
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));

                    polyline.setColor(ContextCompat.getColor(getApplication(), R.color.colorGray));
                    polyline.setClickable(true);

                    //add polyline data to array List
                    mPolylineData.add(new PolylineData(polyline,route.legs[0]));

                    //highlight the fastest rout on Map
                    double tempDuration=route.legs[0].duration.inSeconds;
                    if(tempDuration<duration){
                        duration=tempDuration;
                        onPolylineClick(polyline);

                    }

                }
            }
        });
    }

    // This Method Zoom Out The Map While Draw Rout On Map

    private void zoomRoute(GoogleMap googleMap, List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 100;
        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
    }



    private void removePreviousMarker() {
        if (markersList.isEmpty()) {

        } else {
            for (Marker marker : markersList) {
                marker.remove();

            }

        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 30 && resultCode == Activity.RESULT_OK) {
            getDeviceCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")

    private void getDeviceCurrentLocation() {
        if (mFusedLocationProviderClient == null) {
            Toast.makeText(this, "NULL", Toast.LENGTH_SHORT).show();
        }


        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastLocation = task.getResult();
                            if (mLastLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), DEFAULT_ZOOM));
                            } else {
                                final LocationRequest locationRequest = LocationRequest.create();
                                locationRequest.setInterval(10000);
                                locationRequest.setFastestInterval(5000);
                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                mLocationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        if (locationResult == null) {
                                            return;
                                        }
                                        mLastLocation = locationResult.getLastLocation();
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), DEFAULT_ZOOM));
                                        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);

                                    }
                                };
                                mFusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, null);


                            }

                        } else {
                            Toast.makeText(MapsActivity.this, "unable to get last location", Toast.LENGTH_SHORT).show();
                        }


                    }
                });


    }




    @Override
    public void onPolylineClick(Polyline polyline) {
        for(PolylineData polylineData:mPolylineData){
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(getApplication(),R.color.colorBlue));
                polylineData.getPolyline().setZIndex(1);
                Log.i("check", "onPolylineClick: Time "+polylineData.getLeg().duration);
                Log.i("check", "onPolylineClick: Distance "+polylineData.getLeg().distance);
            }else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(getApplication(),R.color.colorGray));
                polylineData.getPolyline().setZIndex(0);
            }

        }

    }
}
