/**
 * Copyright 2004-2009 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.eve.skills.ui.utils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.util.SpringBeanInjector;

public class ParallelUITasksRunner
{

	private static final Logger log = Logger
			.getLogger(ParallelUITasksRunner.class);
	
	private static UITask createChild( final int index) {
		return new UITask() {

			@Override
			public String getId()
			{
				return "Child #"+index;
			}

			@Override
			public void run() throws Exception
			{
				System.out.println( getId()+" started.");
			}
		};
	}
	
	public static void main(String[] args)
	{
		
		SpringBeanInjector.setInstance( new SpringBeanInjector() );
	
		final ApplicationThreadManager threadManager = new ApplicationThreadManager();
		
		final UITask parent = new UITask() {

			@Override
			public String getId()
			{
				return "parent task";
			}
			
			@Override
			public void cancellationHook()
			{
				System.err.println("Parent task cancelled.");
				threadManager.shutdown(false);
			}
			
			@Override
			public void successHook() throws Exception
			{
				System.out.println("Parent task completed.");
				threadManager.shutdown(false);
			}
			
			@Override
			public void failureHook(Throwable t) throws Exception
			{
				System.err.println("Parent task caught exception "+t.getMessage());
				threadManager.shutdown(false);
			}

			@Override
			public void run() throws Exception
			{
				System.out.println("Parent task has started.");
				threadManager.shutdown(false);
			}
		} ;
		
		final UITask[] children =
			new UITask[] { createChild( 1 ) , createChild(2) , createChild(3) };
		
		boolean submitted =
			submitParallelTasks( threadManager , parent ,children ); 
		
		if ( ! submitted ) {
			System.err.println("Failed to submit parent task");
		}
	}
	
	public static boolean submitParallelTasks(final ApplicationThreadManager threadManager , 
			final UITask parent,UITask... children) 
	{
		final AtomicInteger childSuccesses = new AtomicInteger(0);
		final AtomicInteger childFailures = new AtomicInteger(0);
		
		if ( parent == null ) {
			throw new IllegalArgumentException("parent task cannot be NULL");
		}
		
		if ( ArrayUtils.isEmpty( children ) ) {
			throw new IllegalArgumentException("Need to provide at least one child task");
		}
		final CyclicBarrier startBarrier =
			new CyclicBarrier( children.length + 1 ) {
			@Override
			public void reset()
			{
				System.out.println("========== resetting start barrier =========== ");
				super.reset();
			}
		};
		
		final CountDownLatch childrenTerminated =
			new CountDownLatch ( children.length );
		
		int submittedChildren = 0;
		for ( final UITask child : children ) 
		{
			
			final UITask wrapped = new UITask() {

				@Override
				public void successHook() throws Exception
				{
					boolean success = false;
					try {
						child.successHook();
						success = true;
					} finally {
						if ( success ) {
							childSuccesses.incrementAndGet();
						} else {
							childFailures.incrementAndGet();
						}
						childrenTerminated.countDown();
					}
				}
				
				@Override
				public void beforeExecution()
				{
					child.beforeExecution();
					// note: when this method throws an exception , #failure() is invoked
				}
				
				@Override
				public void setEnabledHook(boolean yesNo)
				{
					child.setEnabledHook(yesNo);
					
				}
				
				@Override
				public void failureHook(Throwable t) throws Exception
				{
					try {
						child.failureHook( t );
					} finally {
						childFailures.incrementAndGet();
						childrenTerminated.countDown();
					}
				}

				@Override
				public void cancellationHook()
				{
					try {
						child.cancellationHook();
					} finally {
						childFailures.incrementAndGet();
						childrenTerminated.countDown();
					}
				}
				
				@Override
				public String getId()
				{
					return child.getId();
				}

				@Override
				public void run() throws Exception
				{
					try {
						if ( log.isTraceEnabled() ) {
							log.trace("run(): Child task "+getId()+" is now waiting.");
						}
						startBarrier.await(); // will BrokenBarrierException if any of the child tasks could not be started,
					} catch(InterruptedException e ) {
						log.error("run(): Child task "+getId()+" was interrupted");
						Thread.currentThread().interrupt();
					}
					catch(BrokenBarrierException e) {
						log.error("run(): Child task"+getId()+" aborted, barrier broken.");
						throw new RuntimeException("Child task not started because another child task failed submitTask()");
					}
					
					if ( log.isTraceEnabled() ) {
						log.trace("run(): Child task "+getId()+" is now running.");
					}
					child.run();
				}
			};
			
			if ( null == threadManager.submitTask( wrapped , false ) ) {
				log.error("submitParallelTasks(): Failed to submit child "+child);
				
				// note: I wait for (submittedChildren-childFailures) because some 
				// child task may have already failed before reaching their run() method
				while ( startBarrier.getNumberWaiting() != ( submittedChildren - childFailures.get() ) ) 
				{
					log.info("submitParallelTasks(): Waiting for all child tasks to reach barrier ( "+startBarrier.getNumberWaiting()+" waiting)");
					try {
						java.lang.Thread.sleep(500);
					} catch(Exception e) {};
				}
				
				startBarrier.reset(); // will cause all child threads waiting on this barrier to terminate
				return false;
			}
			submittedChildren++;
		}
		
		/*
		 * All children are submitted and waiting at the barrier.
		 */
		final boolean parentSubmitted = null != threadManager.submitTask( "Control thread of "+parent.getId() , 
				new Runnable() {

					@Override
					public void run()
					{
						try {
							
							while( true ) {
								try {
									log.debug("run(): Parent task "+parent.getId()+" is waiting for it's children to start...");
									startBarrier.await( 5, TimeUnit.SECONDS );
									break;
								}
								catch (TimeoutException e) {
									if ( childFailures.get() != 0 ) {
										runFailureHookOnEDT(parent , childFailures.get()+
												" child tasks of parent task "+parent.getId()+" failed to start.");
										return;
									}
								}
							}
						}
						catch (InterruptedException e) {
							runFailureHookOnEDT(parent,"Parent task "+parent.getId()+" was interrupted while waiting"+
									" for it's children to start.");
							startBarrier.reset(); // let children fail.
							Thread.currentThread().interrupt();
							return;
						}
						catch (BrokenBarrierException e) {
							runFailureHookOnEDT(parent,"Parent task "+parent.getId()+" failed to wait for it's children");
							throw new RuntimeException("Internal error - task "+parent.getId()+" failed to wait for it's children?");
						}
						
						log.debug("run(): Task "+parent.getId()+" is waiting for it's children to finish");
						try {
							childrenTerminated.await();
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							runFailureHookOnEDT( parent , "Parent task "+parent.getId()+
									" was interrupted while waiting for it's children");
							return;
						}
						
						log.info("run(): All children of parent task "+parent.getId()+
								" have finished ( success: "+
								childSuccesses.get()+" / failure: "+childFailures.get()+")");
						
						if ( childFailures.get() > 0 ) {
							runFailureHookOnEDT(parent, childFailures.get()+
									" child tasks of parent "+parent.getId()+" have FAILED.");
							return;
						} 
						
						if ( null == threadManager.submitTask( parent , false ) ) {
							runFailureHookOnEDT( parent , "Failed to submit parent task "+parent.getId());
							return;
						}
						
					}} , false );
		
		if ( ! parentSubmitted ) {
			log.debug("submitParallelTasks(): Failed to submit parent task "+parent.getId()+" , terminating child tasks.");
			startBarrier.reset(); // aborts all child tasks with a BrokenBarrierException
		}
		
		return parentSubmitted;
	}
	
	protected static void runFailureHookOnEDT(final UITask parent,final String msg) {
		
		log.error("runFailureHookOnEDT(): Failed to submit parent task "+parent.getId()+" : "+msg);
		
		SwingUtilities.invokeLater( new Runnable() {

			@Override
			public void run()
			{
				try {
					parent.failure( new RuntimeException( msg ) );
				}
				catch (Exception e) {
					log.error("runFailureHookOnEDT(): failure() method of "+parent.getId()+" threw an exception ",e);
				}
			}} );
	}
}
