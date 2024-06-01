package com.carlex.drive;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import java.util.concurrent.ThreadLocalRandom;

public class FakeLocationService1 extends Service {
    public double latitude, longitude, altitude;
    public float bearing;
    public double velocidade;
    public static final String TAG = "FakeLocationService";
    public static boolean isRunning = false;
    public static boolean parado = true;
    public Thread backgroundThread;
    public static boolean processado;
    public FusedLocationsProvider fusedLocationsProvider;
    public xLocationManager locationManager;
    public Context context;
    private static final String CHANNEL_ID = "FakeLocationServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        context = MainActivity.mainApp;
        locationManager = xLocationManager.getInstance(MainActivity.mainApp);

        createNotificationChannel();
        startForeground(1, getNotification("66 Fake Gps Ligado").build());

	isRunning = true;

        // Obter a última posição conhecida
        latitude = locationManager.getLatitude();
        longitude = locationManager.getLongitude();
        altitude = locationManager.getAltitude();
        bearing = locationManager.getBearing();
        fusedLocationsProvider = new FusedLocationsProvider(this);
        xLocationManager.initTestProvider(this);
        locationManager = xLocationManager.getInstance(this);
    }


    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {

	    //iniciar servico loop infinito
            while (true) {

                //Limpaar dados antigos
		MyApp.getDatabase().rotaFakeDao().deleteRotaFakeWithTimeGreaterThan(System.currentTimeMillis());
                RotaFake rotaFake1 = MyApp.getDatabase().rotaFakeDao().getRotaFakeWithMinTime(System.currentTimeMillis());

		//Verificar se o veículo está parado
		if (locationManager.getSpeed()>0.5 && parado == true){   
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) { NotificationManagerCompat.from(this).notify(2, getNotification("66 em Rota").build()); }              
			parado = false;                  
		} 

		if (locationManager.getSpeed()<=0.5 && parado == false){
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) { NotificationManagerCompat.from(this).notify(2, getNotification("66 Estacionado").build()); }            
			parado = true;                        
		}

		//iniciar o processo de spoofing
		if (rotaFake1 != null) {
		    Location gpsLocation = spoofLocation(rotaFake1, LocationManager.GPS_PROVIDER);
		    Location networkLocation = spoofLocation(rotaFake1, LocationManager.NETWORK_PROVIDER);
		    Location fusedLocation = spoofLocation(rotaFake1, "fused");

                    //simular provedores localizacao
		    if (latitude < -10.0) {
                        locationManager.setGpsProvider(gpsLocation);
			fusedLocationsProvider.spoof(fusedLocation);
			locationManager.setNetworkProvider(networkLocation);
                    }

		    //esperar tempo para proxima atualizacao
                    long tempo = rotaFake1.getTempo();
                    long diferencaTempo = tempo - System.currentTimeMillis();
		    if (diferencaTempo > 0) {
			    try {Thread.sleep(diferencaTempo);} 
			    catch (InterruptedException e) {}
                    }
                } else {

			//spoofing estacionado
			if (latitude < -10.0){             
			    RotaFake rotaFakeEntry = new RotaFake(latitude, longitude, bearing, 0.0, (long) (System.currentTimeMillis() + 150));
			    MyApp.getDatabase().rotaFakeDao().insert(rotaFakeEntry);
		    	} 
			
			//esperar 0.1s para proxima atualizacao
			try {Thread.sleep((long) 100); }   
			catch (InterruptedException e) {}         
		}                                    

		//limpar pontos antigos
		MyApp.getDatabase().rotaFakeDao().deleteRotaFakeWithTimeGreaterThan(System.currentTimeMillis()); 
		//fim while
	    }

	//fim thread
	}).start();

	//nao reiniciar 
	return START_NOT_STICKY;
    }

	
    //criar localizacao spoofada
    public Location spoofLocation(RotaFake rotaFake1, String provider) {
	    latitude = rotaFake1.getLatitude();   
	    longitude = rotaFake1.getLongitude();       
	    bearing = rotaFake1.getBearing();        
	    velocidade = rotaFake1.getVelocidade();                                                                                  
	    Location gpsLocation = new Location(provider);
	    float noise = (float) (ThreadLocalRandom.current().nextDouble(0, 20)/10);   
	    long Timef = System.currentTimeMillis();              
	    gpsLocation.setLatitude(latitude);                
	    gpsLocation.setLongitude(longitude);            
	    gpsLocation.setBearing(bearing+(noise/2));         
	    gpsLocation.setSpeed((float) ((velocidade+(noise/3.6f))/4));
	    gpsLocation.setTime(Timef);                         
	    gpsLocation.setAltitude((double) ((700 + Math.random() * 50)+noise));
	    gpsLocation.setAccuracy((float) (ThreadLocalRandom.current().nextDouble(0, 20)/10));                                                       
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		    gpsLocation.setVerticalAccuracyMeters((float) (ThreadLocalRandom.current().nextDouble(0, 20)/10));            
		    gpsLocation.setSpeedAccuracyMetersPerSecond(noise/3.6f);
		    gpsLocation.setBearingAccuracyDegrees(noise/2);
	    }

	    gpsLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
	    return gpsLocation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
	isRunning = false;
        stopForeground(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(2, getNotification("Fake Location Service Stopped").build());
        } 
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private NotificationCompat.Builder getNotification(String message) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fake Location Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.ico)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Fake Location Service Channel";
            String description = "Channel for Fake Location Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
