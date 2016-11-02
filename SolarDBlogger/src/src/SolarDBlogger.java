package src;
import inverterdata.InverterData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import smajava.misc;


public class SolarDBlogger implements Runnable
{
	final long LOG_DELAY = TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES);	//10 Minutes
	final long WAKE_DELAY = TimeUnit.MILLISECONDS.convert(30l, TimeUnit.MINUTES);	//30 Minutes
	final long RECONNECT_TIME = TimeUnit.MILLISECONDS.convert(1l, TimeUnit.HOURS);	//1 Hour
	
	private String dbName = "";
	private boolean isRunning = false;
	private Thread loggingThread;
	private CostumPrinter printer;
	private SMAConnection smaConn;
	
	private long timer;
	private long reconnectTimer = 0;
	private boolean sleeping = false;
	//private boolean wakingUp = false;
	
	public SolarDBlogger()
	{
		printer = new CostumPrinter();
		smaConn = new SMAConnection(printer);
		
		timer = 0;
	}
	
	public void Init(String[] args)
	{
		if(args.length == 0)
		{
			System.out.println("No args given, program does nothing. Use -help for info.");
			System.exit(0);
		}
		else
		{
			System.out.println("Args given:");
			for(String arg : args)
			{
				System.out.println(arg);
			}
		}
		
		//Find the path for the database first.
		for(String a : args)
		{
			if(a.equals("-help"))
			{
				ShowHelp();
			}
			else if(a.equals("-debug"))
			{
				printer.EnableDebug(true);
			}
			else if(a.substring(0, 5).equals("-path"))
			{
				dbName = a.substring(6);
			}
		}
		if(dbName != "")
		{
			printer.PrintLine("Specified database path is: " + dbName);
		}
		else
		{
			printer.PrintLine("No database path specified, see -help for info.");
			System.exit(-1);
		}
		
		for(String arg : args)
		{
			if(arg.equals("-initdb"))
			{
				
				printer.PrintLine("Creating database and deleting existing tables.");
				DBconnection.createNewDatabase(dbName);
				DBconnection.deleteTable(dbName);
				DBconnection.createNewTable(dbName);
			}
			else if(arg.equals("-start"))
			{
				StartLogging();
			}
		}
	}	
	
	private boolean GetData()
	{
		boolean result = false;
		
		//try to get data
		int counter = 5;
		while(counter > 0)
		{
			if(smaConn.GetData())
			{
				counter = 0;	
				
				//Check if it is time to sleep, keep sleeping, or time to wake up.
				if(!IsSleepTime())
				{
					/*
					//Reset the timer but not right after waking up to ensure we are at a nice 10 minute interval.
					if(!wakingUp)
					{
						timer = LOG_DELAY;
					}
					else
					{
						wakingUp = false;
					}*/
					timer = LOG_DELAY;
					//return true because we got data succesfully
					result = true;
				}
			}
			else
			{
				//If failed to get data, retry again in 10 seconds.
				//Repeat this until data is received and try up to 5 times.
				counter--;
				try 
				{
					Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
				} 
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(counter == 0)
				{
					printer.PrintLine("Unable to get data from the inverter, check the connection.");
					//Return false and reset the timer to 10 minutes.
					timer = LOG_DELAY;
					result = false;
				}
			}	
		}	
		return result;
	}
	
	private void LogDataToDB()
	{
		InverterData data = smaConn.GetInverterData();
		long timeStamp = System.currentTimeMillis();
		printer.PrintDebug(String.format("Time: \t %s\n"
				+ "eToday: \t %.3fkWh\n"
				+ "eTotal: \t %.3fkWh\n"
				+ "totalAC: \t %7.3fkW\n", new Date(timeStamp).toString(), misc.tokWh(data.EToday), misc.tokWh(data.ETotal), misc.tokW(data.TotalPac)));
		DBconnection.insert(dbName, data.TotalPac, data.EToday, data.ETotal, timeStamp);
	}
	
	private void ShowHelp()
	{
		printer.PrintDebug("=== SolarDBlogger ===");
		printer.PrintDebug("-initdb Create the database\n"
				+ "-path Specify the path for the database ex: path=C:/database/ \n"
				+ "-start Start the logging process.\n"
				+ "-debug Enables debug printing in console."
				+ "-help Show this help.\n"
				+ "-exit While running exits the program.\n");
		System.exit(0);
	}
	
	private void StartLogging()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		    public void run() 
		    {
		    	StopLogging();
		    }
		}));
		
		//Manuals shutdown hook for use in eclipse for example.
		new Thread(new Runnable() 
		{
		    public void run() 
		    {
		    	BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		    	try 
				{
		    		String line = "";
					while(!line.equals("-exit"))
					{
						line = r.readLine();					
					}
					StopLogging();
				} 
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		}).start();
		
		//smaConn.InitSmaLogger();
		WakeUp();
		loggingThread = new Thread(this);
		isRunning = true;
		loggingThread.start();
	}
	
	private void StopLogging()
	{
		printer.PrintLine("Shutting down...");
		isRunning = false;
		if(loggingThread != null)
		{
			try 
			{
				loggingThread.join();
			} catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		if(smaConn.IsInitialized())
		{
			smaConn.StopSmaConnection();
		}
		printer.PrintLine("Program shut down succesfully.");
	}
	
	private void GoToSleep()
	{
		InverterData data = smaConn.GetInverterData();		
		smaConn.StopSmaConnection();
		//Get the last wakeup time
		long wakeupTime = data.WakeupTime;
		//Add 24 hours to that so we get the next wake up time
		wakeupTime += TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
		//Add 10 minutes just to be sure we don't wake up too early.
		wakeupTime += LOG_DELAY;
		long sleepTime = wakeupTime - System.currentTimeMillis();
		timer = sleepTime;
		sleeping = true;	
		int minutes = (int) ((sleepTime / (1000*60)) % 60);
		int hours   = (int) ((sleepTime / (1000*60*60)) % 24);
		printer.PrintLine(String.format("Going to sleep for %02d hours and %02d minutes, with a production of %7.3fkW...", hours, minutes, misc.tokW(data.TotalPac)));
	}
	
	private void WakeUp()
	{		
		if(!smaConn.IsInitialized())
		{
			smaConn.InitSmaLogger();
		}
		if(smaConn.IsInitialized())
		{
			smaConn.GetData();
		}
		InverterData data = smaConn.GetInverterData();
		printer.PrintLine(String.format("...Waking up with a production of: %7.3fkW", misc.tokW(data.TotalPac)));
		sleeping = false;
		//wakingUp = true;
		//Make sure the timer begins at the right interval
		timer = LOG_DELAY;
		long adjustmentTime = System.currentTimeMillis() % LOG_DELAY;
		timer -= adjustmentTime;	
		reconnectTimer = RECONNECT_TIME;
	}
	
	private boolean IsSleepTime()
	{
		boolean isSleepTime = true;
		
		InverterData data = smaConn.GetInverterData();
		
		if(data.TotalPac > 0)
		{
			//If sleeping and the production is larger then 0, wake up.
			if(sleeping)
			{
				WakeUp();	
			}
			isSleepTime = false;
		}
		else	//Production less then 0.
		{		
			if(!sleeping)
			{
				//If not sleeping and the production is less then 0, go to sleep for 8 hours.
				GoToSleep();
			}
			else
			{
				//If we have slept for 8 hours and the production is still less then 0, go into the wakeup cyclus (30 minutes).
				timer = WAKE_DELAY;
			}
		}
		return isSleepTime;
	}
	
	@Override
	public void run() 
	{			
		long beginTime = 0;
		long elapsedTime = 0;
		
		//log solar values to database every x minutes
		while(isRunning)
		{
			beginTime = System.currentTimeMillis();
			
			//Check if the timer has waited the set amount of time.
			if(timer <= 0)
			{		
				//Make sure we have a working connection, else it should reset itself after an hour.
				if(smaConn.IsInitialized())
				{
					if(GetData())
					{				
						if(!sleeping)
						{
							//Insert in database
							LogDataToDB();
						}
					}
				}
				//After an hour reset the SMA connection, as it seems to be disconnecting after 2 hours.
				if(reconnectTimer <= 0)
				{
					printer.PrintLine("Resetting connection...");
					//Stop connection
					smaConn.StopSmaConnection();
					//Wait a bit...
					try 
					{
						Thread.sleep(5000);
					} 
					catch (InterruptedException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//reconnect
					smaConn.InitSmaLogger();
					reconnectTimer = RECONNECT_TIME;
				}
			}	
			//Sleep for a second
			try 
			{
				Thread.sleep(1000);
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Measure how long all this took and add that to the timer.
			elapsedTime = System.currentTimeMillis() - beginTime;
			timer -= elapsedTime;
			reconnectTimer -= elapsedTime;
		}
	}
	
	public static void main(String[] args) 
	{
		SolarDBlogger solarLogger = new SolarDBlogger();
		solarLogger.Init(args);
	}
}
