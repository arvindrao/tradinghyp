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
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arvind Rao
 *
 * Make a market by placing a quote around the last trade price 
 */

@SuppressWarnings("serial")
public class MarketMakerBot extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(MarketMakerBot.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		try{
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);

			CacheManager mcacheMgr = CacheManager.getInstance();
			long lastPrice=0;
			TradeStats tStats=null;
			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");

			if (mktDataCache != null){
				tStats=(TradeStats)mktDataCache.get(Constants.MktDataStatistic.TRADE_STATS);
			}

			if (tStats==null){
				tStats=bMgr.getTradeStats();
			}

			lastPrice=tStats.getLastPrice();

			Queue oQueue= QueueFactory.getQueue("OrderTaskQueue");

			Long ordSize=(Constants.MIN_MM_BOT_QTY+Math.round(Math.random()*(Constants.MAX_MM_BOT_QTY-Constants.MIN_MM_BOT_QTY)))*Constants.LOT_SIZE;
			//place a bid
			Long ordPrice=lastPrice-Constants.TICK_SIZE;
			log.info(Constants.BOT1_USER_ID+" BUYS "+ordSize+"@"+ordPrice);
			if (ordPrice>0){
				oQueue.add(withUrl("/tasks/processNewOrder.do")
						.param("symbol","HYP")
						.param("side","0")
						.param("qty",ordSize.toString())
						.param("price",ordPrice.toString())
						.param("userId", Constants.BOT1_USER_ID)
						.countdownMillis(2000)
						.method(Method.POST));
			}
			//place an offer
			ordPrice=lastPrice+Constants.TICK_SIZE;
			if (ordPrice>0){
				log.info(Constants.BOT1_USER_ID+" SELLS "+ordSize+"@"+ordPrice);
				oQueue.add(withUrl("/tasks/processNewOrder.do")
						.param("symbol","HYP")
						.param("side","1")
						.param("qty",ordSize.toString())
						.param("price",ordPrice.toString())
						.param("userId", Constants.BOT1_USER_ID)
						.countdownMillis(4000)
						.method(Method.POST));
			}

		}
		catch (Exception e){
			log.error("EXCEPTION", e);
		}
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doPost(req,resp);
	}

}
