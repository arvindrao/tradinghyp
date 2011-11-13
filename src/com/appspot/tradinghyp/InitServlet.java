/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Arvind Rao
 *
 * Initialization for a new instance
 */
public class InitServlet implements ServletContextListener {
	private static final Logger log = Logger.getLogger(InitServlet.class.getName());
	
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("BEGIN");
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery obPQ=null,pq=null,pq2=null,pq3=null;
		Query obQ=null,q=null;
		Entity ob=null,ostats=null,o=null,ustats=null,trade=null;
		Transaction txn=null;
		long orderCount=0L,lastOrderId=0L,lastTradeId=0L,tradeCount=0L;

		HashMap<String,User> users=null;
		CacheManager mcacheMgr = CacheManager.getInstance();
		Map props =null;

		try{
			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			if (obPQ.asIterator().hasNext()){
				ob=obPQ.asIterator().next();
			}
			else{
				ob=new Entity("OrderBook");
				ob.setProperty("active",true);
				txn = ds.beginTransaction();
				ds.put(ob);
				txn.commit();
			}

			q=new Query("OrderStats");
			q.setAncestor(ob.getKey());
			q.addFilter("active",Query.FilterOperator.EQUAL,true);
			pq=ds.prepare(q);
			ostats=pq.asIterator().hasNext()?pq.asIterator().next():null;

			q=new Query("Order");
			q.setAncestor(ob.getKey());
			q.addSort("orderId", Query.SortDirection.DESCENDING);
			pq=ds.prepare(q);
			o=pq.asIterator().hasNext()?pq.asIterator().next():null;

			q=new Query("Trade");
			q.setAncestor(ob.getKey());
			q.addSort("tradeId", Query.SortDirection.ASCENDING);
			q.addSort("tradeTime", Query.SortDirection.ASCENDING);
			pq2=ds.prepare(q);
			float openPrice=Constants.INIT_OPEN_PRICE;
			float lastPrice=Constants.INIT_OPEN_PRICE;
			float highPrice=Constants.INIT_OPEN_PRICE;
			float lowPrice=Constants.INIT_OPEN_PRICE;
			float closePrice=Constants.INIT_CLOSE_PRICE;

			long lastVol=0;
			double dayValue=0;
			
			long dayVol=0;
			for (Entity tstats : pq2.asIterable()){
				lastTradeId=(Long)tstats.getProperty("tradeId");
				lastPrice=((Double)tstats.getProperty("price")).floatValue();
				lastVol=(Long)tstats.getProperty("qty");
				dayVol+=lastVol;
				dayValue+=(lastVol*lastPrice);
				tradeCount++;
				if (lastPrice>highPrice){
					highPrice=lastPrice;
				}
				if (lastPrice<lowPrice){
					lowPrice=lastPrice;
				}
				
			}

			if (tradeCount==0){
				lastTradeId=1L;
				lastVol=Constants.LOT_SIZE*1;
				dayVol+=lastVol;
				dayValue+=(lastVol*lastPrice);
				tradeCount++;
				trade=new Entity("Trade",ob.getKey());
				trade.setProperty("tradeId", lastTradeId);
				trade.setProperty("tradeTime", System.currentTimeMillis());
				trade.setProperty("qty", lastVol);
				trade.setProperty("price", lastPrice);
				trade.setProperty("buyerId", "BOT1");
				trade.setProperty("sellerId", "BOT2");
			}

			Cache userCache=mcacheMgr.getCache("UserCache");
			if (userCache == null){
				log.info("Creating user cache");
				props = new HashMap();
				props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
				userCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("UserCache", userCache);
			}
			users=Util.fetchUsers();
			userCache.put(Constants.UserType.TRADER, users);

			q=new Query("UserStats");
			q.setAncestor(ob.getKey());
			q.addFilter("active",Query.FilterOperator.EQUAL,true);
			pq3=ds.prepare(q);
			ustats=pq3.asIterator().hasNext()?pq3.asIterator().next():null;
			
			if (ustats==null){
				log.info("Creating user stats in datastore");
				ustats=new Entity("UserStats",ob.getKey());
				ustats.setProperty("active",true);
				ustats.setProperty("lastGuestTraderId", 0L);
			}
			
			ustats.setProperty("activeTraderCount",users.size());
			
			if (o==null){
				orderCount=0L;
				lastOrderId=0L;
			}
			else{
				orderCount=pq.countEntities();
				lastOrderId=(Long)o.getProperty("orderId");
			}

			if (ostats==null){
				log.info("Creating order stats in datastore");
				ostats=new Entity("OrderStats",ob.getKey());
			}

			float changeFromClose=lastPrice-closePrice;
			float changeFromClosePercent=(changeFromClose/closePrice)*100;
			
			ostats.setProperty("orderCount", orderCount);
			ostats.setProperty("lastOrderId", lastOrderId);
			ostats.setProperty("lastTradeId", lastTradeId);
			ostats.setProperty("tradeCount", tradeCount);
			ostats.setProperty("lastVolume", lastVol);
			ostats.setProperty("lastPrice", lastPrice);
			ostats.setProperty("highPrice", highPrice);
			ostats.setProperty("lowPrice", lowPrice);
			ostats.setProperty("closePrice", closePrice);
			ostats.setProperty("openPrice", openPrice);
			ostats.setProperty("changeFromClose", changeFromClose);
			ostats.setProperty("changeFromClosePercent", changeFromClosePercent);
			ostats.setProperty("dayVolume", dayVol);
			ostats.setProperty("dayValue", dayValue);
			ostats.setProperty("active", true);

			txn=ds.beginTransaction();
			ds.put(ostats);
			ds.put(ustats);
			if (trade!=null){
				ds.put(trade);
			}
			txn.commit();

			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");
			if (mktDataCache == null){
				log.info("Creating MarketDataCache");
				props = new HashMap();
			    props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
				mktDataCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("MarketDataCache", mktDataCache);
			}

		}
		catch (Exception e){
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
		finally{
			if (txn!=null){
				if (txn.isActive()){
					log.severe("Rollback txn");
					txn.rollback();
				}
			}
			log.info("END");

		}


	}

}
