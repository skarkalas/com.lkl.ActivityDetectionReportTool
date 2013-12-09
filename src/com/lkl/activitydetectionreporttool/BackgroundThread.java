package com.lkl.activitydetectionreporttool;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BackgroundThread extends Thread
{
	private Activity activity;
	private Handler handler;
	private boolean running = false;
	private boolean stop = false;
	private int interval;
	private int INTERVAL=5000;

	public BackgroundThread(String str,Handler handler,Activity activity,int interval)
	{
        super(str);
        this.activity=activity;
        setHandler(handler);
        setInterval(interval);
		Log.v("normal","Thread "+this.getName()+" is created");
	}
	
	public boolean isRunning()
	{
		return running;
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

	public void setInterval(int interval)
	{
		if(interval<=0)
		{
			this.interval=INTERVAL;
		}
		else
		{
			this.interval=interval;
		}		
	}
	
	@Override
	public void run()
	{
		Log.v("normal","Thread "+this.getName()+" is alive");
		
		while(stop==false)
		{
			try
			{						
				synchronized(this)
				{
					while(running==false)
					{
						wait();
					}
				}

				Log.v("normal","Thread "+this.getName()+" does some work");
				doWork();
				Log.v("normal","Thread "+this.getName()+" is put to sleep for "+interval+"ms");
				Thread.sleep(interval);
	        }
			catch (InterruptedException e)
			{
				Log.v("Error",e.toString());
	        }
			catch(Exception e)
			{
				Log.v("Error",e.toString());
			}

			Log.v("normal","Thread "+this.getName()+" woke up and is working again");
		}

		Log.v("normal","Thread "+this.getName()+" terminates");
	}
		
	private void doWork()
	{
/*		if(((MainActivity)activity).isOnline()==false)
		{
			String message="(do work) the device is not connected to a network - data cannot be retrieved";
			Log.v("normal","Thread "+this.getName()+message);
	    	return;
		}
*/
		sendMessage("start");
		MainActivity mainActivity=(MainActivity)activity;
		String uri=mainActivity.URI;
		int port=mainActivity.PORT;
		int timeout=mainActivity.TIMEOUT;
		
		Users users=new Users(uri,port,timeout);
		users.populate();
		sendMessage(users);
	}
	
	private void sendMessage(String txt)
	{
		Bundle bundle=new Bundle();
		bundle.putString("data", txt);
		Message message=new Message();
		message.arg1=1;
		message.arg2=1;
		message.setData(bundle);
		handler.sendMessage(message);		
	}
	
	private void sendMessage(Users users)
	{
		Message message=new Message();
		message.arg1=1;
		message.arg2=0;
		message.obj=users;
		handler.sendMessage(message);		
	}
	
	public void pauseThread() throws InterruptedException
	{
		Log.v("normal","Thread "+this.getName()+" is paused by the user");
		running = false;
	}

	public synchronized void resumeThread()
	{
		Log.v("normal","Thread "+this.getName()+" is resumed by the user");
		running = true;
		notify();
	}

	public void terminateThread()
	{
		Log.v("normal","Thread "+this.getName()+" is terminated by the user");
		stop = true;
	}
}
