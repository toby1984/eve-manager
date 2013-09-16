package de.codesourcery.utils;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public abstract class DelayedMethodInvokerThread extends Thread 
{ 
	private static final Logger LOG = Logger.getLogger(DelayedMethodInvokerThread.class);
	
	private final AtomicLong lastEventTimestamp = new AtomicLong(0);
	
	private volatile boolean terminate = false;
	
	private final int delayInMillis;
	
	public DelayedMethodInvokerThread(int delayInMillis)
	{
		if ( delayInMillis < 50 ) { // limit delay because we're actually idle-polling ...
			throw new IllegalArgumentException("Delay must be >= 50 ms");
		}
		this.delayInMillis = delayInMillis;
		setName("delayedMethodInvoker-thread");
		setDaemon(true);
	}
	
	public void run() {
		
		while( ! terminate ) 
		{
			try 
			{
				Thread.sleep( Math.round( delayInMillis*0.9 ) );
			} 
			catch(Exception e) {
				// ok
			}
			long last = lastEventTimestamp.get();
			if ( last != 0 ) {
				long delta = System.currentTimeMillis() - last;
				if ( delta >= delayInMillis) 
				{
					if ( lastEventTimestamp.compareAndSet( last , 0 ) ) 
					{
						try {
							invokeDelayedMethod();
						} catch (Exception e) {
							LOG.error("run(): Caught ",e);
						}
					}
				}
			}
		}
	}
	
	public void terminate() {
		this.terminate = true;
	}
	
	public void eventOccured() {
		lastEventTimestamp.set( System.currentTimeMillis() );
	}
	
	protected abstract void invokeDelayedMethod() throws Exception;
}	