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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class ApplicationThreadManager {

	public static final Logger log = Logger
	.getLogger(ApplicationThreadManager.class);

	private final ExecutorService executor;

	private final ConcurrentHashMap<String,Task> tasks =
		new ConcurrentHashMap<String,Task>();

	private interface ICancellationAware {
		
		/**
		 * Invoked after cancellation of a task.
		 *
		 * This method is always executed on the 
		 * EDT if this is an UI task.
		 * 		 
		 * @see ITask#isUITask()
		 */
		public void cancelled();

	}
	
	public interface ITask extends ICancellationAware {
		
		/**
		 * Invoked after the {@link #run()}
		 * method returned normally.
		 * 
		 * This method is always executed on the 
		 * EDT if this is an UI task.		 
		 * 
		 * @throws Exception
		 */
		public void success() throws Exception;
		
		/**
		 * Invoked right before this task is
		 * execution.
		 *
		 * This method is always executed on the 
		 * EDT if this is an UI task.		 
		 */		
		public void beforeExecution();
		
		/**
		 * Invoked after the {@link #run()}
		 * method failed with an exception.
		 * 
		 * This method is always executed on the 
		 * EDT if this is an UI task.
		 * 
		 * @throws Exception
		 * @see {@link #isUITask()}
		 */
		public void failure(Throwable t) throws Exception;
		
		/**
		 * Returns whether this is a UI task
		 * and callback methods should be invoked
		 * from the EDT.
		 * 
		 * @return
		 */
		public boolean isUITask();
	}
	
	public interface ITaskWithoutResult extends ITask {

		/**
		 * Perform task.
		 * 
		 * @throws Exception
		 */
		public void run() throws Exception;
		
	}
	
	public interface ITaskWithResult<T> extends ITask {
		
		/**
		 * Perform task.
		 * 
		 * @throws Exception
		 */		
		public T run() throws Exception;
		

	}	
	
	protected static final Object runTask(final ITask task) throws Exception {

		final boolean isUITask =
			task.isUITask();
		
		final AtomicReference<Throwable> ref=new AtomicReference<Throwable>();
		
		try {
			
			runOnEDT( isUITask , new Runnable() {
				@Override
				public void run() {
					task.beforeExecution();					
				}
			} );
			
			if ( task instanceof ITaskWithResult) {
				return ((ITaskWithResult<?>) task).run();
			} else if ( task instanceof ITaskWithoutResult) {
				((ITaskWithoutResult) task).run();
				return null;
			} else {
				throw new IllegalArgumentException("Unhandled ITask "+task);
			}
			
		} catch(Throwable t) {
			ref.set( t );
			if ( t instanceof Exception) {
				throw (Exception) t;
			} else if ( t instanceof Error ) {
				throw (Error) t;
			}
			throw new RuntimeException(t);
		}
		finally {
			
			final Runnable notifier =
				new Runnable() {

				@Override
				public void run() {
					
					try {
						if ( ref.get() != null ) {
							task.failure( ref.get() );
						} else {
							task.success();
						}
					} catch (Exception e) {
						log.error("run(): Exception after "+task+" done ",e);
						throw new RuntimeException(e);
					}
					
				}
			};
			
			runOnEDT( isUITask , notifier );
		}
	}
	
	protected static void runOnEDT(boolean isUITask , Runnable notifier) {
		if ( ! isUITask || SwingUtilities.isEventDispatchThread() ) {
			notifier.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( notifier );
			} catch (Exception e) {
				log.error("runOnEDT(): Caught ",e);
			}
		}
	}

	public final class Task extends FutureTask<Object> {

		private final String id;
		private final Object wrappedObject;

		public Task(String id,final Callable<Object> r) {
			super( new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return r.call();
				}} );
			this.wrappedObject = r;
			this.id = id;
		}
		
		public Task(String id,final ITaskWithResult<?> callable) {
			super( new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return runTask( callable );
				}
			});
			this.wrappedObject = callable;
			this.id = id;
		}
		
		public Task(String id,final ITaskWithoutResult callable) {
			super( new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return runTask( callable );
				}
			});
			this.wrappedObject = callable;
			this.id = id;
		}
		
		public boolean isUITask() {
			return (wrappedObject instanceof ITask) && ((ITask) wrappedObject).isUITask();
		}
		
		public Task(String id,final Runnable r) {
			super( new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					r.run();
					return null;
				}
			} );
			this.id = id;
			this.wrappedObject = r;
		}
		
		public String getId() {
			return id;
		}

		@Override
		protected void done() {
			
			boolean isCancelled =
				isCancelled();
			
			if ( log.isDebugEnabled() ) {
				log.debug("done(): task "+getId()+" [ cancelled = "+isCancelled+" ]" );
			}
			
			tasks.remove( id );
			
			if ( ! isCancelled  ) {
				return;
				
			}
			
			if (wrappedObject instanceof ICancellationAware) {
				
				runOnEDT( isUITask() , new Runnable() {
					@Override
					public void run() {
						((ICancellationAware) wrappedObject).cancelled();						
					}} );
			}			
		}

		public Object getWrappedObject() {
			return wrappedObject;
		}

	}
	
	public ApplicationThreadManager() {
		this(30);
	}

	public ApplicationThreadManager(int maxThreads) {
		this.executor = 
			new ThreadPoolExecutor(0, Integer.MAX_VALUE,
					60L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>()); // new ObservableLinkedBlockingQueue<Runnable>( 30 ) 
	}
	
	/**
	 * Shutdown thread manager.
	 * 
	 * @param force if set to <code>true</code> , all
	 * pending threads will be cancelled.
	 */
	public void shutdown(boolean force) {
		
		log.info("shutdown(): force = "+force);
		
		if ( force ) {
			executor.shutdownNow();
		} else {
			executor.shutdown();
		}
	}

	/**
	 * Tries to submit a named task, does nothing
	 * if a task by that name is still running.
	 * 
	 * @param <X>
	 * @param id
	 * @param t
	 * @return
	 */
	public <X> FutureTask<?>  submitTask(String id, ITaskWithResult<X> t) {
		return submitTask( id , new Task(id,t) );
	}	
	
	public FutureTask<?>  submitTask(UITask task) {
		return submitTask( task , false );
	}
	
	public FutureTask<?>  submitTask(UITask task,boolean cancelExisting) {
		final String id = task.getId();
		return submitTask( id , new Task(id,task) , cancelExisting );
	}	
	
	/**
	 * Tries to submit a named task, does nothing
	 * if a task by that name is still running.
	 * 
	 * @param <X>
	 * @param id
	 * @param t
	 * @return
	 */	
	public FutureTask<?>  submitTask(String id, ITaskWithoutResult t) {
		return submitTask( id , new Task(id,t) );
	}	

	/**
	 * Tries to submit a named task, does nothing
	 * if a task by that name is still running.
	 * 
	 * @param <X>
	 * @param id
	 * @param t
	 * @return
	 */	
	public FutureTask<?>  submitTask(String id, Runnable r) {
		return submitTask( id , new Task(id,r) , false );
	}	
	
	public FutureTask<?>  submitTask(String id, Runnable r,boolean cancelExisting) {
		return submitTask( id , new Task(id,r) , cancelExisting );
	}	
	
	public boolean containsTask(String id) {
		return tasks.containsKey( id );
	}
	
	public Task getTaskById(String id) {
		return tasks.get( id );
	}

	protected FutureTask<?>  submitTask(String id, Task task) {
		return submitTask( id , task , false );
	}
	
	protected FutureTask<?> submitTask(String id, Task task,boolean replaceExisting) {

		log.info("submitTask(): Submitting task with ID "+id);
		
		Task existing =
			tasks.putIfAbsent( id , task ) ;
		
		if ( existing != null && ! replaceExisting ) {
			log.info("submitTask(): [ NOT SUBMITTED] task ID="+id+" , " +
					"current thread count: "+tasks.size() );
			return null;
		}
		
		while ( existing != null ) {
			
			log.info("submitTask(): Cancelling existing task with ID '"+id+"' ...");
			existing.cancel(true);
				
			try {
				log.info("submitTask(): Waiting for task '"+id+"' to terminate...");
				existing.get();
			} 
			catch(Exception e) {
				log.info("submitTask(): Task '"+id+"' terminated",e);
			}
			
			// sanity check
			if ( tasks.get( id ) == existing ) {
				log.error("submitTask(): Internal error, failed to cancel task with ID '"+id+
						"' , class "+existing);
				throw new RuntimeException("Internal error, failed to cancel task with ID '"+id+
						"' , class "+existing);
			}
			existing =
				tasks.putIfAbsent( id , task ) ;			
		}
				

		boolean success = false;
		try {
			executor.execute( task );
			log.info("submitTask(): Successfully submitted task with ID "+id+" ( tasks in queue: "+tasks.size()+") ");
			success = true;
		} 
		finally {
			if ( ! success ) {
				tasks.remove( id );
			}
		}
		
		return task;
	}

}
