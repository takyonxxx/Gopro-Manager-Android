package com.gopromanager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;

public class GoproActivity extends Activity {
	private static final String TAG = "gopro";
	public static final String PREFS_NAME = "MyPrefsFile";	

	WifiManager wifiManager;
	TextView log,txturl;
	VideoView videoView;
	String wifiSSID;
	String wifiPass;
	WifiReceiver wifiReceiver;
	URL url;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gopro);
		videoView = (VideoView) findViewById(R.id.videoView1);		
		log = (TextView) findViewById(R.id.log);
		
		// Restore preferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); 			
		wifiSSID = preferences.getString("SSID", "n/a");
		wifiPass = preferences.getString("PW", "n/a");

		if (wifiSSID.equals("") || wifiPass.equals("")) {
			Intent i = new Intent(GoproActivity.this, MyPreferencesActivity.class);
		    startActivity(i);	  
		}
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		log.setText(wifiInfo.getSSID());
		wifiReceiver = new WifiReceiver();
		registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
			log.setText("Disconnected");
			wifiManager.setWifiEnabled(true);
		}

		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + wifiSSID + "\"";
		conf.preSharedKey = "\"" + wifiPass + "\"";
		wifiManager.addNetwork(conf);

		refreshWifi();			
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.gopro, menu);
	  return true;		
	}

	// This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	  switch (item.getItemId()) {
	  // We have only one menu option
	  case R.id.action_settings:
	    // Launch Preference activity
	    Intent i = new Intent(GoproActivity.this, MyPreferencesActivity.class);
	    startActivity(i);	   
	    break;
	  }
	  return true;
	} 
	public void onPowerClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {				
			sendcommand("http://10.5.5.9/bacpac/PW?t=" + wifiPass	+ "&p=%01");			
		} else {			
			sendcommand("http://10.5.5.9/bacpac/PW?t=" + wifiPass	+ "&p=%00");
		}
	}
	public void onCaptureClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();		
		if (on) {				
			sendcommand("http://10.5.5.9/bacpac/SH?t=" + wifiPass	+ "&p=%01");			
		} else {			
			sendcommand("http://10.5.5.9/bacpac/SH?t=" + wifiPass	+ "&p=%00");
		}
	}
	public void onToggleClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {							
			sendcommand("http://10.5.5.9/camera/PV?t=" + wifiPass	+ "&p=%02");			
			PlayVideo();
		} else {			
			videoView.stopPlayback();			
			sendcommand("http://10.5.5.9/camera/PV?t=" + wifiPass	+ "&p=%00");	
		}
	}
	private void sendcommand(final String StrUrl)
	{	
		log.setText(StrUrl);    
		 (new Thread(new Runnable() {

             @Override
             public void run() {
                 try {
                     url = new URL(StrUrl);
                     java.net.URLConnection con = url.openConnection();
                     con.connect();
                     java.io.BufferedReader in =new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()));
                     
                 } catch (MalformedURLException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 } catch (IOException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }

             }
         })).start();
	}
	private void PlayVideo()
	 {
	  try
	       {  
	        final VideoView videoView = (VideoView) findViewById(R.id.videoView1);
	        MediaController mediaController = new MediaController(this);
	        mediaController.setAnchorView(videoView);
	        Uri video=Uri.parse("http://10.5.5.9:8080/live/amba.m3u8"); 
	        videoView.setMediaController(mediaController);
	        videoView.setVideoURI(video);
	        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
	            public void onCompletion(MediaPlayer mp) {	           		
	            	 mp.stop();
	            	 PlayVideo();
	            }
	        });	
	        videoView.setOnErrorListener(new OnErrorListener() {
	            public boolean onError(MediaPlayer mp, int what, int extra) {	                              
	                return true;
	            }
	        });
	        videoView.setOnPreparedListener(new OnPreparedListener() {
	            public void onPrepared(MediaPlayer mp) {
	            	videoView.start();
	            }
	        });
	        
	            }
	       catch(Exception e)
	       {	              
	                System.out.println("Video Play Error :"+e.toString());
	                finish();
	       }   

	 }	
	protected void refreshWifi() {
		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		if (wifiInfo.getSSID() == null || !wifiInfo.getSSID().equals(wifiSSID)) {

			List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
			for (WifiConfiguration i : list) {
				if (i.SSID != null && i.SSID.equals("\"" + wifiSSID + "\"")) {
					wifiManager.disconnect();
					wifiManager.enableNetwork(i.networkId, true);
					wifiManager.reconnect();
					break;
				}
			}
		}

	}
	protected void onPause() {
		unregisterReceiver(wifiReceiver);
		super.onPause();
	}

	protected void onResume() {
		registerReceiver(wifiReceiver, new IntentFilter(
				WifiManager.NETWORK_STATE_CHANGED_ACTION));
		super.onResume();
	}

	class WifiReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			NetworkInfo wifiNetworkInfo = (NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			Log.v(TAG, "mWifiNetworkInfo: " + wifiNetworkInfo.toString());

			if (wifiNetworkInfo.getState() == State.CONNECTED) {
				log.setText("Connected to: "
						+ wifiManager.getConnectionInfo().getSSID());
				if (wifiManager != null
						&& wifiManager.getConnectionInfo() != null
						&& wifiManager.getConnectionInfo().getSSID() != null
						&& !wifiManager.getConnectionInfo().getSSID()
								.equals(wifiSSID)) {
					Log.v(TAG, wifiManager.getConnectionInfo().getSSID());

					refreshWifi();					
				}
			} else if (wifiNetworkInfo.getState() == State.CONNECTING) {
				log.setText("Connecting...");
			} else if (wifiNetworkInfo.getState() == State.DISCONNECTING) {
				log.setText("Disconnecting...");
			} else if (wifiNetworkInfo.getState() == State.DISCONNECTED) {
				log.setText("Disconnected");
			}

		}
	}
}
/*Turn on camera : http://<ip>/bacpac/PW?t=<password>&p=%01
Turn off camera : http://<ip>/bacpac/PW?t=<password>&p=%00
Change mode    : http://<ip>/bacpac/PW?t=<password>&p=%02
 
Start capture : http://<ip>/bacpac/SH?t=<password>&p=%01
Stop capture : http://<ip>/bacpac/SH?t=<password>&p=%00
 
Preview
On : http://<ip>/camera/PV?t=<password>&p=%02
Off : http://<ip>/camera/PV?t=<password>&p=%00
 
Mode
Camera     : http://<ip>/camera/CM?t=<password>&p=%00
Photo        : http://<ip>/camera/CM?t=<password>&p=%01
Burst         : http://<ip>/camera/CM?t=<password>&p=%02
Timelapse : http://<ip>/camera/CM?t=<password>&p=%03
Timelapse : http://<ip>/camera/CM?t=<password>&p=%04
 
Orientation
Head up     : http://<ip>/camera/UP?t=<password>&p=%00
Head down : http://<ip>/camera/UP?t=<password>&p=%01
 
Video Resolution
WVGA-60  : http://<ip>/camera/VR?t=<password>&p=%00
WVGA-120  : http://<ip>/camera/VR?t=<password>&p=%01
720-30   : http://<ip>/camera/VR?t=<password>&p=%02
720-60   : http://<ip>/camera/VR?t=<password>&p=%03
960-30   : http://<ip>/camera/VR?t=<password>&p=%04
960-60   : http://<ip>/camera/VR?t=<password>&p=%05
1080-30 : http://<ip>/camera/VR?t=<password>&p=%06
 
FOV
wide : http://<ip>/camera/FV?t=<password>&p=%00
medium : http://<ip>/camera/FV?t=<password>&p=%01
narrow : http://<ip>/camera/FV?t=<password>&p=%02
 
Photo Resolution
11mp wide     : http://<ip>/camera/PR?t=<password>&p=%00
8mp medium  : http://<ip>/camera/PR?t=<password>&p=%01
5mp wide       : http://<ip>/camera/PR?t=<password>&p=%02
5mp medium  : http://<ip>/camera/PR?t=<password>&p=%03
 
Timer
0,5sec : http://<ip>/camera/TI?t=<password>&p=%00
1sec    : http://<ip>/camera/TI?t=<password>&p=%01
2sec    : http://<ip>/camera/TI?t=<password>&p=%02
5sec    : http://<ip>/camera/TI?t=<password>&p=%03
10sec  : http://<ip>/camera/TI?t=<password>&p=%04
30sec  : http://<ip>/camera/TI?t=<password>&p=%05
60sec  : http://<ip>/camera/TI?t=<password>&p=%06
 
Localisation
On : http://<ip>/camera/LL?t=<password>&p=%01
Off : http://<ip>/camera/LL?t=<password>&p=%00
 
Bip Volume
0%     : http://<ip>/camera/BS?t=<password>&p=%00
70%   : http://<ip>/camera/BS?t=<password>&p=%01
100% : http://<ip>/camera/BS?t=<password>&p=%02*/
