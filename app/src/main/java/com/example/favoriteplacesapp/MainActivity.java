package com.example.favoriteplacesapp;

import android.arch.persistence.room.Room;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RouteOptions.TransportMode;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

public class MainActivity extends AppCompatActivity {
    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private SupportMapFragment mapFragment = null;

    // map route to display
    private MapRoute mapRoute = null;

    // collection of favorite points
    private List<MapObject> mapPoints = new ArrayList<>();

    // Database to store favorite locations
    private static final String DATABASE_NAME = "favorites_db";
    private FavoritesDatabase favoritesDatabase = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initialize() {
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = getMapFragment();

        // Set up disk cache path for the map service for this application
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "com.example.favoriteplacesapp.MapService");

        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();

                        // Set an initial view
                        map.setCenter(new GeoCoordinate(49.1963, -123.004770, 0.0),
                                Map.Animation.NONE);
                        map.setZoomLevel(12);

                        // create the database
                        favoritesDatabase = Room.databaseBuilder(getApplicationContext(),
                                FavoritesDatabase.class, DATABASE_NAME).fallbackToDestructiveMigration().build();

                        // load previous sessions points
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                List<Favorites> lastFavorites = favoritesDatabase.daoAccess().fetchAll();
                                for (Favorites favorite : lastFavorites) {
                                    MapMarker marker = new MapMarker();
                                    marker.setCoordinate(new GeoCoordinate(favorite.getLatitude(), favorite.getLongitude()));
                                    mapPoints.add(marker);
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        map.addMapObjects(mapPoints);
                                    }
                                });
                            }
                        }).start();

                        // turn on the position and navigation setup
                        PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);
                        mapFragment.getPositionIndicator().setVisible(true);
                        mapFragment.getMapGesture().addOnGestureListener(gestures,0,true);
                        NavigationManager.getInstance().setMap(map);
                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");
                    }
                }
            });
        }
    }

    MapGesture.OnGestureListener gestures = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() {

        }

        @Override
        public void onPanEnd() {

        }

        @Override
        public void onMultiFingerManipulationStart() {

        }

        @Override
        public void onMultiFingerManipulationEnd() {

        }

        @Override
        public boolean onMapObjectsSelected(List<ViewObject> list) {
            for (ViewObject obj: list
                 ) {
                if(obj instanceof MapMarker) {
                    MapMarker marker = (MapMarker)obj;
                    if(PositioningManager.getInstance().hasValidPosition()) {
                        calculateRoute(PositioningManager.getInstance().getPosition().getCoordinate(), marker.getCoordinate());
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onPinchLocked() {

        }

        @Override
        public boolean onPinchZoomEvent(float v, PointF pointF) {
            return false;
        }

        @Override
        public void onRotateLocked() {

        }

        @Override
        public boolean onRotateEvent(float v) {
            return false;
        }

        @Override
        public boolean onTiltEvent(float v) {
            return false;
        }

        @Override
        public boolean onLongPressEvent(PointF pointF) {
            MapMarker marker = new MapMarker();
            final GeoCoordinate coordinate = map.pixelToGeo(pointF);
            marker.setCoordinate(coordinate);
            mapPoints.add(marker);
            map.addMapObject(marker);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    favoritesDatabase.daoAccess().insert(new Favorites(coordinate.getLatitude(),coordinate.getLongitude()));
                }
            }).start();
            return false;
        }

        @Override
        public void onLongPressRelease() {

        }

        @Override
        public boolean onTwoFingerTapEvent(PointF pointF) {
            return false;
        }
    };

    private class RouteLister implements CoreRouter.Listener {
        public void onProgress(int percentage) {

        }

        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {
            if (error == RoutingError.NONE) {
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                map.addMapObject(mapRoute);
                NavigationManager.getInstance().setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
                NavigationManager.getInstance().startNavigation(routeResult.get(0).getRoute());
            }
        }
    }

    void calculateRoute(GeoCoordinate start, GeoCoordinate end) {
        CoreRouter router = new CoreRouter();
        RoutePlan plan = new RoutePlan();
        plan.addWaypoint(new RouteWaypoint(start));
        plan.addWaypoint(new RouteWaypoint(end));
        RouteOptions options = new RouteOptions();
        options.setTransportMode(TransportMode.CAR);
        options.setRouteType(RouteOptions.Type.FASTEST);
        plan.setRouteOptions(options);
        router.calculateRoute(plan, new RouteLister());
    }

    public void stopNavigation(View v) {
        NavigationManager.getInstance().stop();
        if(mapRoute != null && map != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }
    }

    public void clearPoints(View v) {
        map.removeMapObjects(mapPoints);
        mapPoints.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                favoritesDatabase.daoAccess().deleteAll();
            }
        }).start();
    }

    /* Following code dealing with permissions is provided from the HERE BasicSolutionMap sample
     * provided in the SDK download.
     */
    /**
     * Checks the dynamically controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }
}
