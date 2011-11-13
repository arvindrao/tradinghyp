/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;
import java.io.IOException;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Arvind Rao
 *
 * Make a market by placing a quote around the last trade price 
 */

@SuppressWarnings("serial")
public class MarketMakerBot extends HttpServlet {
	private static final Logger log = Logger.getLogger(MarketMakerBot.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		try{
			CacheManager mcacheMgr = CacheManager.getInstance();
			Float lastPrice=null;
			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");

			if (mktDataCache != null){
				lastPrice=(Float)mktDataCache.get(Constants.MktDataStatistic.LAST_PRICE);
			}

			if (lastPrice==null){
				DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
				PreparedQuery pq=null,obPQ=null;
				Entity ob=null,ostats=null;
				Query q=null, obQ=null;
				obQ=new Query("OrderBook");
				obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
				obPQ = ds.prepare(obQ);
				ob=obPQ.asIterator().next();

				q=new Query("OrderStats");
				q.setAncestor(ob.getKey());
				q.addFilter("active",Query.FilterOperator.EQUAL,true);
				pq=ds.prepare(q);
				ostats=pq.asIterator().next();

				lastPrice=((Double)ostats.getProperty("lastPrice")).floatValue();
			}

			Queue oQueue= QueueFactory.getQueue("OrderTaskQueue");

			Long ordSize=(Constants.MIN_MM_BOT_QTY+Math.round(Math.random()*(Constants.MAX_MM_BOT_QTY-Constants.MIN_MM_BOT_QTY)))*Constants.LOT_SIZE;
			//place a bid
			Float ordPrice=lastPrice-Constants.TICK_SIZE;
			log.info(Constants.BOT1_TRADER_ID+" BUYS "+ordSize+"@"+ordPrice);
			if (ordPrice>0){
				oQueue.add(withUrl("/tasks/processNewOrder.do")
						.param("symbol","HYP")
						.param("side","0")
						.param("qty",ordSize.toString())
						.param("price",ordPrice.toString())
						.param("traderId", Constants.BOT1_TRADER_ID)
						.countdownMillis(2000)
						.method(Method.POST));
			}
			//place an offer
			ordPrice=lastPrice+Constants.TICK_SIZE;
			if (ordPrice>0){
				log.info(Constants.BOT1_TRADER_ID+" SELLS "+ordSize+"@"+ordPrice);
				oQueue.add(withUrl("/tasks/processNewOrder.do")
						.param("symbol","HYP")
						.param("side","1")
						.param("qty",ordSize.toString())
						.param("price",ordPrice.toString())
						.param("traderId", Constants.BOT1_TRADER_ID)
						.countdownMillis(4000)
						.method(Method.POST));
			}

		}
		catch (Exception e){
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doPost(req,resp);
	}

}
