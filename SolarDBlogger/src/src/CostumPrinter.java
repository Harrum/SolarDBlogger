package src;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CostumPrinter 
{
	private final String FILE_NAME = "SolarLOG.txt";
	
	private DateFormat dateFormat;
	private File logFile;
	private boolean debug = false;
	private BufferedWriter writer;
	
	public CostumPrinter()
	{
		String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		logFile = new File(path + "\\" + FILE_NAME);
		if(!logFile.exists())
		{
			try 
			{
				logFile.createNewFile();
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
		try 
		{
			writer = new BufferedWriter(new FileWriter(logFile, true));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			System.exit(-1);
		}
		dateFormat = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss z");
		dateFormat.setTimeZone(TimeZone.getTimeZone("CEST"));
	}
	
	public void Exit()
	{
		try 
		{
			writer.close();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void EnableDebug(boolean enable)
	{
		debug = enable;
	}
	
	public void PrintDebug(String line)
	{
		if(debug)
		{
			System.out.println(line);
		}
	}
	
	public void PrintLine(String line)
	{
		if(debug)
		{
			System.out.println(line);
		}
		else
		{
			Date d = new Date();
			String logLine = String.format("%s  -  %s", dateFormat.format(d), line);
			try 
			{
				writer.append(logLine);
				writer.newLine();
				writer.flush();
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
