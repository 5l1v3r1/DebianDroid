package net.debian.debiandroid;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import net.debian.debiandroid.apiLayer.ApiTools;
import net.debian.debiandroid.utils.NetUtils;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class DDNotifyService extends Service {

	public static int updateIntervalTime = 600000; // 600000 ms = 10 minutes
	
	public static boolean isForeground;
	
	private static Timer timer; 
	private final static int LOST_CONNECTION = 0, CONNECTION_RESTORED = 1;
	private final ddSHandler ddHandler = new ddSHandler(this);
	private static class ddSHandler extends Handler {
		private final WeakReference<DDNotifyService> serviceRef;
		
		public ddSHandler(DDNotifyService reference) {
			serviceRef = new WeakReference<DDNotifyService>(reference);
		}
		
		@Override
	    public void handleMessage(Message msg) {
			DDNotifyService service = serviceRef.get();
			if(service!=null) {
				switch (msg.what) {
				case LOST_CONNECTION:
					if(isForeground)
						Toast.makeText(service, service.getString(R.string.lost_connectivity), Toast.LENGTH_SHORT).show();
					break;
				case CONNECTION_RESTORED:
					if(isForeground)
						Toast.makeText(service, service.getString(R.string.connection_restored), Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
				}
			}
			super.handleMessage(msg);
		}
	};

	@Override
	  public void onCreate() {
			timer = new Timer();
		    timer.scheduleAtFixedRate(new CheckConnectivityTask(), 1000, updateIntervalTime);
		    registerReceiver(wifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
	  }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		timer.cancel();
		timer = null;
		unregisterReceiver(wifiStateChangedReceiver);
	}

	private class CheckConnectivityTask extends TimerTask {
		public void run() {
			checkConnectivity();
		}
	}
	
	private void checkConnectivity() {
		// Check if wifi not enabled and 3g not allowed
		// if wifi and 3g not enabled
		// notify users in both cases and if no3g allowed deactivate requests
		Context context = getApplicationContext();
		boolean hasConnectivity = NetUtils.hasConnectivity(context);
		
		if(!hasConnectivity) {
			if(ApiTools.isNetEnabled()) {
				// Notify user
				ddHandler.sendEmptyMessage(LOST_CONNECTION);
				ApiTools.disableUseOfNet();
			}
		} else {
			boolean hasMobileCon = NetUtils.hasNetProviderConnection(context, NetUtils.MOBILE);
			boolean mobileAllowed = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use3g", true);
			if(hasMobileCon && !mobileAllowed) {
				if(ApiTools.isNetEnabled()) {
					// Notify user
					ddHandler.sendEmptyMessage(LOST_CONNECTION);
					ApiTools.disableUseOfNet();
				}
			} else {
				if(!ApiTools.isNetEnabled()) {
					ddHandler.sendEmptyMessage(CONNECTION_RESTORED);
					ApiTools.enableUseOfNet();
				}
			}
		}
		
	}
	
	private BroadcastReceiver wifiStateChangedReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

	        switch (extraWifiState) {
		        case WifiManager.WIFI_STATE_DISABLED:
		        	checkConnectivity(); break;
		        case WifiManager.WIFI_STATE_DISABLING:
		        	break;
		        case WifiManager.WIFI_STATE_ENABLED:
		        	checkConnectivity(); break;
		        case WifiManager.WIFI_STATE_ENABLING:
		            break;
		        case WifiManager.WIFI_STATE_UNKNOWN:
		            break;
	        }
	    }
	};
	
	public static synchronized void activityResumed() {
		isForeground = true;
	}
	
	public static synchronized void activityPaused() {
		isForeground = false;
	}
}