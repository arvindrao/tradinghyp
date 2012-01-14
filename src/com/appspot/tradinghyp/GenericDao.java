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
import org.hibernate.SessionFactory;
/**
 * @author Arvind Rao
 * 
 * DAO layer. Contains generic methods and entity specific methods 
 */
public interface GenericDao {
	
	//generic methods
	public void setSessionFactory(SessionFactory sessionFactory);
	public <T> long save(T entity);
	public <T> void update(T entity);
	public <T> void delete(T entity);
	public <T> T findEntity(Class<T> T,long entityId);
	public <T> List<T> findTypedEntities(Class<T> T,String queryName);
	public List<Object[]> findNonEntities(String queryName);
	
	//entity specific methods
	public List<Trade> findTrades(long startTradeId,long endTradeId);
	public List<Order> findBidsToMatch(long price);
	public List<Order> findOffersToMatch(long price);
	public List<Order> findUserOrders(long userId);
}
