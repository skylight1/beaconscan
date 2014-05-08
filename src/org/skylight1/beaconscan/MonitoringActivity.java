package org.skylight1.beaconscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;

/**
 *
 */
public class MonitoringActivity extends Activity implements IBeaconConsumer{
	protected static final String TAG = "MonitoringActivity";
//	protected BeaconScanConsumer beaconConsumer;
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitoring);
		verifyBluetooth();
//		beaconConsumer = new BeaconScanConsumer(iBeaconManager, getApplicationContext());
	    iBeaconManager.bind(this);			
	}
	
	public void onRangingClicked(View view) {
		Intent myIntent = new Intent(this, RangingDemoActivity.class);
		this.startActivity(myIntent);
	}

	private void verifyBluetooth() {

		try {
			if (!IBeaconManager.getInstanceForApplication(this).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");			
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finish();
			            System.exit(0);  //TODO: really?				
					}					
				});
				builder.show();
			}			
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Bluetooth LE not available");			
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
		            System.exit(0);  //TODO: really?
				}
			});
			builder.show();	
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
    	if (iBeaconManager.isBound(this)) {
    		iBeaconManager.setBackgroundMode(this, true);
    	}
		unregisterReceiver(intentReceiver);
    }

    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(this)) {
    		iBeaconManager.setBackgroundMode(this, false);
    	}
		registerReceiver(intentReceiver, makeIntentFilter());
    }    
	private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,intent.getExtras().getString(BeaconScanConsumer.EXTRA_DATA));
		}
	};
	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BeaconScanConsumer.UI_INTENT);
		return intentFilter;
	}

	@Override
	public void onIBeaconServiceConnect() {
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
	        @Override
	        public void didEnterRegion(Region region) {
	          String data = "I just saw an iBeacon for the first time!";
	          Log.d(TAG,data);
	  			runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View v = MonitoringActivity.this.findViewById(android.R.id.content);
							v.setBackgroundColor(Color.RED);
							v.invalidate();
						}
	  				
	  			});
	        }
	
	        @Override
	        public void didExitRegion(Region region) {
	        	Log.d(TAG,"I no longer see an iBeacon");
	  			runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View v = MonitoringActivity.this.findViewById(android.R.id.content);
							v.setBackgroundColor(Color.BLUE);
							v.invalidate();
						}
	  				
	  			});
	        }
	
	        @Override
	        public void didDetermineStateForRegion(int state, Region region) {
	        	Log.d(TAG,"I have just switched from seeing/not seeing iBeacons: "+state);     
    	        //TODO: send intent with data
	        }
        });

        try {
            iBeaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {   }
		
	}    	
}
