package lbs.realserver.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class SystemMetricsThread extends Thread
{
	private SystemMetricsThread()
	{
	}

	public static SystemMetricsThread getInstance()
	{
		if (InstanceHolder.INSTANCE == null)
		{
			InstanceHolder.INSTANCE = new SystemMetricsThread();
			InstanceHolder.INSTANCE.start();
		}
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder
	{
		public static SystemMetricsThread INSTANCE;;
	}

	SystemMetrics systemMetrics = new SystemMetrics();

	@Override
	public void run()
	{
		try
		{
			collectMetrics();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void collectMetrics() throws IOException
	{
		try
		{
			// String[] commands = {"cmd", "/K","C:", "cd Windows","cd System32","typeperf
			// \"\\Processor(_Total)\\*\""};
			// "cmd /c start cmd.exe /K \"c: && cd Windows && cd System32 && typeperf
			// \"\\Processor(_Total)\\*\""
			// Process process = Runtime.getRuntime().exec("cmd /K \" c: && cd Windows && cd
			// System32 && typeperf \"\\Processor(_Total)\\*\"");

			ProcessBuilder pb = new ProcessBuilder("typeperf.exe", "-si", "5", "\"\\Processor(_Total)\\*\"",
					"\"\\Memory\\*\"", "\"\\PhysicalDisk(_Total)\\*\"");
			pb.redirectOutput();
			pb.redirectError();
			pb.directory(new File("C:/Windows/System32"));
			Process process = pb.start();
			System.out.println("Command executed");
			InputStream in = process.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			br.readLine(); // skip blank line
			String headerline = br.readLine();
			String[] headers = headerline.split("[\\\"\\,]+");
			for (String header : headers)
			{
				System.out.println(header);
			}
			while ((line = br.readLine()) != null)
			{
				Object[] f = parseValues(line.split("[\\\"\\,]+"));
				// System.out.println(Arrays.toString(f));
				String[] metric = Arrays.toString(f).split(",");

				synchronized (systemMetrics)
				{
					systemMetrics.processorTime = Float.parseFloat(metric[2]);
					systemMetrics.pageFaults = Float.parseFloat(metric[17]);
					systemMetrics.availableBytes = Double.parseDouble(metric[18]);
					systemMetrics.commitedBytes = Double.parseDouble(metric[19]);
					systemMetrics.cacheFaults = Float.parseFloat(metric[23]);
				}
			}

			in.close();

			System.out.println("\nFinished");
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");

	private static Object[] parseValues(String[] str)
	{
		Object[] d = new Object[str.length];
		for (int i = 0; i < str.length; i++)
		{
			try
			{
				if (i == 0)
					continue;
				if (i == 1)
					d[i] = sdf.parse(str[i]);
				d[i] = Float.parseFloat(str[i]);
			} catch (Exception ignore)
			{
			}
		}
		return d;
	}

}
