package src;

import inverter.Inverter;
import inverterdata.InverterData;
import inverterdata.InverterDataType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import smajava.SmaLogger;

public class SMAConnection 
{
	private boolean initialized;
	final String SMA_PASS = "0000";	//Default pass
	
	private SmaLogger smaLogger;
	private Inverter inverter;
	private CostumPrinter printer;
	
	public SMAConnection(CostumPrinter printer)
	{
		this.printer = printer;
		initialized = false;
	}
	
	public void InitSmaLogger()
	{
		smaLogger = new SmaLogger();
		
		int rc = 0;
		
		printer.PrintLine("Initializing SMA Logger");
		rc = smaLogger.Initialize(new String[]{"-q"});
		
		if(rc != 0)
		{
			printer.PrintLine("Failed to initialize SMA Logger");
			System.exit(-1);
		}
		else
		{
			printer.PrintLine("SMA Logger succesfully initialized");
		}
		inverter = smaLogger.CreateInverter("192.168.1.110");
		
		boolean succes = true;
		
		inverter.Logon(SMA_PASS);
		
		//Wake up time
		if ((rc = inverter.GetInverterData(InverterDataType.TypeLabel)) != 0)
		{
			succes = false;
		}
		
		if(succes)
		{
			printer.PrintLine("Succesfully received data from SMA inverter.");
                        
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));    
            
            //printer.PrintLine(String.format("Inverter wake-up time: %s\t Long: %d\n", formatter.format(new Date(inverter.Data.WakeupTime)), inverter.Data.WakeupTime));
            
            //printer.PrintLine(String.format("Logger starting time is: %d:%d\n", this.startingHour, this.startingMinute));
            //printer.PrintLine(String.format("Logger stopping time is: %d:%d\n", this.endingHour, this.endingMinute));
            
            this.initialized = true;
		}
		else 
		{
			this.initialized = false;
			printer.PrintLine("Failed to receive data from SMA inverter.");
			StopSmaConnection();
		}
	}
	
	public boolean IsInitialized()
	{
		return this.initialized;
	}
	
	public void StopSmaConnection()
	{
		if(initialized)
		{
			this.initialized = false;
			smaLogger.ShutDown();
			smaLogger = null;
			inverter = null;
		}
	}
	
	public boolean GetData()
	{
		boolean succes = false;
		if(inverter.Logon(SMA_PASS) == 0)
		{
			succes = true;	//Logged on...
		}
		else
		{
			succes = false;	//Failed to log in...
		}

		if(succes)
		{
			//eToday, eTotal
			if (inverter.GetInverterData(InverterDataType.EnergyProduction) != 0)
		        succes = false;
			
			if (inverter.GetInverterData(InverterDataType.SpotDCPower) != 0)
		        succes = false;

		    if (inverter.GetInverterData(InverterDataType.SpotDCVoltage) != 0)
		        succes = false;

			
			//PAC 1 - 3
			if (inverter.GetInverterData(InverterDataType.SpotACPower) != 0)
		        succes = false;
			
			//UAC 1 - 3, IAC 1 - 3
		    if (inverter.GetInverterData(InverterDataType.SpotACVoltage) != 0)
		        succes = false;

		    //PAC_TOTAL
		    if (inverter.GetInverterData(InverterDataType.SpotACTotalPower) != 0)
		        succes = false;

		    //Calculate missing AC Spot Values
		    inverter.CalcMissingSpot();
		}
		inverter.Logoff();
		return succes;
	}
	
	public InverterData GetInverterData()
	{
		return inverter.Data;
	}
}
