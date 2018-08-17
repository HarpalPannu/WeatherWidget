package com.example.harpalpannu.weatherwidget;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.LOCATION_SERVICE;

public class WeatherWidget extends AppWidgetProvider {
    SharedPreferences coordinates;
    LocationManager mLocationManager;
    DecimalFormat decimalFormat = new DecimalFormat("##.#");

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        Intent intent = new Intent(context, WeatherWidget.class);
        intent.setAction("WidgetUpdate");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.refreshButton, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        makeRequest(context);
    }

    @Override
    public void onEnabled(Context context) {
        makeRequest(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "WidgetUpdate")) {
            Toast toast = Toast.makeText(context, "Updating..", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            Intent updateIntent = new Intent(context, WeatherWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WeatherWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(updateIntent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onDisabled(Context context) {

    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            mLocationManager.removeUpdates(mLocationListener);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    public void makeRequest(final Context context) {
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (mLocationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              Toast toast =  Toast.makeText(context,"Enable Location From App Setting",Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000,
                    10, mLocationListener);
        }
        if (mLocationManager != null) {
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                String locationData = lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
                coordinates = context.getSharedPreferences("locData", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = coordinates.edit();
                editor.putString("locData", locationData);
                editor.apply();
            }
        }
        final String[] lnglat = Objects.requireNonNull(coordinates.getString("locData", null)).split(",");
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Log.d("Hz", Arrays.toString(lnglat) + "");
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(Double.parseDouble(lnglat[0]), Double.parseDouble(lnglat[1]), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final List<Address> finalAddresses = addresses;
        String JsonURL = "https://api.openweathermap.org/data/2.5/weather?q=" + (finalAddresses != null ? finalAddresses.get(0).getLocality() + "," + finalAddresses.get(0).getCountryCode() : "Tarn Taran,IN") + "&appid=b36fc6876bd09a09e4fe190a3d9cf51e&units=metric";
        Log.d("Hz", JsonURL);

        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, JsonURL, new Response.Listener<JSONObject>() {

            public void onResponse(JSONObject response) {
                try {
                    JSONArray weatherData = response.getJSONArray("weather");
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("MM-dd hh:mm a", Locale.getDefault());
                    String formattedDate = df.format(c.getTime());
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
                    ComponentName thisWidget = new ComponentName(context, WeatherWidget.class);
                    remoteViews.setTextViewText(R.id.currentTemperature, String.valueOf(decimalFormat.format(Double.parseDouble(response.getJSONObject("main").getString("temp")))) + "°C");
                    remoteViews.setTextViewText(R.id.windSpeed, String.valueOf(response.getJSONObject("wind").getString("speed")) + " KMH");
                    remoteViews.setTextViewText(R.id.cityName, response.getString("name"));


                    remoteViews.setTextViewText(R.id.weatherCondition, weatherData.getJSONObject(0).getString("description"));
                    remoteViews.setTextViewText(R.id.updatedTime, "Updated On : " + formattedDate);
                    remoteViews.setTextViewText(R.id.humidity, String.valueOf(response.getJSONObject("main").getString("humidity")));
                    double t = Double.parseDouble(response.getJSONObject("main").getString("temp"));
                    double v = Double.parseDouble((response.getJSONObject("wind").getString("speed")));
                    double w = 13.12 + (.6215 * t) - (11.37 * Math.pow(v, 0.16)) + (.3965 * t * Math.pow(v, 0.16));

                    remoteViews.setTextViewText(R.id.feelsLike, "Feels like : " + decimalFormat.format(w) + "°C");
                    String icon = "i" + weatherData.getJSONObject(0).getString("icon");
                    Log.d("Hz", icon);
                    int drawableResourceId = context.getResources().getIdentifier(icon, "drawable", context.getPackageName());
                    Bitmap image = BitmapFactory.decodeResource(context.getResources(), drawableResourceId);

                    remoteViews.setImageViewBitmap(R.id.widgetIcon,image);
                    appWidgetManager.updateAppWidget(thisWidget, remoteViews);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Toast toast =  Toast.makeText(context,"Unable To Update",Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
        );
        requestQueue.add(objectRequest);

    }


}

