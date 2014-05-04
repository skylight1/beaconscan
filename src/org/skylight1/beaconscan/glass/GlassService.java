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

import org.skylight1.beaconscan.BeaconScanConsumer;
import org.skylight1.beaconscan.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.radiusnetworks.ibeacon.IBeaconManager;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class GlassService extends Service {

    private static final String LIVE_CARD_TAG = "beaconscan";

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
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private final BeaconScanBinder mBinder = new BeaconScanBinder();

    private TextToSpeech mSpeech;

    private LiveCard mLiveCard;
    private Renderer mRenderer;
    
	protected BeaconScanConsumer beaconConsumer;
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

    
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
		beaconConsumer = new BeaconScanConsumer(iBeaconManager, getApplicationContext());
	    iBeaconManager.bind(beaconConsumer);			
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	showCard();
 
    	// onResume()
    	if (iBeaconManager.isBound(beaconConsumer)) {
    		iBeaconManager.setBackgroundMode(beaconConsumer, false);  //TODO: background  		
    	}    	    	
		registerReceiver(intentReceiver, makeIntentFilter());
        
        return START_STICKY;
    }

    private void showCard() {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new Renderer(this);

            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

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
        mSpeech.shutdown();
        mSpeech = null;
        
        //onPause():
        unregisterReceiver(intentReceiver);
    	//if (iBeaconManager.isBound(beaconConsumer)) {
    	//	iBeaconManager.setBackgroundMode(beaconConsumer, true); 		
    	//}
    	
        iBeaconManager.unBind(beaconConsumer);      
        
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

}
