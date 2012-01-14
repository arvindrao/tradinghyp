/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package com.appspot.tradinghyp;


import java.util.List;

import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * @author Arvind Rao
 * 
 * Implementation of DAO layer using Hibernate
 */
public class GenericDaoImpl implements GenericDao {

	private HibernateTemplate hibernateTemplate;
	
	public void setSessionFactory(SessionFactory sessionFactory){
		this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}
	
	@Override
	public <T> long save(T entity) {
		return (Long) hibernateTemplate.save(entity);
	}

	@Override
	public <T> void update(T entity) {
		hibernateTemplate.update(entity);
	}

	@Override
	public <T> void delete(T entity) {
		hibernateTemplate.delete(entity);
		
	}

	@Override
	public <T> T findEntity(Class<T> T, long entityId) {
		return hibernateTemplate.get(T, entityId);
	}

	@Override
	public <T> List<T> findTypedEntities(Class<T> T,String queryName) {
		return (List<T>) hibernateTemplate.findByNamedQuery(queryName);
	}

	@Override
	public List<Object[]> findNonEntities(String queryName) {
		return (List<Object[]>) hibernateTemplate.findByNamedQuery(queryName);
	}

	@Override
	public List<Trade> findTrades(long startTradeId, long endTradeId) {
        return this.hibernateTemplate.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) {
            	Query q=session.getNamedQuery(Constants.QUERY_TRADES);
            	q.setLong(0, 1);
            	q.setLong(1, 1);
                return (List<Trade>) q.list();
            }
        }
        );
    }

	@Override
	public List<Order> findBidsToMatch(final long price) {
        return this.hibernateTemplate.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) {
            	Query q=session.getNamedQuery(Constants.QUERY_MATCH_BIDS);
            	q.setLong("price", price);
                return (List<Order>) q.list();
            }
        }
        );
	}

	@Override
	public List<Order> findOffersToMatch(final long price) {
        return this.hibernateTemplate.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) {
            	Query q=session.getNamedQuery(Constants.QUERY_MATCH_OFFERS);
            	q.setLong("price", price);
                return (List<Order>) q.list();
            }
        }
        );
	}

	@Override
	public List<Order> findUserOrders(final long userId) {
        return this.hibernateTemplate.execute(new HibernateCallback() {

            public Object doInHibernate(Session session) {
            	Query q=session.getNamedQuery(Constants.QUERY_USER_ORDERS);
            	q.setLong("userId", userId);
                return (List<Order>) q.list();
            }
        }
        );
	}

	
}