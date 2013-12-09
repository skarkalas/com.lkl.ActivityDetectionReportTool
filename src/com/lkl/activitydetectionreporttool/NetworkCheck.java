package com.lkl.activitydetectionreporttool;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NetworkCheck extends Thread
{
	private Activity activity;
	private Handler handler;
	private String host;
	private int port;
	private int timeout;
	private final int TIMEOUT=3000;

	public NetworkCheck(String name, Activity activity, Handler handler, String host, int port, int timeout)
	{
        super(name);
        this.activity=activity;
        this.host=host;
        this.port=port;
        setHandler(handler);
        setTimeout(timeout);
	}
	
	public void setTimeout(int timeout)
	{
		if(timeout<=0)
		{
			this.timeout=TIMEOUT;
		}
		else
		{
			this.timeout=timeout;
		}		
	}
	
	@Override
	public void run()
	{
		sendMessage("start");
		boolean status=isOnline();
		sendMessage(Boolean.toString(status));
	}
	
	private void sendMessage(String txt)
	{
		Bundle bundle=new Bundle();
		bundle.putString("network", txt);
		Message message=new Message();
		message.arg1=0;
		message.setData(bundle);
		handler.sendMessage(message);		
	}
	
	public void setHandler(Handler handler)
	{
		if(handler==null)
		{
			this.handler=activity.getWindow().getDecorView().getHandler(); 
		}
		else
		{
			this.handler=handler;
		}
	}
	
	public boolean isOnline()
	{
		ConnectivityManager cm=(ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo=cm.getActiveNetworkInfo();
	    boolean online=netInfo!=null&&netInfo.isAvailable()&&netInfo.isConnected();
	    
	    if(online==false)
	    {
			String message="Thread "+this.getName()+" the device is not connected to a network - data cannot be retrieved";
			Log.v("normal",message);
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
			SocketAddress sa=new InetSocketAddress(host,port);

            // Create an unbound socket
            Socket socket=new Socket();

            // This method will block no more than timeoutMs.
            // If the timeout occurs, SocketTimeoutException is thrown.
            int timeoutMs = timeout;
            socket.connect(sa, timeoutMs);
            exists=socket.isConnected();
		}
		catch(SocketTimeoutException e)
		{
			Log.v("Error",e.toString());
		}
		catch(Exception e)
		{
			Log.v("Error",e.toString());
		}
		
	    if(exists==false)
	    {
			String message="Thread "+this.getName()+" the host is not available - data cannot be retrieved";
			Log.v("normal",message);
	    }
		
		return exists;
	}
}
