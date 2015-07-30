package com.idega.hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.annotation.Transactional;

import com.idega.util.DBUtil;

@Transactional(readOnly = true)
public class HibernateUtil extends DBUtil {

	private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());

	@Override
	@SuppressWarnings("unchecked")
	public <T> T initializeAndUnproxy(T entity) {
		if (entity == null) {
			throw new NullPointerException("Entity passed for initialization is null");
		}

		Hibernate.initialize(entity);
		if (entity instanceof HibernateProxy) {
			entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
		}

		return entity;
	}

	@Override
	public boolean isInitialized(Object object) {
		return Hibernate.isInitialized(object);
	}

	@Override
	public <T> T lazyLoad(T entity) {
		if (isInitialized(entity)) {
			return entity;
		}

		org.hibernate.Session s = null;
		Transaction transaction = null;
		try {
			s = SessionFactoryHelper.getInstance().getSessionFactory().openSession();
			transaction = s.beginTransaction();
			if (!s.isConnected()) {
				LOGGER.warning("Session is not opened, can not lazy load entity!");
			}

			s.refresh(entity);
			entity = initializeAndUnproxy(entity);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error initializing entity " + entity, e);
		} finally {
			transaction.commit();
			if (s.isOpen()) {
				s.close();
			}
		}

		return entity;
	}

	@Override
	public void doInitializeCaching(Query query, String cacheRegion) {
		if (query instanceof org.hibernate.Query) {
			org.hibernate.Query hQuery = (org.hibernate.Query) query;
			hQuery.setCacheable(true);
			if (cacheRegion != null) {
				hQuery.setCacheRegion(cacheRegion);
			}
		} else if (query instanceof HibernateQuery) {
			HibernateQuery hQuery = (HibernateQuery) query;
			org.hibernate.Query tmp = hQuery.getHibernateQuery();
			tmp.setCacheable(true);
			if (cacheRegion != null) {
				tmp.setCacheRegion(cacheRegion);
			}
		} else {
			LOGGER.warning("Query is not type of " + org.hibernate.Query.class.getName() + ", can not use caching");
		}
	}

}