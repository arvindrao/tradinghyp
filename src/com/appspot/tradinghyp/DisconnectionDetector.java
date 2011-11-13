/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import javax.servlet.http.*;
import com.google.appengine.api.channel.*;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @author Arvind Rao
 * 
 * Process user disconnections
 */

@SuppressWarnings("serial")
public class DisconnectionDetector extends HttpServlet {
	private static final Logger log = Logger.getLogger(DisconnectionDetector.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		ChannelService chService;
		ChannelPresence chPresence;
		String traderId=null;
		ArrayList<Entity> updatedOrders;
		Short side;
		DatastoreService ds;
		Transaction txn=null;
		PreparedQuery pq=null,obPQ=null,pq2=null,pq3=null;
		Query q=null, obQ=null;
		Queue mdQueue= null;
		boolean activeOrders=false;
		boolean mktDataCacheAvailable=false;
		TreeMap<Float,Long> askMap=null,bidMap=null;
		long orderQty,execQty;
		long lastVol=0,availableVolAtThisPrx;
		float lastPrice;
		Entity ob=null,u=null,ustats=null;
		Long activeTraderCount=0L;
		CacheManager mcacheMgr =null;
		Cache userCache=null;
		HashMap<String,User> users=null;
		log.info("BEGIN");

		try{
			mcacheMgr = CacheManager.getInstance();

			chService=ChannelServiceFactory.getChannelService();
			chPresence=chService.parsePresence(req);
			traderId=chPresence.clientId();
			log.info("TraderId "+traderId);

			updatedOrders=new ArrayList<Entity>();

			ds = DatastoreServiceFactory.getDatastoreService();

			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			ob=obPQ.asIterator().next();

			mcacheMgr = CacheManager.getInstance();

			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");

			if (mktDataCache!=null){
				bidMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
				askMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);

				if (bidMap==null || askMap==null){
					mktDataCacheAvailable=false;
				}
				else{
					mktDataCacheAvailable=true;
				}
			}
			else{
				mktDataCacheAvailable=false;
			}
			
			q=new Query("User");
			q.setAncestor(ob.getKey());
			q.addFilter("traderId", FilterOperator.EQUAL, traderId);
			q.addFilter("logoutTime", FilterOperator.EQUAL, 0);
			pq2=ds.prepare(q);
			if (pq2.asIterator().hasNext()){
				u=pq2.asIterator().next();
				u.setProperty("logoutTime", System.currentTimeMillis());
				txn=ds.beginTransaction();
				ds.put(u);
				txn.commit();
				log.info("Removed from User");
			}

			userCache=mcacheMgr.getCache("UserCache");
			if (userCache != null){
				users=(HashMap<String,User>) userCache.get(Constants.UserType.TRADER);
				if (users!=null){
					users.remove(traderId);
					userCache.put(Constants.UserType.TRADER, users);
					log.info("Removed from UserCache");
				}
			}

			q=new Query("UserStats");
			q.setAncestor(ob.getKey());
			q.addFilter("active", FilterOperator.EQUAL, true);
			pq3=ds.prepare(q);
			ustats=pq3.asIterator().next();
			activeTraderCount=(Long)ustats.getProperty("activeTraderCount")-1;
			ustats.setProperty("activeTraderCount",activeTraderCount);
			txn=ds.beginTransaction();
			ds.put(ustats);
			txn.commit();
			log.info("Updated UserStats");

			q=new Query("Order");
			q.setAncestor(ob.getKey());
			q.addFilter("traderId",Query.FilterOperator.EQUAL,traderId);
			q.addFilter("status",Query.FilterOperator.EQUAL,Constants.ORD_ACK);
			pq = ds.prepare(q);

			if (pq.countEntities()>0){
				activeOrders=true;
			}
			else{
				activeOrders=false;
				log.info("No active orders");
			}

			if (activeOrders){
				//cancel orders
				for (Entity res : pq.asIterable()){
					log.info("Cancelling orderId "+res.getProperty("orderId"));
					orderQty=(Long)res.getProperty("orderQty");
					execQty=(Long)res.getProperty("execQty");
					side=((Long)res.getProperty("side")).shortValue();

					lastPrice=((Double)res.getProperty("price")).floatValue();
					lastVol=orderQty-execQty;
					res.setProperty("status", Constants.ORD_CXL);
					updatedOrders.add(res);

					if (mktDataCacheAvailable){
						availableVolAtThisPrx=(side==Constants.BUY?bidMap:askMap).get(lastPrice)-lastVol;
						if (availableVolAtThisPrx==0){
							(side==Constants.BUY?bidMap:askMap).remove(lastPrice);
						}
						else{
							(side==Constants.BUY?bidMap:askMap).put(lastPrice, availableVolAtThisPrx);
						}
					}

				}
				
				txn = ds.beginTransaction();
				ds.put(updatedOrders);
				txn.commit();

				//update market data
				if (mktDataCacheAvailable){
					mktDataCache.put(Constants.MktDataStatistic.BID, bidMap);
					mktDataCache.put(Constants.MktDataStatistic.ASK, askMap);
				}
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
				else{
					if (activeOrders){
						mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
						mdQueue.add(
								withUrl("/tasks/processMarketData.do")
								.param("reloadCache", mktDataCacheAvailable?"N":"Y")
								.method(Method.POST)
								);
						
					}

				}
			}
			log.info("END");

		}

	}
}
