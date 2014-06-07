package org.skylight1.beaconscan;

import java.util.ArrayList;
import java.util.Collection;

import org.skylight1.beaconscan.glass.GlassService;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.preview.support.wearable.notifications.WearableNotifications;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

public class RangingDemoActivity extends Activity implements IBeaconConsumer {
	public static final String TAG = "RangingDemoActivity";
	
  public static final String Beacon1_UUID="8deefbb9-f738-4297-8040-96668bb44281";
  //public static final String Beacon1_UUID = new String("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0").toLowerCase();

  private static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

	private ArrayList<Double> range = new ArrayList<Double>();
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    int previousColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rangingdemo);
        iBeaconManager.bind(this);
        stopService(new Intent(this, GlassService.class));
     // if running on wearable //TODO: test for 
		moveTaskToBack(true); 
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        iBeaconManager.unBind(this);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, true);    		
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, false);    		
    }

    @Override
    public void onIBeaconServiceConnect() {
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
        @Override 
        public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
        	if(iBeacons != null) {
        		if (iBeacons.size() > 0) {
        			// iterate through each beacon found
        			range.clear();
        			for (IBeacon i : iBeacons) {
        				Log.d(TAG,"UUID:" + i.getProximityUuid() + " dist " + i.getAccuracy());
        				if(i.getProximityUuid().equals(Beacon1_UUID)) {
        					range.add(i.getAccuracy());
        				}
        			}

        			if(range.size() > 0) {
        				setDisplay(range);
        			}
        		}
        	}
        }

        });

        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
    
    private void setDisplay(ArrayList<Double> range) {
    	if(range != null) {
    		double distance;
    		distance = range.get(0);
    		Log.d(TAG,"distance " + distance );
    		if(distance <= 1.0f) {
    			if(previousColor!=Color.RED) {
	    			runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View v = RangingDemoActivity.this.findViewById(android.R.id.content);
							v.setBackgroundColor(Color.RED);
							v.invalidate();
							sendNotification(Color.RED);
						}	    				
	    			});
	    				    			
    			}
    			previousColor=Color.RED;

    		} else if (distance > 1.0f && distance <= 3.0f) {
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View v = RangingDemoActivity.this.findViewById(android.R.id.content);
						v.setBackgroundColor(Color.MAGENTA);
						v.invalidate();
						sendNotification(Color.MAGENTA);
					}
    				
    			});
    		} else {
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View v = RangingDemoActivity.this.findViewById(android.R.id.content);
						v.setBackgroundColor(Color.BLUE);
						v.invalidate();
						sendNotification(Color.BLUE);						
					}
    			});	
    		}		
    	}
    }
    
	
	private void sendNotification(int color) {
	    final int notificationId = 1;
	    int resourceid = R.drawable.ic_marker;
	    if(color==Color.MAGENTA) {
	    	resourceid = R.drawable.ic_marker2;
	    } else if(color==Color.BLUE) {
	    	resourceid = R.drawable.ic_marker3;
	    }
		final Bitmap bitmapIcon = BitmapFactory.decodeResource(this.getResources(), resourceid);
		NotificationManagerCompat.from(this).cancelAll();
		Intent viewIntent = new Intent(this, RangingDemoActivity.class);
		viewIntent.putExtra(EXTRA_EVENT_ID, notificationId);
		PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);

		String eventTitle = getString(R.string.app_name);
		String eventLocation = "- beacon in the area -";
		if(color==Color.RED) {
			eventLocation = "Beacon Found!";
		} else if(color==Color.MAGENTA) {
			eventLocation = "getting closer...";
		}
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
		        .setContentTitle(eventTitle)
		        .setContentText(eventLocation)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setLargeIcon(bitmapIcon)
		        .setContentIntent(viewPendingIntent);

        Notification notification = new WearableNotifications.Builder(notificationBuilder).build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		notificationManager.notify(notificationId, notification);
	}
}