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
	public static final String UI_INTENT = "UI_INTENT";
	public static final String EXTRA_DATA = "EXTRA_DATA";
	protected static final String TAG = "BeaconScanConsumer";
	private IBeaconManager iBeaconManager;
	private Context applicationContext;

	public BeaconScanConsumer(IBeaconManager iBeaconManager, Context context) {
		this.iBeaconManager = iBeaconManager;
		applicationContext = context;
	}

	@Override
	public void onIBeaconServiceConnect() {
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
	        @Override
	        public void didEnterRegion(Region region) {
	          String data = "I just saw an iBeacon for the first time!";
	          Log.d(TAG,data);
	          broadcastUpdate(UI_INTENT, data);
	        }
	
	        @Override
	        public void didExitRegion(Region region) {
	        	Log.d(TAG,"I no longer see an iBeacon");
    	        //TODO: send intent with data
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

	@Override
	public Context getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void unbindService(ServiceConnection paramServiceConnection) {
		applicationContext.unbindService(paramServiceConnection);
	}

	@Override
	public boolean bindService(Intent paramIntent,
			ServiceConnection paramServiceConnection, int paramInt) {
		return applicationContext.bindService(paramIntent, paramServiceConnection, paramInt);
	}

	private void broadcastUpdate(final String action, String extraData) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DATA, extraData);
		applicationContext.sendBroadcast(intent);
	}
	
}
