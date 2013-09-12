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
package de.codesourcery.eve.skills.db.dao;

import java.io.Serializable;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class HibernateDAO<T, PK extends Serializable> implements IReadOnlyDAO<T, PK> , InitializingBean
{
	
	private static final Logger log = Logger.getLogger(HibernateDAO.class);
	
	private final Class<T> clasz;
	
	private static final ThreadLocal<Session> currentSession = new ThreadLocal<Session>();

	private final TransactionTemplate template =
		new TransactionTemplate();
	
	private SessionFactory sessionFactory;
	
	protected interface HibernateCallback<T> {
		public T doInSession(Session session);
	}
	
	public void setSessionFactory(SessionFactory factory) {
		this.sessionFactory = factory;
	}

	private Session getCurrentSession() {
		
//		if ( ! SwingUtilities.isEventDispatchThread() ) 
//		{
//			final String msg = "Internal error - Thread "+
//			Thread.currentThread()+" [ "+Thread.currentThread().getName()+" ] tries "+
//			" to obtain a Hibernate session but it is not the EDT ?";
//			log.error("getCurrentSession(): "+msg, new Exception() );
//			throw new RuntimeException( msg );
//		}
		if ( currentSession.get() == null ) {
			currentSession.set( sessionFactory.openSession() );
		}
		return currentSession.get();
	}
	
	public HibernateDAO(Class<T> clasz) {
		this.clasz = clasz;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final T fetch(final PK id) {

		return (T) execute( new HibernateCallback<T>() {

			@Override
			public T doInSession(Session session) {
				return (T) getCurrentSession().load( clasz , id );
			}
		} );
	}
	
	@SuppressWarnings("unchecked")
	public List<T> fetchAll() {
		
		return (List<T>) execute( new HibernateCallback<List<T>>() {

			@Override
			public List<T> doInSession(Session session) {
				final Criteria c = getCurrentSession().createCriteria( clasz );
				return c.list();
			}} );
		
	}
	
	public void setTransactionManager(PlatformTransactionManager manager) {
		template.setTransactionManager( manager );
	}
	
	protected <X> X execute(HibernateCallback<X> callback) {
		
		final Session session = getCurrentSession();
		final Transaction transaction = session.beginTransaction();
		boolean success = false;
		try {
			X result = callback.doInSession( session);
			success = true;
			return result;
		} finally {
			if ( success ) {
				transaction.commit();
			} else {
				transaction.rollback();
			}
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		template.afterPropertiesSet();
	}
	
	protected static <T> T getExactlyOneResult(List<T> result) {
		if ( result.isEmpty() ) {
			throw new EmptyResultDataAccessException(1);
		}
		if ( result.size() > 1 ) {
			throw new IncorrectResultSizeDataAccessException("Internal error, got more than I expected?",1,result.size());
		}
		return result.get(0);
	}


}
