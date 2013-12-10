package com.lkl.activitydetectionreporttool;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.lkl.activitydetectionreporttool.R;
import com.lkl.activitydetectionreporttool.Users.User;

public class MainActivity extends Activity
{
	Users users;
	ListView lstUserDetails;
	ToggleButton btnActivity;
	Handler handler;
	Thread background;
	ProgressBar pbarRefreshData;
	boolean backThreadRunning=false;
	boolean networkReady=true;
	public final String HOST="193.61.44.50";
	public final String URI="http://"+HOST+"/com.lkl.eclipsedata/rest/service";
	public final int PORT=8282;
	public final int TIMEOUT=2000;		//wait time for background net check thread
	public final int INTERVAL=5000;		//wait time for background data loader thread

	private void showProgress(boolean flag)
	{
	    int visibility=pbarRefreshData.getVisibility();
		
		if(flag==true)
		{
			if(visibility==ProgressBar.INVISIBLE)
			{
			    pbarRefreshData.setVisibility(ProgressBar.VISIBLE);
			}
		}
		else
		{
			if(visibility==ProgressBar.VISIBLE)
			{
			    pbarRefreshData.setVisibility(ProgressBar.INVISIBLE);
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.v("normal","** onCreate (Activity Lifecycle)");

		//set up the GUI
		setContentView(R.layout.activity_main);
		
		//get references for the GUI controls needed
		lstUserDetails=(ListView)findViewById(R.id.lstUserDetails);
		btnActivity=(ToggleButton)findViewById(R.id.btnActivity);
		pbarRefreshData=(ProgressBar)findViewById(R.id.pbarRefreshData);

		OnItemClickListener mMessageClickedHandler=new OnItemClickListener()
		{
		    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		    {
		    	//String selectedFromList =(String)(lstUserDetails.getItemAtPosition(position));
		    	if(users!=null)
		    	{
		    		User user;
		    		
		    		try
		    		{
		    			user=users.getUser(position);
			    		String event=user.getEvent();
				    	Toast.makeText(getApplicationContext(),event, Toast.LENGTH_SHORT).show();
		    		}
		    		catch(Exception e)
		    		{
						Log.v("Error",e.toString());
		    		}		    		
		    	}
		    }
		};

		//attach the listener to the list control
		lstUserDetails.setOnItemClickListener(mMessageClickedHandler);
		
		//create a handler for the main activity
		handler=new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if(msg.arg1==0)	//network check
				{
					Bundle bundle=msg.getData();
					String netstatus=bundle.getString("network");

					if(netstatus.equals("start"))
					{
						showProgress(true);
						return;
					}
					
					if(netstatus.equals("false"))
					{
						String message="network is down or host is unavailable";
				    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
				    	networkReady=false;
					}
					else
					{
						String message="network and host are available";
				    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
				    	networkReady=true;
					}
					
					showProgress(false);
				}
				else			//data load
				{
					if(msg.arg2==1)
					{
						Bundle bundle=msg.getData();
						String status=bundle.getString("data");
						
						if(status.equals("start"))
						{
							showProgress(true);
						}						
					}
					else
					{
						users=(Users)msg.obj;
						
						if(users.size()!=0)
						{
							ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, users.toStringArray());
							lstUserDetails=(ListView)findViewById(R.id.lstUserDetails);
							lstUserDetails.setAdapter(adapter);
						}
						
						showProgress(false);
						
						if(users.getSuccess()==false)
						{
							String message="Network problem or host unavailable - data cannot be retrieved";
							Log.v("normal",message);
					    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
							btnActivity.setChecked(false);
							networkReady=false;
						}
					}
				}
			}
		};

		//create the background thread
		background=new BackgroundThread("TMain",handler,this,INTERVAL);
		
		//create a listener for button events
		btnActivity.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{			
				boolean checked=buttonView.isChecked();
				
				if(background==null)
				{
					return;
				}
				
				if(checked==true)
				{
					if(networkReady==false)
					{
						btnActivity.setChecked(false);
						String message="Press [Network Check] to check availability";
						Log.v("normal",message);
				    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
						return;
					}
					
					((BackgroundThread)background).resumeThread();
					backThreadRunning=((BackgroundThread)background).isRunning();
				}
				else
				{
					try
					{
						((BackgroundThread)background).pauseThread();
					}
					catch (InterruptedException e)
					{
						Log.v("Error",e.toString());
			        }

					backThreadRunning=((BackgroundThread)background).isRunning();
				}
			}
		});
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();  // Always call the superclass method first

		Log.v("normal","** onRestart (Activity Lifecycle)");

		//create a new background thread
		background=new BackgroundThread("TMain",handler,this,INTERVAL);
		backThreadRunning=((BackgroundThread)background).isRunning();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();  // Always call the superclass
	    
		Log.v("normal","** onDestroy (Activity Lifecycle)");

		// Stop method tracing that the activity started during onCreate()
		android.os.Debug.stopMethodTracing();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();  // Always call the superclass method first

		Log.v("normal","** onPause (Activity Lifecycle)");

		//pause the background thread and update the button
		backThreadRunning=((BackgroundThread)background).isRunning();

		if(backThreadRunning==true)
		{
			btnActivity.setChecked(false);
		}
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();  // Always call the superclass method first

		Log.v("normal","** onStop (Activity Lifecycle)");

		if(background!=null&&background.isAlive())
		{
			((BackgroundThread)background).terminateThread();
			btnActivity.setChecked(false);
		}		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();  // Always call the superclass method first

		Log.v("normal","** onResume (Activity Lifecycle)");

		if(backThreadRunning==true)
		{
			if(((BackgroundThread)background).isRunning()==false)
			{
				if(networkReady==false)
				{
					btnActivity.setChecked(false);
					return;
				}
				
				btnActivity.setChecked(true);
			}
		}
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		Log.v("normal","** onStart (Activity Lifecycle)");

		//check network and server availability
		new NetworkCheck("NetChecker", this, handler, HOST, PORT, TIMEOUT).start();
		
		//start the background thread
		background.start();	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void netCheck(View v)
	{
		//check network and server availability
		new NetworkCheck("NetChecker", this, handler, HOST, PORT, TIMEOUT).start();
	}
/*	
	public boolean isOnline()
	{
		pbarRefreshData.setVisibility(ProgressBar.VISIBLE);
		ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo=cm.getActiveNetworkInfo();
	    boolean online=netInfo!=null&&netInfo.isAvailable()&&netInfo.isConnected();
	    
	    if(online==false)
	    {
			String message="(Main activity) the device is not connected to a network - data cannot be retrieved";
			Log.v("normal",message);
	    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
	    }
	    else
	    {
	    	online=isHostAvailable();
	    }
	    
	    return online;
	}
	
	public boolean isHostAvailable()
	{
		boolean exists=false;

		try
		{
			SocketAddress sa=new InetSocketAddress(HOST,PORT);

            // Create an unbound socket
            Socket socket=new Socket();

            // This method will block no more than timeoutMs.
            // If the timeout occurs, SocketTimeoutException is thrown.
            int timeoutMs = TIMEOUT;   // 2 seconds
            socket.connect(sa, timeoutMs);
            exists=true;
		}
		catch(Exception e)
		{
			Log.v("Error",e.toString());
		}
		
	    if(exists==false)
	    {
			String message="(Main activity) the host is not available - data cannot be retrieved";
			Log.v("normal",message);
	    	Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
	    }

	    pbarRefreshData.setVisibility(ProgressBar.INVISIBLE);
		
		return exists;
	}
	*/
}
