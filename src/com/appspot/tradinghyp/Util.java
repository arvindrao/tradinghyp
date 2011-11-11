/**
 * 
 */
package com.appspot.tradinghyp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;

/**
 * @author Arvind Rao
 *
 */
public class Util {
	private static final Logger log = Logger.getLogger(Util.class.getName());

	public static HashMap<String,User> fetchUsers(){
		log.info("BEGIN");
		HashMap<String,User> users=new HashMap<String,User>();

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery obPQ=null,pq=null;
		Query obQ=null,q=null;
		Entity ob=null;

		obQ=new Query("OrderBook");
		obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
		obPQ = ds.prepare(obQ);
		if (obPQ.asIterator().hasNext()){
			ob=obPQ.asIterator().next();
		}

		q=new Query("User");
		q.setAncestor(ob.getKey());
		q.addFilter("logoutTime", FilterOperator.EQUAL, 0);
		pq=ds.prepare(q);
		for (Entity res : pq.asIterable()){
			User aUser=new User();
			aUser.setUserId((String)res.getProperty("userId"));
			aUser.setTraderId((String)res.getProperty("traderId"));
			aUser.setLoginTime((Long)res.getProperty("loginTime"));
			aUser.setLogoutTime((Long)res.getProperty("logoutTime"));
			aUser.setLongAvgPrice(((Double)res.getProperty("longAvgPrice")).floatValue());
			aUser.setLongCash((Double)res.getProperty("longCash"));
			aUser.setLongPosition((Long)res.getProperty("longPosition"));
			aUser.setShortAvgPrice(((Double)res.getProperty("shortAvgPrice")).floatValue());
			aUser.setShortCash((Double)res.getProperty("shortCash"));
			aUser.setShortPosition((Long)res.getProperty("shortPosition"));
			aUser.setOrigRealizedGain((Double)res.getProperty("origRealizedGain"));
			aUser.setRealizedGain((Double)res.getProperty("realizedGain"));
			users.put(aUser.getTraderId(), aUser);
		}

		log.info("END");
		return users;
	}

	public static TreeMap<Float,Long> getBidsFromOrderBook(){
		log.info("BEGIN");
		float lastPrice=0;
		long lastQty=0;
		//reverse sort order because best (higher) bids should be on top
		TreeMap<Float,Long> bidMap=new TreeMap<Float,Long>(Collections.reverseOrder());

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query obQ=new Query("OrderBook");
		obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
		PreparedQuery obPQ = ds.prepare(obQ);
		Entity ob=obPQ.asIterator().next();

		//find active orders
		Query q=new Query("Order");
		q.setAncestor(ob.getKey());
		q.addFilter("status",Query.FilterOperator.EQUAL,Constants.ORD_ACK);
		q.addFilter("side",Query.FilterOperator.EQUAL,Constants.BUY);
		q.addSort("price", Query.SortDirection.DESCENDING);

		PreparedQuery pq = ds.prepare(q);
		//log.info(pq.countEntities());
		lastQty=0;

		for (Entity res : pq.asIterable()){
			if (((Double)(res.getProperty("price"))).floatValue()!=lastPrice && lastPrice!=0 && lastQty>0){
				//store the last ask
				bidMap.put(lastPrice, lastQty);
				lastQty=0;
			}

			//accumulate volumes at this bid price
			lastPrice=((Double)(res.getProperty("price"))).floatValue();
			lastQty+=(Long)(res.getProperty("orderQty"))-(Long)(res.getProperty("execQty"));
		}

		//store the last processed bid
		if (lastPrice!=0 && lastQty>0){
			bidMap.put(lastPrice, lastQty);
		}
		
		log.info("END");
		return bidMap;
	}

	public static TreeMap<Float,Long> getAsksFromOrderBook(){
		log.info("BEGIN");
		float lastPrice=0;
		long lastQty=0;
		TreeMap<Float,Long> askMap=new TreeMap<Float,Long>();

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query obQ=new Query("OrderBook");
		obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
		PreparedQuery obPQ = ds.prepare(obQ);
		Entity ob=obPQ.asIterator().next();

		Query q=new Query("Order");
		q.setAncestor(ob.getKey());
		q.addFilter("status",Query.FilterOperator.EQUAL,Constants.ORD_ACK);
		q.addFilter("side",Query.FilterOperator.EQUAL,Constants.SELL);
		q.addSort("price", Query.SortDirection.ASCENDING);
		PreparedQuery pq = ds.prepare(q);

		lastPrice=0;
		lastQty=0;

		for (Entity res : pq.asIterable()){
			if (((Double)(res.getProperty("price"))).floatValue()!=lastPrice && lastPrice!=0 && lastQty>0){
				//store the last ask
				askMap.put(lastPrice, lastQty);
				lastQty=0;
			}
			//accumulate volumes at this ask price
			lastPrice=((Double)(res.getProperty("price"))).floatValue();
			lastQty+=(Long)(res.getProperty("orderQty"))-(Long)(res.getProperty("execQty"));
		}

		//store the last processed ask
		if (lastPrice!=0 && lastQty>0){
			askMap.put(lastPrice, lastQty);
		}

		log.info("END");

		return askMap;

	}
	
	public static ArrayList<Trade> getTradesFromTradeBook(Long startTradeId,Long endTradeId){
		log.info("BEGIN");
		//reverse sort order because best (higher) bids should be on top
		ArrayList<Trade> tlist=new ArrayList<Trade>();

		if (startTradeId==0 || endTradeId==0 || startTradeId>endTradeId){
			return tlist;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query obQ=new Query("OrderBook");
		obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
		PreparedQuery obPQ = ds.prepare(obQ);
		Entity ob=obPQ.asIterator().next();

		//find trades
		Query q=new Query("Trade");
		q.setAncestor(ob.getKey());
		q.addFilter("tradeId",Query.FilterOperator.GREATER_THAN_OR_EQUAL,startTradeId);
		q.addFilter("tradeId",Query.FilterOperator.LESS_THAN_OR_EQUAL,endTradeId);
		q.addSort("tradeId", Query.SortDirection.ASCENDING);
		q.addSort("tradeTime", Query.SortDirection.ASCENDING);

		PreparedQuery pq = ds.prepare(q);
		//log.info(pq.countEntities());

		for (Entity res : pq.asIterable()){
			Trade tradeCacheEntry=new Trade((Long)res.getProperty("tradeId"),
											(Long)res.getProperty("tradeTime"),
											(Long)res.getProperty("qty"),
											((Double)res.getProperty("price")).floatValue(),
											(String)res.getProperty("buyerId"),
											(String)res.getProperty("sellerId")
											);
			tlist.add(tradeCacheEntry);
		}

		
		log.info("END");
		return tlist;
		
	}


}
