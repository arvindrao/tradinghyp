/**
 * 
 */
package com.appspot.tradinghyp;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import java.io.IOException;
import java.util.TreeMap;
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
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.channel.*;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @author Arvind Rao
 *
 */

@SuppressWarnings("serial")
public class CancelOrderTaskProcessor extends HttpServlet {
	private static final Logger log = Logger.getLogger(CancelOrderTaskProcessor.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//System.out.println("CancelOrderTaskProcessor: BEGIN");

		String symbol=req.getParameter("symbol");
		String traderId=req.getParameter("traderId");
		long orderId=new Long(req.getParameter("orderId"));

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn=null;
		PreparedQuery pq=null,obPQ=null;
		Query q=null, obQ=null;
		Queue mdQueue= null;
		boolean cancelPossible=false;
		boolean mktDataCacheAvailable=false;
		TreeMap<Float,Long> askMap=null,bidMap=null;
		long currQty=0;
		Entity ob=null,o=null;
		float price=0;
		double tradeValue=0;
		short side=0;
		long orderQty=0,execQty=0;
		CacheManager mcacheMgr = null;
		Cache mktDataCache=null;



		try{

			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			ob=obPQ.asIterator().next();

			q=new Query("Order");
			q.setAncestor(ob.getKey());
			q.addFilter("orderId", FilterOperator.EQUAL, orderId);
			q.addFilter("status", FilterOperator.EQUAL, Constants.ORD_ACK);
			pq=ds.prepare(q);
			o=pq.asIterator().next();

			if (o==null){
				//cannot cancel, order must have been completed
				cancelPossible=false;
			}
			else{
				//cancel
				side=((Long)o.getProperty("side")).shortValue();
				price=((Double)o.getProperty("price")).floatValue();
				orderQty=(Long)o.getProperty("orderQty");
				execQty=(Long)o.getProperty("execQty");
				tradeValue=(Double)o.getProperty("tradeValue");

				o.setProperty("status", Constants.ORD_CXL);

				cancelPossible=true;

				mcacheMgr = CacheManager.getInstance();
				mktDataCache=mcacheMgr.getCache("MarketDataCache");
				if (mktDataCache!=null){
					if (side==Constants.BUY){
						bidMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
						if (bidMap==null){
							mktDataCacheAvailable=false;
						}
						else{
							mktDataCacheAvailable=true;
							if (bidMap.containsKey(price)){
								currQty=bidMap.get(price)-(orderQty-execQty);
								if (currQty>0){
									bidMap.put(price,currQty);
								}
								else{
									bidMap.remove(price);
								}
							}
						}
					}
					else{
						askMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);
						if (askMap==null){
							mktDataCacheAvailable=false;
						}
						else{
							mktDataCacheAvailable=true;
							if (askMap.containsKey(price)){
								currQty=askMap.get(price)-(orderQty-execQty);
								if (currQty>0){
									askMap.put(price,currQty);
								}
								else{
									askMap.remove(price);
								}
							}
						}
					}
				}
				else{
					mktDataCacheAvailable=false;
				}
			}

			if (cancelPossible){
				String orderData=new String("{\"type\":\"ORDER\",\"order\":[{\"orderId\":"
						+orderId
						+",\"side\":"
						+(side==Constants.BUY?"\"B\"":"\"S\"")
						+",\"limitPrice\":"+new DecimalFormat("#0.00").format(price)
						+",\"orderQty\":"+orderQty
						+",\"execQty\":"+execQty
						+",\"avgPrice\":"+new DecimalFormat("#0.00").format(execQty>0?(tradeValue/execQty):0)
						+",\"status\":\"Cancelled\"}]}"
						);


				txn = ds.beginTransaction();
				ds.put(o);
				ChannelService chService=ChannelServiceFactory.getChannelService();
				//Send order update to party
				chService.sendMessage(new ChannelMessage(traderId,orderData));
				//System.out.println("TraderId:"+traderId+" Message:"+orderData);
				txn.commit();

				//update market data

				if (mktDataCacheAvailable){
					if (side==Constants.BUY){
						mktDataCache.put(Constants.MktDataStatistic.BID, bidMap);
					}
					else if (side==Constants.SELL){
						mktDataCache.put(Constants.MktDataStatistic.ASK, askMap);
					}

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
					mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
					mdQueue.add(
							withUrl("/tasks/processMarketData.do")
							.param("symbol",symbol)
							.param("reloadCache", mktDataCacheAvailable?"N":"Y")
							.method(Method.POST)
							);
				}
			}
		}
		//System.out.println("CancelOrderTaskProcessor: END");
	}
}

