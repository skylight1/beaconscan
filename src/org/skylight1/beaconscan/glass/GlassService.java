package org.skylight1.beaconscan.glass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.skylight1.beaconscan.BeaconScanConsumer;
import org.skylight1.beaconscan.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Patterns;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class GlassService extends Service implements IBeaconConsumer {
	protected static final String TAG = "GlassService";
    private static final String LIVE_CARD_TAG = "beaconscan";
	
    public static final String Beacon1_UUID="8deefbb9-f738-4297-8040-96668bb44281";
//	public static final String Beacon1_UUID = new String("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0").toLowerCase();

	
	private ArrayList<Double> range = new ArrayList<Double>();
	private RemoteViews mLiveCardView;
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;    
    String email = "tester@beaconcrawl.glass";
    int previousColor = 0;

    //    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable = new UpdateLiveCardRunnable();
//    private static final long DELAY_MILLIS = 1000;

    private final BeaconScanBinder mBinder = new BeaconScanBinder();
//	protected BeaconScanConsumer beaconConsumer;
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class BeaconScanBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readAloud() {

            Resources res = getResources();
            String[] spokenDirections = res.getStringArray(R.array.spoken_beacon_names);
            String beaconName = "one"; //TODO:.....

            String headingText = "Welcome to Beacon Crawl for Glass. Find the hidden beacon. then post a photo when you find it. To cool your Glass and stop scanning, select stop in the menu.";// TODO: res.getString(format, beaconName);....
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });
        
//		beaconConsumer = new BeaconScanConsumer(iBeaconManager, getApplicationContext());

        iBeaconManager.bind(this);			
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    
	@Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mSpeech.shutdown();
        mSpeech = null;
        
        //onPause():
        unregisterReceiver(intentReceiver);
    	//if (iBeaconManager.isBound(beaconConsumer)) {
    	//	iBeaconManager.setBackgroundMode(beaconConsumer, true); 		
    	//}    	
        iBeaconManager.unBind(this);      
        
        super.onDestroy();
    }
    
	private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("GLASS SERVICE",intent.getExtras().getString(BeaconScanConsumer.EXTRA_DATA));			
            mLiveCard.navigate();
		}
	};

//	private static IntentFilter makeIntentFilter() {
//		final IntentFilter intentFilter = new IntentFilter();
//		intentFilter.addAction(BeaconScanConsumer.UI_INTENT);
//		return intentFilter;
//	}

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
/*
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
	        @Override
	        public void didEnterRegion(Region region) {
	          String data = "I just saw an iBeacon for the first time!";
	          Log.e(TAG,data);
	          mLiveCard.navigate();
	        }
	
	        @Override
	        public void didExitRegion(Region region) {
	        	Log.e(TAG,"I no longer see an iBeacon");
    	        //TODO: send intent with data
	        }
	
	        @Override
	        public void didDetermineStateForRegion(int state, Region region) {
	        	Log.e(TAG,"I have just switched from seeing/not seeing iBeacons: "+state);     
    	        //TODO: send intent with data
	        }
        });
*/
        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {   }		
	}

    private void setDisplay(ArrayList<Double> range) {
    	if(range != null) {
    		double distance;
    		distance = range.get(0);
    		Log.d(TAG,"distance " + distance );
    		if(distance <= 1.0f) {    			
    			setColor(Color.RED);
    			if(previousColor!=Color.RED) {
    				mSpeech.speak("found the beacon!", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "found the beacon!");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.RED;
    		} else if (distance > 1.0f && distance <= 3.0f) {
				setColor(Color.MAGENTA);
    			if(previousColor!=Color.MAGENTA) {
    				mSpeech.speak("getting closer", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "getting closer");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.MAGENTA;
    		} else {
    			setColor(Color.BLUE);
    			if(previousColor!=Color.BLUE) {
    				mSpeech.speak("beacon in the area", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "beacon in the area");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.BLUE;
    		}
    	}
    }
    private void setColor(final int color) {
		Runnable task = new Runnable() {
			public void run() {
				Log.e(TAG,"set color =" + color);
				//View v = mRenderer.getViewById(android.R.id.content);
				//v.setBackgroundColor(Color.RED);
				//v.invalidate();
			}
		};							
	    new Handler(Looper.getMainLooper()).post(task);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
                
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                email = account.name;
                break;
            }
        }
        
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.beaconscan_glass);

            mLiveCardView.setTextViewText(R.id.beacpn_text, "scanning beacons...");
            mLiveCard.setViews(mLiveCardView);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            // Publish the live card
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);

            Log.d(TAG, "mLiveCard.publish " + mLiveCard.isPublished());
            
        } else if (!mLiveCard.isPublished()) {
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }

}
