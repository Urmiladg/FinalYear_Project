package lbs.realserver.metrics;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemMetrics implements java.io.Serializable, Cloneable
{
	private static final long serialVersionUID = -898408288215473421L;

	public float processorTime;
	public float pageFaults, cacheFaults;
	public double availableBytes, commitedBytes;
	public long time;
	///
	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	@Override
	public SystemMetrics clone()
	{
		try
		{
			return (SystemMetrics) super.clone();
		} catch (CloneNotSupportedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException("Should not be thrown");
	}

	@Override
	public String toString()
	{
		return sdf.format(new Date(time)) + ","+ processorTime+","+pageFaults+","+cacheFaults+","+availableBytes+","+commitedBytes;
				
	}
}
