package lbs.realserver.metrics;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SystemMetricsService extends Thread
{
	@Override
	public void run()
	{
		try
		{
			System.out.println("Starting service");
			ServerSocket ss = new ServerSocket(2000);
			while (true)
			{
				System.out.println("Waiting for client");
				final Socket s = ss.accept();
				new Thread() {
					@Override
					public void run()
					{
						try
						{
							ObjectOutputStream oos = new ObjectOutputStream(
									new BufferedOutputStream(s.getOutputStream()));
							oos.flush();
							ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
							while (true)
							{
								String command = ois.readUTF();
								switch (command.toUpperCase()) {
								case "GET_METRICS":
									SystemMetrics clone;
									synchronized (SystemMetricsThread.getInstance().systemMetrics)
									{
										clone = SystemMetricsThread.getInstance().systemMetrics.clone();
									}
									oos.writeObject(clone);
									oos.flush();

									break;
								}
							}
						} catch (IOException ignore)
						{
							ignore.printStackTrace();

						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}.start();
			}
		} catch (IOException ignore)
		{
			ignore.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		new SystemMetricsService().start();
	}
}
