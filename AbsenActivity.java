package com.nfc.ta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class AbsenActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	
	private NfcAdapter mNfcAdapter;
	private static final int MESSAGE_SENT = 1;
	static final int DIALOG_ERROR_CONNECTION = 2;
	public String jsonResult;
	String serverurl;
	
	TextView nrpMhsTextView, mkTextView, klsTextView, DsnTextView;
	String user_name_bundle, nama_dosen_bundle, kelas_bundle, nama_mk_bundle ,send_msg_nfc;
	String user_name_encode, mk_encode, kelas_encode, nama_dosen_encode;
	
	/**
	 * Create AbsenActivity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_absen);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		nrpMhsTextView = (TextView)this.findViewById(R.id.nrp_mhs);
		mkTextView = (TextView)this.findViewById(R.id.mk_nfc);
		klsTextView = (TextView)this.findViewById(R.id.kls_nfc);
		DsnTextView = (TextView)this.findViewById(R.id.dsn_nfc);
		SharedPreferences Pref  = this.getSharedPreferences( "ServerData", Context.MODE_PRIVATE);
        serverurl = Pref.getString("server",null);
		
		if (!isOnline(AbsenActivity.this)) 
        {
        	showDialog(DIALOG_ERROR_CONNECTION); //displaying the created dialog.
        } 
        else 
        {
        	/**
    		 * Get value from MahasiswaActivity
    		 */
    		Bundle extras = getIntent().getExtras();
    		user_name_bundle = extras.getString("UserName");
    		nama_mk_bundle = extras.getString("Nama_MK");
    		kelas_bundle = extras.getString("Kelas");
    		nama_dosen_bundle = extras.getString("Nama_Dosen");
    		
    		nrpMhsTextView.setText(user_name_bundle);
    		klsTextView.setText(kelas_bundle);
    		
        
    		if (!isOnline(AbsenActivity.this)) {
            	showDialog(DIALOG_ERROR_CONNECTION); //displaying the created dialog.
            }
    		else {
    			/**
    			 * Checking NFC Connectivity and Availability
    			 */
    			
    			if (mNfcAdapter == null) {
    				Toast.makeText(this, "NFC is not available on your device", Toast.LENGTH_LONG).show();
    				finish();
    				return;
    			}
    			
    			else if (mNfcAdapter.isEnabled() == false) {
    				Toast.makeText(this, "NFC not activated yet", Toast.LENGTH_LONG).show();
    				finish();
    				new Handler().postDelayed(new Runnable() {
    					
    					@Override
    					public void run() {
    						startActivityForResult(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS), 0);
    						return;
    					}
    				}, 1500);
    			}
    			/**
    			 * Execute NFC Callback
    			 * -- setNdefPushMessageCallback(this, this); -> Execute createNdefMessage(NfcEvent arg0)
    			 * -- setOnNdefPushCompleteCallback(this, this); -> Execute onNdefPushComplete(NfcEvent arg0)
    			 */
    			
    			else {
    				AbsenAutoTask task = new AbsenAutoTask();
        			try {
        				user_name_encode = URLEncoder.encode(user_name_bundle.toString(),"UTF-8");
                		kelas_encode = URLEncoder.encode(kelas_bundle.toString(),"UTF-8");
                		nama_dosen_encode = URLEncoder.encode(nama_dosen_bundle.toString(),"UTF-8");
                		mk_encode = URLEncoder.encode(nama_mk_bundle.toString(), "UTF-8");
        				String url = "http://"+serverurl+"/Login.php?function=PresensiNfc&nrp=" + user_name_encode + "&kls=" + kelas_encode + "&dosen=" + nama_dosen_encode + "&mk=" + mk_encode + "";
            			task.execute(url);
        			} catch (UnsupportedEncodingException e) {
        				e.printStackTrace();
        			}
    				mNfcAdapter.setNdefPushMessageCallback(this, this);
    				mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
    			}  			
    		}
    		
    		
    	}
	}
	
	/**
	 * Execute after Message Sent
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Toast.makeText(getApplicationContext(), "Message sent !", Toast.LENGTH_LONG).show();
				break;
			}
		}
	};

	/**
	 * Sending Message through NFC to the NFC target
	 */
	@Override
	public void onNdefPushComplete(NfcEvent arg0) {
		mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
	}

	/**
	 * Create NDEF Message
	 */
	@Override
	public NdefMessage createNdefMessage(NfcEvent arg0) {
		send_msg_nfc = nrpMhsTextView.getText().toString() + ":" + mkTextView.getText().toString() + ":" + klsTextView.getText().toString() + ":" + DsnTextView.getText().toString() + ":" + "Hadir";
		NdefMessage msg = new NdefMessage(
				new NdefRecord[] { createMime(
						"application/com.nfc.ta", send_msg_nfc.getBytes())
				});
		return msg;
	}

	/**
	 * Encapsulating the MIME type or URI and the payload into an intent.
	 */
	private NdefRecord createMime(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}
	
	/**
     * Class for execute Http URL
     */
	private class AbsenAutoTask extends AsyncTask<String, Integer, String> {
	   	ProgressDialog prDialog;
	   	
	   	@Override
	   	protected void onPreExecute() {
	    	prDialog=ProgressDialog.show(AbsenActivity.this, "", "Loading. Please wait...");
	   		prDialog.setCancelable(true);
	   		prDialog.setOnCancelListener(new OnCancelListener() {
					
					public void onCancel(DialogInterface dialog) {
						prDialog.dismiss();
						finish();
					}
				});
	   	};
	   	
	   	@Override
	       protected String doInBackground(String... urls) {
	   		
	   		HttpGet httpGet = new HttpGet(urls[0]);
	       	HttpParams httpParameters = new BasicHttpParams();

	       	int timeoutSocket = 10000;
	       	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
	       	
	       	DefaultHttpClient client = new DefaultHttpClient(httpParameters);
	       	
	       	StringBuilder builder = new StringBuilder();
	           Log.i("urls[0]", urls[0]);
	           try {
	             HttpResponse response = client.execute(httpGet);
	             StatusLine statusLine = response.getStatusLine();
	             int statusCode = statusLine.getStatusCode();
	             if (statusCode == 200) {
	               HttpEntity entity = response.getEntity();
	               InputStream content = entity.getContent();
	               BufferedReader reader = new BufferedReader(new InputStreamReader(content));
	               String line;
	               while ((line = reader.readLine()) != null) {
	                 builder.append(line);
	               }
	             } 
	             else {
	               Log.e("failed", "Failed to download file");
	             }
	           } catch (ClientProtocolException e) {
	             e.printStackTrace();
	           } catch (SocketTimeoutException e) {
	             e.printStackTrace();
	           } catch (ConnectTimeoutException e) {
				 e.printStackTrace();
			   } catch (IllegalStateException e) {
				   e.printStackTrace();
			   } catch (IOException e) {
				   e.printStackTrace();
			   }
	           return builder.toString();
	       }

	   	@Override
	       protected void onProgressUpdate(Integer... progress) {
	           setProgress(progress[0]);
	       }

	   	@Override
	       protected void onPostExecute(String result) {
	   		if (!isOnline(AbsenActivity.this)) 
	        {
	        	showDialog(DIALOG_ERROR_CONNECTION); //displaying the created dialog.
	        } 
	        else 
	        {
	        	prDialog.dismiss();
	        	jsonResult = result;
	        	showName();
	        }
	       }
	   }
	
	/**
     * Get Data from JSONObject for comparing values / Sending JSONObject values to another Activity
     */
    public void showName() {
    	Log.d("aa", jsonResult);
    	int i;
    	
    	JSONArray jsonArray;
    	
    	try {
    		jsonArray = new JSONArray(jsonResult);
    		
    		for (i = 0; i < jsonArray.length(); i++) {
    			Log.d("aa loop "  , String.valueOf(i));
    			
    			DsnTextView.setText(String.valueOf(jsonArray.getJSONObject(i).getString("NIP")));
        		mkTextView.setText(String.valueOf(jsonArray.getJSONObject(i).getString("Kode_MK")));
    		
    		}
    		
    	} catch (JSONException e) {
    		e.printStackTrace();
    		Log.d("catch error ", e.getMessage());
    	}
    }
	
	
	/**
     * Checking Connectivity
     */
	public boolean isOnline(Context c) {
    	ConnectivityManager cm = (ConnectivityManager) c
    			.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	
    	if (ni != null && ni.isConnected()) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
	
	/**
     * Create Dialog Message
     */
	public Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	switch (id) {
    	case DIALOG_ERROR_CONNECTION:
    		AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
    		errorDialog.setIcon(android.R.drawable.ic_dialog_alert);
    		errorDialog.setTitle("Error");
    		errorDialog.setMessage("No Internet Connection !");
    		
    		errorDialog.setNeutralButton("OK",
    				new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							finish();
							new Handler().postDelayed(new Runnable() {
								
								@Override
								public void run() {
									startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
									return;
								}
							}, 1000);
						}
					});
    		
    		AlertDialog errorAlert = errorDialog.create();
    		return errorAlert;
    		
    		default:
    			break;
    	}
    	return dialog;
    }
}
