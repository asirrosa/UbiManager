package Main;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

import static Main.SearchActivity.REQUEST_CODE_AUTOCOMPLETE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonIOException;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.WalkingOptions;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationWalkingOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, MenuItem.OnMenuItemClickListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private DirectionsRoute currentRoute;
    private static final String TAG="DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private MenuItem itemSearch, itemRouteOptions;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private LatLng latLng;
    private FloatingActionButton button;
    public String transporte;
    public String[] exclude;
    private Point originPoint;
    private Point destinationPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.mapbox_access_token));
        setContentView(R.layout.navigation_layout);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }

    /**
     * Metodo para crear el inflater con el menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.navigation_menu, menu);

        itemSearch = menu.findItem(R.id.searchNavigation);
        itemSearch.setOnMenuItemClickListener(this);

        itemRouteOptions = menu.findItem(R.id.routeOptions);
        itemRouteOptions.setOnMenuItemClickListener(this);

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.searchNavigation:
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .language("es")
                                .build(PlaceOptions.MODE_CARDS))
                        .build(this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
                break;

            case R.id.routeOptions:
                navDialog();
                break;
        }
        return false;
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), style -> {
            enableLocationComponent(style);
            addDestinationIconSymbolLayer(style);

            //El OnClick
            button = findViewById(R.id.button);
            button.setOnClickListener(v -> {
                boolean simulateRoute = true;
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .darkThemeResId(0)
                        .shouldSimulateRoute(simulateRoute)
                        .build();
                NavigationLauncher.startNavigation(NavigationActivity.this, options);
            });
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    latLng = new LatLng(
                            ((Point) selectedCarmenFeature.geometry()).latitude(),
                            ((Point) selectedCarmenFeature.geometry()).longitude());

                    //para acceder a la navegacion con el punto de meta que hemos buscado
                    navigateToSearchedPlace(
                            ((Point) selectedCarmenFeature.geometry()).latitude(),
                            ((Point) selectedCarmenFeature.geometry()).longitude()
                    );
                }
            }
        }
    }

    public void navDialog(){
        //mostrar el dialog con las opciones de la ruta
        NavigationDialog navigationDialog = new NavigationDialog(this);
        //para que aun clicando el back button o fuera del dialog este no se cierre
        navigationDialog.setCancelable(false);
        navigationDialog.show(getSupportFragmentManager(), "example dialog");
    }

    private void navigateToSearchedPlace(double latitude, double longitude){
        destinationPoint = Point.fromLngLat(longitude, latitude);
        originPoint = Point.fromLngLat(
                locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLongitude()
        );

        //esto lo he quitado porque CREO que no hace nada
        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if(source != null){
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        navigationRouteApiCall();
        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapbox_blue);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this,loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
        }
        else{
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle){
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id","destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    private String excludeOptions(){
        String excludeText = "";
        if(exclude[0] != null){
            excludeText = exclude[0];
            if(exclude[1] != null){
                excludeText = excludeText + "," + exclude[1];
                if(exclude[2] != null){
                    excludeText = excludeText + "," + exclude[2];
                }
            }
            else if(exclude[2] != null){
                excludeText = excludeText + "," + exclude[2];
            }
        }
        else if(exclude[1] != null){
            excludeText = exclude[1];
            if(exclude[2] != null){
                excludeText = excludeText + "," + exclude[2];
            }
        }
        else if(exclude[2] != null){
            excludeText = exclude[2];
        }
        return excludeText;
    }

    private NavigationRoute navigationOptions(){
        NavigationRoute result = null;
        String excludeText = excludeOptions();
        if(excludeText.equals("")){
            if(transporte.equals("driving")) {
                result = NavigationRoute.builder(this)
                        .accessToken(Mapbox.getAccessToken())
                        .alternatives(Boolean.TRUE)
                        .language(new Locale("es_ES"))
                        .enableRefresh(true)
                        .origin(originPoint)
                        .destination(destinationPoint)
                        .build();
            }
            else if(transporte.equals("walking")){
                result = NavigationRoute.builder(this)
                        .accessToken(Mapbox.getAccessToken())
                        .alternatives(Boolean.TRUE)
                        .language(new Locale("es_ES"))
                        .walkingOptions(NavigationWalkingOptions.builder().build())
                        .enableRefresh(true)
                        .origin(originPoint)
                        .destination(destinationPoint)
                        .build();
            }
        }
        else{
            if(transporte.equals("driving")) {
                result = NavigationRoute.builder(this)
                        .accessToken(Mapbox.getAccessToken())
                        .alternatives(Boolean.TRUE)
                        .language(new Locale("es_ES"))
                        .exclude(excludeText)
                        .enableRefresh(true)
                        .origin(originPoint)
                        .destination(destinationPoint)
                        .build();
            }
            else if(transporte.equals("walking")){
                result = NavigationRoute.builder(this)
                        .origin(originPoint)
                        .destination(destinationPoint)
                        .accessToken(Mapbox.getAccessToken())
                        .alternatives(Boolean.TRUE)
                        .language(new Locale("es_ES"))
                        .exclude(excludeText)
                        .walkingOptions(NavigationWalkingOptions.builder().build())
                        .enableRefresh(true)
                        .build();
            }
        }
        return result;
    }

    public void navigationRouteApiCall() {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .alternatives(true)
                .language(Locale.ENGLISH)
                .origin(originPoint)
                .destination(destinationPoint)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {

                        if (response.body() == null) {
                            Toast.makeText(getApplicationContext(), "Developer Message: Algun parametro de la api esta mal", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Toast.makeText(getApplicationContext(), "No se han encontrado rutas a esa ubicacion", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Move map camera to the selected location
                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(latLng)
                                        .zoom(14)
                                        .build()), 4000);

                        //hacer el boton de navegacion visible
                        button.setVisibility(FloatingActionButton.VISIBLE);

                        //draw in the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                            navigationMapRoute.showAlternativeRoutes(true);
                        }

                        List<DirectionsRoute> routes = response.body().routes();
                        for (int i = 0; i < routes.size(); i++) {
                            navigationMapRoute.addRoute(routes.get(i));
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Developer Message: No funciona la funcionalidad", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocationComponent(mapboxMap.getStyle());
        }
        else{
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Please allow GPS on your phone",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
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
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
}
