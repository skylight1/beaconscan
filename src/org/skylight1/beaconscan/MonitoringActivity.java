package org.skylight1.beaconscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.radiusnetworks.ibeacon.IBeaconManager;

/**
 * 
 * @author dyoung
 *
 */
public class MonitoringActivity extends Activity {
	protected static final String TAG = "MonitoringActivity";
	protected BeaconScanConsumer beaconConsumer;
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "oncreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitoring);
		verifyBluetooth();
		beaconConsumer = new BeaconScanConsumer(iBeaconManager);
	    iBeaconManager.bind(beaconConsumer);			
	}
	
	public void onRangingClicked(View view) {
		Intent myIntent = new Intent(this, RangingActivity.class);
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
			            System.exit(0);					
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
        iBeaconManager.unBind(beaconConsumer);
    }
    @Override 
    protected void onPause() {
    	super.onPause();
    	if (iBeaconManager.isBound(beaconConsumer)) iBeaconManager.setBackgroundMode(beaconConsumer, true);    		
    }

    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(beaconConsumer)) iBeaconManager.setBackgroundMode(beaconConsumer, false);    		
    }    
    	
}
