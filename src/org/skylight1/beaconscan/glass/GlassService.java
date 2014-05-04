/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skylight1.beaconscan.glass;

import java.util.ArrayList;
import java.util.Collection;

import org.skylight1.beaconscan.BeaconScanConsumer;
import org.skylight1.beaconscan.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
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
	private ArrayList<Double> range = new ArrayList<Double>();
	private RemoteViews mLiveCardView;
    private final BeaconScanBinder mBinder = new BeaconScanBinder();
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;
    private Renderer mRenderer;
    
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
        public void readHeadingAloud() {

            Resources res = getResources();
            String[] spokenDirections = res.getStringArray(R.array.spoken_beacon_names);
            String beaconName = "beacon one"; //TODO:.....

            String headingText = "";// TODO: res.getString(format, beaconName);....
//            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
//        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                // Do nothing.
//            }
//        });
//		beaconConsumer = new BeaconScanConsumer(iBeaconManager, getApplicationContext());
	    iBeaconManager.bind(this);			
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	showCard();
 
    	// onResume()
    	if (iBeaconManager.isBound(this)) {
    		iBeaconManager.setBackgroundMode(this, false);  //TODO: background  		
    	}    	    	
		registerReceiver(intentReceiver, makeIntentFilter());
        
        return START_STICKY;
    }

    private void showCard() {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            
			mLiveCardView = new RemoteViews(getPackageName(), R.layout.beaconscan_glass);
//            mRenderer = new Renderer(this);
//            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

			mLiveCardView.setTextViewText(R.id.tips_view,"test");
			
            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            
            mLiveCard.attach(this);            
            
            mLiveCard.publish(PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }
	}
    
	@Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
//        mSpeech.shutdown();
//	        mSpeech = null;
        
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
			Log.d("GLASS ERVICE",intent.getExtras().getString(BeaconScanConsumer.EXTRA_DATA));			
            mLiveCard.navigate();
		}
	};
	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BeaconScanConsumer.UI_INTENT);
		return intentFilter;
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
    		} else if (distance > 1.0f && distance <= 3.0f) {
				setColor(Color.BLACK);
    		} else {
    			setColor(Color.BLUE);
    		}
    		
    	}
    }
    private void setColor(int color) {
		Runnable task = new Runnable() {
			public void run() {
				//View v = mRenderer.getViewById(android.R.id.content);
				//v.setBackgroundColor(Color.RED);
				//v.invalidate();
			}
		};							
	    new Handler(Looper.getMainLooper()).post(task);
    }

}
