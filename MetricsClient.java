package lbs.loadbalancer.metrics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lbs.realserver.metrics.SystemMetrics;

public class MetricsClient extends Thread
{
	PrintWriter output;

	private class ClientThread extends Thread
	{
		String ip;
		InetAddress addr;

		ClientThread(String ip) throws Exception
		{
			this.ip = ip;
			addr=InetAddress.getByName(ip);
		}

		@Override
		public void run()
		{
			try
			{
				System.out.println("Starting metrics client for "+ip);
				Socket s = new Socket(ip, 2000);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.flush();
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				while (true)
				{
					oos.writeUTF("GET_METRICS");
					oos.flush();
					SystemMetrics sm = (SystemMetrics) ois.readObject();
					sm.time=System.currentTimeMillis();
					metricsMap.put(addr, sm);
					Thread.sleep(10000);
				}
			} catch (IOException ignore)
			{
				System.err.println(ignore.getMessage()+":"+ip);
				ignore.printStackTrace();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			metricsMap.remove(ip);
		}
	}

	private MetricsClient()
	{
		try
		{
			output = new PrintWriter(new FileWriter(new File("Metrics.csv"),false), true);
			output.println("IP,Time,Processor Time,Page Faults,Cache Faults,Available Bytes,Committed Bytes");
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run()
			{
				output.close();
			}
		});
	}

	public static MetricsClient getInstance()
	{
		if (InstanceHolder.INSTANCE == null)
		{
			InstanceHolder.INSTANCE = new MetricsClient();
			InstanceHolder.INSTANCE.start();
		}
		return InstanceHolder.INSTANCE;
	}

	private static class InstanceHolder
	{
		public static MetricsClient INSTANCE;;
	}

	public Map<InetAddress, SystemMetrics> metricsMap = new HashMap<>();

	public void addRealServer(String ip) throws Exception
	{
		new ClientThread(ip).start();
	}
	
	public Optional<SystemMetrics> getMetrics(String ip) throws Exception
	{
		System.out.println("getMetrics:"+ip);
		return Optional.ofNullable(metricsMap.get(InetAddress.getByName(ip)));
	}

	public Optional<SystemMetrics> getMetrics(InetAddress ip)
	{
		System.out.println("getMetrics:"+ip);
		return Optional.ofNullable(metricsMap.get(ip));
	}

	@Override
	public void run()
	{
		while (true)
		{
			for (Map.Entry<InetAddress, SystemMetrics> e : metricsMap.entrySet())
			{
				System.out.println("Hashmap");
				System.out.println(e.getKey());
				String st = e.getKey() + "," + e.getValue().toString();
				System.out.println(e.getValue());
				output.println(st);
			}
			try
			{
				Thread.sleep(10000);
			} catch (InterruptedException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		MetricsClient.getInstance().addRealServer("mahananda");
		MetricsClient.getInstance().addRealServer("DESKTOP-Q00J1OI");
	}
}
