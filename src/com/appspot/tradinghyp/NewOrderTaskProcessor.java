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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.*;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.channel.*;
import java.text.DecimalFormat;

/**
 * @author Arvind Rao
 *
 * Process new order requests and match with opposite side if possible
 */

@SuppressWarnings("serial")
public class NewOrderTaskProcessor extends HttpServlet {
	private static final Logger log = Logger.getLogger(NewOrderTaskProcessor.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		short side=new Short(req.getParameter("side"));
		long partyOrderQty=new Long(req.getParameter("qty"));
		float partyOrderPrice=new Float(req.getParameter("price"));
		String traderId=req.getParameter("traderId");
		
		HashMap<String,StringBuffer> orderUpdate = new HashMap<String,StringBuffer>();
		HashMap<String,String> scoreUpdate = new HashMap<String,String>();
		
		ArrayList<Entity> updatedOrders=new ArrayList<Entity>();
		
		//log.info("Order Task Processor:"+symbol+";"+side+";"+partyOrderQty+";"+partyOrderPrice+";"+traderId);
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn=null;
		PreparedQuery pq=null,obPQ=null;
		Query q=null, obQ=null;
		Queue mdQueue= null;
		boolean matchPossible=false;
		boolean mktDataCacheAvailable=false,tradeCacheAvailable=false;
		TreeMap<Float,Long> askMap=null,bidMap=null;
		ArrayList<Trade> tradeList=new ArrayList<Trade>();
		long orderQty,execQty,temp=partyOrderQty;
		long lastVol=0,availableVolAtThisPrx,currQty=0;
		float lastPrice = 0,lowPrice,highPrice,closePrice;
		Entity ob=null,ostats=null;
		long lastOrderId=0L,orderCount=0L,
			 lastTradeId=0L,tradeCount=0L,startTradeId=0L,endTradeId=0L,orderId=0L,
			 oldDayVol=0L;
		double oldDayValue=0;
		String score;
		long tradeVol=0;
		Map props =null;
	
		try{
			
			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			ob=obPQ.asIterator().next();

			q=new Query("OrderStats");
			q.setAncestor(ob.getKey());
			q.addFilter("active",Query.FilterOperator.EQUAL,true);
			pq=ds.prepare(q);
			ostats=pq.asIterator().next();

			lastOrderId=(Long)ostats.getProperty("lastOrderId");
			orderCount=(Long)ostats.getProperty("orderCount");
			lastTradeId=(Long)ostats.getProperty("lastTradeId");
			tradeCount=(Long)ostats.getProperty("tradeCount");
			oldDayVol=(Long)ostats.getProperty("dayVolume");
			oldDayValue=((Double)ostats.getProperty("dayValue"));
			highPrice=((Double)ostats.getProperty("highPrice")).floatValue();
			lowPrice=((Double)ostats.getProperty("lowPrice")).floatValue();
			closePrice=((Double)ostats.getProperty("closePrice")).floatValue();

			//Order matching begins
			CacheManager mcacheMgr = CacheManager.getInstance();

			Cache userCache=mcacheMgr.getCache("UserCache");
			HashMap<String,User> users=null;
			if (userCache != null){
				users=(HashMap<String,User>) userCache.get(Constants.UserType.TRADER);
			}
			else{
				log.warning("Creating user cache");
				props = new HashMap();
				props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
				userCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("UserCache", userCache);
			}
			if (users==null){
				users=Util.fetchUsers();
			}
			
			HashMap<String,User> updateUserScore=new HashMap<String,User>();
			User partyUser=users.get(traderId);

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
				tradeCacheAvailable=true;
				
			}
			else{
				mktDataCacheAvailable=false;
				tradeCacheAvailable=false;
			}

			if (side==Constants.BUY){
				if (mktDataCacheAvailable){
					//use the cache
					if (askMap.isEmpty()){
						//no offers
						matchPossible=false;
					}
					else if (partyOrderPrice>0 && askMap.firstKey()<=partyOrderPrice){
						//there is at least on offer which can be matched
						 matchPossible=true;
					 }
					else if (partyOrderPrice==0){
						matchPossible=true;
					}
					currQty=bidMap.containsKey(partyOrderPrice)?bidMap.get(partyOrderPrice):0;
				}

				if(!mktDataCacheAvailable || (mktDataCacheAvailable && matchPossible)){
					//get offers from order book
					lastPrice=0;

					q=new Query("Order");
					q.setAncestor(ob.getKey());
					q.addFilter("status",Query.FilterOperator.EQUAL,Constants.ORD_ACK);
					q.addFilter("side",Query.FilterOperator.EQUAL,Constants.SELL);
					if (partyOrderPrice>0){
						q.addFilter("price", Query.FilterOperator.LESS_THAN_OR_EQUAL, partyOrderPrice);
					}
					q.addSort("price", Query.SortDirection.ASCENDING);
					q.addSort("orderTime", Query.SortDirection.ASCENDING);
					pq = ds.prepare(q);

					if (pq.countEntities()>0){
						matchPossible=true;
					}
					else{
						matchPossible=false;
					}

				}
				
			}
			else{
				if (mktDataCacheAvailable){
					//use the cache
					if (bidMap.isEmpty()){
						//no bids
						matchPossible=false;
					}
					else if (partyOrderPrice>0 && bidMap.firstKey()>=partyOrderPrice){
						//there is at least one bid which can be matched
						matchPossible=true;
					}
					else if (partyOrderPrice==0){
						matchPossible=true;
					}
					currQty=askMap.containsKey(partyOrderPrice)?askMap.get(partyOrderPrice):0;
				}
				
				if(!mktDataCacheAvailable || (mktDataCacheAvailable && matchPossible)){
					//get bids from order book
					q=new Query("Order");
					q.setAncestor(ob.getKey());
					q.addFilter("status",Query.FilterOperator.EQUAL,Constants.ORD_ACK);
					q.addFilter("side",Query.FilterOperator.EQUAL,Constants.BUY);
					if (partyOrderPrice>0){
						q.addFilter("price", Query.FilterOperator.GREATER_THAN_OR_EQUAL, partyOrderPrice);
					}
					q.addSort("price", Query.SortDirection.DESCENDING);
					q.addSort("orderTime", Query.SortDirection.ASCENDING);
					pq = ds.prepare(q);

					if (pq.countEntities()>0){
						matchPossible=true;
					}
					else{
						matchPossible=false;
					}

				}
				
			}
			
			double cpartyTradeValue=0;
			double tradeValue=0;
			String cpartyTraderId=null;
			StringBuffer orderData=null;
			StringBuffer oldOrderData=null;
			
			if (matchPossible){
				//matchOrder
				startTradeId=lastTradeId+1;
				
				for (Entity res : pq.asIterable()){

					Entity trade=new Entity("Trade",ob.getKey());
					orderQty=(Long)res.getProperty("orderQty");
					execQty=(Long)res.getProperty("execQty");
					lastPrice=((Double)res.getProperty("price")).floatValue();
					cpartyTradeValue=(Double)res.getProperty("tradeValue");
					orderId=(Long)res.getProperty("orderId");
					short cpartySide=((Long)res.getProperty("side")).shortValue();
					//Build order data update JSON
					cpartyTraderId=(String)res.getProperty("traderId");
					orderData=new StringBuffer("{\"orderId\":"+orderId
												+",\"side\":"+(cpartySide==Constants.BUY?"\"B\"":"\"S\"")
												+",\"limitPrice\":"+new DecimalFormat("#0.00").format(lastPrice)
												+",\"orderQty\":"+orderQty);

					if (temp>orderQty-execQty){
						lastVol=orderQty-execQty;
						temp-=(orderQty-execQty);
						cpartyTradeValue+=lastVol*lastPrice;
						res.setProperty("execQty", orderQty);
						res.setProperty("status", Constants.ORD_EXEC);
						res.setProperty("tradeValue", cpartyTradeValue);
						orderData.append(",\"execQty\":"+orderQty
										 +",\"avgPrice\":"+new DecimalFormat("#0.00").format(cpartyTradeValue/orderQty)
										 +",\"status\":\"Done\"}");
						
					}
					else if (temp==orderQty-execQty){
						lastVol=orderQty-execQty;
						temp=0;
						cpartyTradeValue+=lastVol*lastPrice;
						res.setProperty("execQty", orderQty);
						res.setProperty("status", Constants.ORD_EXEC);
						res.setProperty("tradeValue", cpartyTradeValue);
						orderData.append(",\"execQty\":"+orderQty
								 +",\"avgPrice\":"+new DecimalFormat("#0.00").format(cpartyTradeValue/orderQty)
								 +",\"status\":\"Done\"}");
						
					}
					else if (temp<orderQty-execQty){
						lastVol=temp;
						cpartyTradeValue+=lastVol*lastPrice;
						res.setProperty("execQty", execQty+temp);
						//dont change status, still working
						res.setProperty("tradeValue", cpartyTradeValue);
						orderData.append(",\"execQty\":"+(execQty+temp)
								 +",\"avgPrice\":"+new DecimalFormat("#0.00").format(cpartyTradeValue/(execQty+temp))
								 +",\"status\":\"Working\"}");
						temp=0;

					}
					
					if (!traderId.equals(cpartyTraderId)){
						User cpartyUser=users.get(cpartyTraderId);
						//if user logged off and this is their order
						//create a new dummy user to handle gracefully
						if (cpartyUser==null){
							cpartyUser=new User("LoggedOff",cpartyTraderId);
						}
						cpartyUser.updateScore(cpartySide, lastVol, lastPrice);
						users.put(cpartyTraderId, cpartyUser);
						updateUserScore.put(cpartyTraderId, cpartyUser);
						score=new String(",\"score\":{\"realizedGain\":"
												+cpartyUser.getRealizedGain()
												+",\"longPosition\":"
												+cpartyUser.getLongPosition()
												+",\"longAvgPrice\":"
												+cpartyUser.getLongAvgPrice()
												+",\"shortPosition\":"
												+cpartyUser.getShortPosition()
												+",\"shortAvgPrice\":"
												+cpartyUser.getShortAvgPrice()
												+"}"
												);
						scoreUpdate.put(cpartyTraderId, score);
					}
					else{
						//self-match
						partyUser.updateScore(cpartySide, lastVol, lastPrice);
						//dont update json here, do it below
					}
					
					partyUser.updateScore(side, lastVol, lastPrice);
					score=new String(",\"score\":{\"realizedGain\":"
							+partyUser.getRealizedGain()
							+",\"longPosition\":"
							+partyUser.getLongPosition()
							+",\"longAvgPrice\":"
							+partyUser.getLongAvgPrice()
							+",\"shortPosition\":"
							+partyUser.getShortPosition()
							+",\"shortAvgPrice\":"
							+partyUser.getShortAvgPrice()
							+"}"
							);
					scoreUpdate.put(traderId, score);

					
					tradeVol+=lastVol;
					tradeValue+=(lastVol*lastPrice);
					
					lastTradeId++;
					Trade tradeCacheEntry=new Trade(lastTradeId,System.currentTimeMillis(),
													lastVol,lastPrice,
													side==Constants.BUY?traderId:cpartyTraderId,
													side==Constants.SELL?traderId:cpartyTraderId);
					trade.setProperty("tradeId", tradeCacheEntry.getTradeId());
					trade.setProperty("tradeTime", tradeCacheEntry.getTradeTime());
					trade.setProperty("qty", tradeCacheEntry.getQty());
					trade.setProperty("price", tradeCacheEntry.getPrice());
					trade.setProperty("buyerId", tradeCacheEntry.getBuyerId());
					trade.setProperty("sellerId", tradeCacheEntry.getSellerId());
					
					tradeCount++;
					
					updatedOrders.add(res);
					updatedOrders.add(trade);
					//if there are multiple orders for a trader
					//append these order details to the existing JSON
					if (orderUpdate.containsKey(cpartyTraderId)){
						oldOrderData=orderUpdate.get(cpartyTraderId);
						oldOrderData.delete(oldOrderData.lastIndexOf("]"),oldOrderData.length());
						
						oldOrderData.append(",");
						oldOrderData.append(orderData);
						oldOrderData.append("]");
						orderUpdate.put(cpartyTraderId, oldOrderData);
						
					}
					else{
						orderData.insert(0, "{\"type\":\"ORDER\",\"order\":[");
						orderData.append("]");
						orderUpdate.put(cpartyTraderId,orderData);
					}
					
					if (mktDataCacheAvailable){
						availableVolAtThisPrx=(side==Constants.BUY?askMap:bidMap).get(lastPrice)-lastVol;
						if (availableVolAtThisPrx==0){
							(side==Constants.BUY?askMap:bidMap).remove(lastPrice);
						}
						else{
							(side==Constants.BUY?askMap:bidMap).put(lastPrice, availableVolAtThisPrx);
						}
						
					}
					
					if (tradeCacheAvailable){
						tradeList.add(tradeCacheEntry);
					}
					
					if (temp==0){
						break;
					}
				}
				
			}

			
			if (tradeVol>0){
				
				endTradeId=lastTradeId;
				
				users.put(traderId, partyUser);
				updateUserScore.put(traderId, partyUser);
				
				Query scq=new Query("User");
				scq.setAncestor(ob.getKey());
				scq.addFilter("traderId", FilterOperator.IN, updateUserScore.keySet());
				PreparedQuery scpq=ds.prepare(scq);
				for (Entity u : scpq.asIterable()){
					User aUser=updateUserScore.get((String)u.getProperty("traderId"));
					u.setProperty("longAvgPrice",aUser.getLongAvgPrice());
					u.setProperty("longCash",aUser.getLongCash());
					u.setProperty("longPosition",aUser.getLongPosition());
					u.setProperty("shortAvgPrice",aUser.getShortAvgPrice());
					u.setProperty("shortCash",aUser.getShortCash());
					u.setProperty("shortPosition",aUser.getShortPosition());
					u.setProperty("origRealizedGain",aUser.getOrigRealizedGain());
					u.setProperty("realizedGain",aUser.getRealizedGain());
					updatedOrders.add(u);
				}
			}
			
			lastOrderId++;
			orderCount++;
			
			Entity o = new Entity("Order",ob.getKey());
			o.setProperty("orderId", lastOrderId);
			o.setProperty("symbol",symbol);
			o.setProperty("side",side);
			o.setProperty("orderQty",partyOrderQty);
			o.setProperty("price",partyOrderPrice);
			o.setProperty("execQty",partyOrderQty-temp);
			o.setProperty("tradeValue",tradeValue);
			//if its an incomplete market order, cancel it
			o.setProperty("status",temp>0?(partyOrderPrice>0?Constants.ORD_ACK:Constants.ORD_CXL):Constants.ORD_EXEC);
			o.setProperty("orderTime",System.currentTimeMillis());
			o.setProperty("traderId",traderId);
			updatedOrders.add(o);

			orderData=new StringBuffer("{\"orderId\":"+lastOrderId
										+",\"side\":"
										+(side==Constants.BUY?"\"B\"":"\"S\"")
										+",\"limitPrice\":"+new DecimalFormat("#0.00").format(partyOrderPrice)
										+",\"orderQty\":"+partyOrderQty
										+",\"execQty\":"+(partyOrderQty-temp)
										+",\"avgPrice\":"+new DecimalFormat("#0.00").format(tradeVol>0?(tradeValue/tradeVol):0)
										+",\"status\":"
										+(temp>0?(partyOrderPrice>0?"\"Working\"}":"\"Eliminated\"}"):"\"Done\"}")
									);

			if (orderUpdate.containsKey(traderId)){
				oldOrderData=orderUpdate.get(traderId);
				oldOrderData.delete(oldOrderData.lastIndexOf("]"),oldOrderData.length());
				
				oldOrderData.append(",");
				oldOrderData.append(orderData);
				oldOrderData.append("]");
				orderUpdate.put(traderId, oldOrderData);
				
			}
			else{
				orderData.insert(0, "{\"type\":\"ORDER\",\"order\":[");
				orderData.append("]");
				orderUpdate.put(traderId,orderData);
			}
			
			//Order matching ends
			ostats.setProperty("orderCount", orderCount);
			ostats.setProperty("lastOrderId", lastOrderId);
			ostats.setProperty("lastTradeId", lastTradeId);
			ostats.setProperty("tradeCount", tradeCount);
			
			float changeFromClose=lastPrice-closePrice;
			float changeFromClosePercent=(changeFromClose/closePrice)*100;

			if (tradeVol>0){

				if (lastPrice>highPrice){
					highPrice=lastPrice;
					ostats.setProperty("highPrice", lastPrice);
				}
				if (lastPrice<lowPrice){
					lowPrice=lastPrice;
					ostats.setProperty("lowPrice", lastPrice);
				}
				ostats.setProperty("lastVolume", lastVol);
				ostats.setProperty("lastPrice", lastPrice);
				ostats.setProperty("changeFromClose", changeFromClose);
				ostats.setProperty("changeFromClosePercent", changeFromClosePercent);
				
				ostats.setProperty("dayVolume", oldDayVol+tradeVol);
				ostats.setProperty("dayValue", oldDayValue+tradeValue);
			}
			updatedOrders.add(ostats);

			txn = ds.beginTransaction();

			ds.put(updatedOrders);

			if (tradeVol>0){
				userCache.put(Constants.UserType.TRADER, users);
			}
			
			ChannelService chService=ChannelServiceFactory.getChannelService();
			
			//Send order update to party and counterparties
			for (Map.Entry<String,StringBuffer> entry : orderUpdate.entrySet()){
				StringBuffer stemp=new StringBuffer(entry.getValue());
				if (scoreUpdate.containsKey(entry.getKey())){
					stemp.append(scoreUpdate.get(entry.getKey()));
				}
				stemp.append("}");
				//do not send update to bots
				if (!entry.getKey().contains("BOT")){
					chService.sendMessage(new ChannelMessage(entry.getKey(),stemp.toString()));
				}
				//log.info("TraderId:"+entry.getKey()+" Message:"+stemp.toString());
			}
			
			txn.commit();
			
			//update market data
			
			if (mktDataCacheAvailable){
				if (temp>0 && partyOrderPrice>0){
					(side==Constants.BUY?bidMap:askMap).put(partyOrderPrice, temp+currQty);
				}
				mktDataCache.put(Constants.MktDataStatistic.BID, bidMap);
				mktDataCache.put(Constants.MktDataStatistic.ASK, askMap);
				
				if (tradeVol>0){
					mktDataCache.put(Constants.MktDataStatistic.LAST_VOLUME, lastVol);
					mktDataCache.put(Constants.MktDataStatistic.LAST_PRICE, lastPrice);
					mktDataCache.put(Constants.MktDataStatistic.HIGH_PRICE, highPrice);
					mktDataCache.put(Constants.MktDataStatistic.LOW_PRICE, lowPrice);
					mktDataCache.put(Constants.MktDataStatistic.CHANGE_FROM_CLOSE, changeFromClose);
					mktDataCache.put(Constants.MktDataStatistic.CHANGE_FROM_CLOSE_PERCENT, changeFromClosePercent);
					mktDataCache.put(Constants.MktDataStatistic.DAY_VOLUME,oldDayVol+tradeVol);
					mktDataCache.put(Constants.MktDataStatistic.DAY_VALUE,oldDayValue+tradeValue);
				}
			}
			
			if (tradeCacheAvailable){
				mktDataCache.put(Constants.MktDataStatistic.ONX_TRADE, tradeList);
			}
			

		}
		catch (NullPointerException npe){
			if (bidMap==null){
				log.log(Level.SEVERE, "bidMap missing", npe);
			}
			else if (askMap==null){
				log.log(Level.SEVERE, "askMap missing", npe);
			}
			else{
				log.log(Level.SEVERE, "bidMap,askMap OK, misc issue", npe);
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
					mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
					mdQueue.add(
							withUrl("/tasks/processMarketData.do")
							.param("symbol",symbol)
							.param("reloadCache", mktDataCacheAvailable?"N":"Y")
							.param("startTradeId", ((Long)startTradeId).toString())
							.param("endTradeId", ((Long)endTradeId).toString())
							.param("scores", tradeVol>0?"Y":"N")
							.method(Method.POST)
					);
				}
			}
		}
}
	
		

}
