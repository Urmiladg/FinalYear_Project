package lbs.loadbalancer.redirection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import lbs.loadbalancer.metrics.MetricsClient;
import lbs.realserver.metrics.SystemMetrics;

public class RedirectionServer1 extends Thread
{

	public static final InetSocketAddress[] REALSERVER = {new InetSocketAddress("URMILA-PC", 8085),new InetSocketAddress("DESKTOP-Q00J1OI",8080), new InetSocketAddress("mahananda",8084) };
	public static final int LOADBALANCER_PORT = 8084;

	class RedirectionThread extends Thread
	{
		InputStream is;
		OutputStream os;

		RedirectionThread(InputStream is, OutputStream os)
		{
			this.os = os;
			this.is = is;
		}

		public void run()
		{
			byte[] b = new byte[102400];
			int n;

			try
			{
				while ((n = is.read(b)) != -1)
				{
					os.write(b, 0, n);
				}
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			try
			{
				is.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try
			{
				os.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block0
				e.printStackTrace();
			}
		}
	}

	public void run()
	{
		int n = 0;
		try
		{
			ServerSocket ss = new ServerSocket(LOADBALANCER_PORT, 10);
			while (true)
			{
				System.out.println("Loadbalabcer waiting for clients...");
				final Socket s = ss.accept();
				System.out.println("Loadbalancer connected client " + s);
				final InetSocketAddress selectedserver = REALSERVER[n];
				System.out.println("Hello1");
				n++;
				if (n >= REALSERVER.length)
					n = 0;
				new Thread() {
					public void run()
					{
						System.out.println("Hello2");

						long ts = System.nanoTime();
						try (final Socket s2 = new Socket() {
							{
								System.out.println("Redirecting to : " + selectedserver);
								this.connect(selectedserver);
							}
						}; InputStream is1 = s.getInputStream(); OutputStream os1 = s.getOutputStream();)
						{

							InputStream is2 = s2.getInputStream();
							OutputStream os2 = s2.getOutputStream();

							RedirectionThread t1 = new RedirectionThread(is1, os2);
							t1.start();
							RedirectionThread t2 = new RedirectionThread(is2, os1);
							t2.start();

							t1.join();
							t2.join();

						} catch (Exception ignore)
						{

						}
						long te = System.nanoTime();

						long diff = (te - ts);
						try
						{
							System.out.println("Logging entry :" + selectedserver + ":" + diff);
							log(selectedserver, diff);
						} catch (FileNotFoundException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}.start();
			}
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static PrintWriter logwriter;

	public static void log(InetSocketAddress ip, long responsetime) throws FileNotFoundException
	{
		if (logwriter == null)
		{
			try
			{

				File file = new File("data1.csv");
				boolean exists = file.exists();

				logwriter = new PrintWriter(new FileWriter(file, true), true);
				if (!exists)
				{
					logwriter.println(
							"Host Name,Time,Processor Time,Page Faults,Cache Faults,Available Bytes,Committed Bytes,Response Time");
					System.out.println("HERE   .............");
				}
				System.out.println("HERE   .............");
				
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run()
					{
						if (logwriter != null)
						{
							logwriter.flush();
							logwriter.close();
						}
					}
				});
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// write csv data
		System.out.println("Metrics" + MetricsClient.getInstance().metricsMap);

		Optional<SystemMetrics> sm = MetricsClient.getInstance().getMetrics(ip.getAddress());
		if (sm.isPresent())
		{
			logwriter.println(ip.getAddress().getHostName() + "," + sm.get() + "," + responsetime);
		}
		else
		{
			System.err.println("Data not found for "+ip.getAddress());
		}

	}

	public static void main(String[] args) throws Exception
	{
		for (InetSocketAddress a : REALSERVER)
		{
			MetricsClient.getInstance().addRealServer(a.getHostString());
		}

		new RedirectionServer1().start();
	}
}
