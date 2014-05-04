package org.skylight1.beaconscan;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;

public class BeaconScanConsumer implements IBeaconConsumer {
	protected static final String TAG = "BeaconScanConsumer";
	private IBeaconManager iBeaconManager;

	public BeaconScanConsumer(IBeaconManager iBeaconManager) {
		this.iBeaconManager = iBeaconManager;
	}

	@Override
	public void onIBeaconServiceConnect() {
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
	        @Override
	        public void didEnterRegion(Region region) {
	          Log.d(TAG,"I just saw an iBeacon for the first time!");  
	          //TODO: register intent, UI listeners, send intent with data
	        }
	
	        @Override
	        public void didExitRegion(Region region) {
	        	Log.d(TAG,"I no longer see an iBeacon");
    	        //TODO: register intent, UI listeners, send intent with data
	        }
	
	        @Override
	        public void didDetermineStateForRegion(int state, Region region) {
	        	Log.d(TAG,"I have just switched from seeing/not seeing iBeacons: "+state);     
    	        //TODO: register intent, UI listeners, send intent with data
	        }
        });

        try {
            iBeaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {   }
	}

	@Override
	public Context getApplicationContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unbindService(ServiceConnection paramServiceConnection) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean bindService(Intent paramIntent,
			ServiceConnection paramServiceConnection, int paramInt) {
		// TODO Auto-generated method stub
		return false;
	}

}
