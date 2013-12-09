package com.lkl.activitydetectionreporttool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

class Users
{
	private HttpGet uri;
	private int port;
	private int timeout;
	private boolean success;
	private ArrayList<User> users=new ArrayList<User>();
	
	public Users(String uri, int port, int timeout)
	{
		this.uri=new HttpGet(uri);
		this.port=port;
		this.timeout=timeout;
		this.success=true;
	}
	
	public User getUser(int position) throws Exception
	{
		if(position>=size())
		{
			throw new Exception("Out of Bounds");
		}
		
		return users.get(position);
	}
	
	public boolean getSuccess()
	{
		return success;
	}
	
	public int size()
	{
		return users.size();
	}
	
	private static String convertStreamToString(InputStream is) throws Exception
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;

	    while((line=reader.readLine())!=null)
	    {
	        sb.append(line);
	    }

	    is.close();

	    return sb.toString();
	}
	
	public void populate()
	{	
		try
		{
			BasicHttpParams httpParams = new BasicHttpParams();
			ConnManagerParams.setTimeout(httpParams, timeout);
			SchemeRegistry schemeRegistry=new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), port));
			ThreadSafeClientConnManager cm=new ThreadSafeClientConnManager(httpParams, schemeRegistry);
			DefaultHttpClient client = new DefaultHttpClient(cm,httpParams);
			HttpResponse response = client.execute(uri);
	
			StatusLine status = response.getStatusLine();
			
			if (status.getStatusCode()!=200)
			{
			    Log.v("Error", "HTTP error, invalid server status code: " + response.getStatusLine());  
			    success=false;
			}
	
			InputStream is = response.getEntity().getContent();
			String json=convertStreamToString(is);
			Log.v("normal", "** Json:"+json);
			getUsers(json);
		}
		catch (IOException e)
		{
			Log.v("Error",e.toString());
			success=false;
		}
		catch (Exception e)
		{
			Log.v("Error",e.toString());
			success=false;
		}
	}
		
	public void display()
	{
		for(User user : users)
		{
			System.out.println(user.toString());
		}
	}
	
	public String[] toStringArray()
	{
		String[] array=new String[size()];
		
		for(int i=size()-1;i>=0;i--)
		{
			User user=((User)users.get(i));
			array[i]=user.STUDENT;
		}
		
		return array;
	}
	
	private void getUsers(String json)
	{
		try
		{
			JSONArray array = new JSONArray(json);
		    
		    for(int i=0; i<array.length(); i++)
		    {
		        JSONObject object=array.getJSONObject(i);
		        String student=object.getString("STUDENT");
		        String event=object.getString("EVENT");
		        users.add(new User(student,event));
		    }
		}
	    catch (JSONException e)
	    {
			Log.v("Error",e.toString());
	    }
	    catch (Exception e)
	    {
			Log.v("Error",e.toString());
	    }
	}
	
	public static class User
	{
		public User(String student,	String event)
		{
			this.STUDENT=student;
			this.EVENT=event;		
		}
		
		public void prettyPrint()
		{
			String s="";
			s+="student: "+STUDENT+"\n";
			s+="event: "+EVENT;
			System.out.println(s);
		}

		public String toString()
		{
			return STUDENT+","+EVENT;
		}
				
		public String getUser()
		{
			return STUDENT;
		}

		public String getEvent()
		{
			return EVENT;
		}
		
		public static Timestamp getEvent(String event) throws ParseException
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
			Date parsedDate = dateFormat.parse(event);
			return new Timestamp(parsedDate.getTime());
		}

		private String STUDENT;
		private String EVENT;
	}
}
