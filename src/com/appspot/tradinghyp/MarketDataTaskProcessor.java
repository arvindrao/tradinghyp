/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.io.IOException;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;
import java.text.DecimalFormat;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.channel.*;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Arvind Rao
 *
 * Send market data update to user(s) using the cache (if available), or the datastore
 */

@SuppressWarnings("serial")
public class MarketDataTaskProcessor extends HttpServlet {
	private static final Logger log = Logger.getLogger(MarketDataTaskProcessor.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		String reloadCache=req.getParameter("reloadCache")!=null?req.getParameter("reloadCache"):"N";
		String traderId=req.getParameter("traderId");
		String scores=req.getParameter("scores")!=null?req.getParameter("scores"):"N";
		Long startTradeId=req.getParameter("startTradeId")!=null?new Long(req.getParameter("startTradeId")):0;
		Long endTradeId=req.getParameter("endTradeId")!=null?new Long(req.getParameter("endTradeId")):0;
		
		Map props =null;
		try{
			CacheManager mcacheMgr = CacheManager.getInstance();

			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");
			if (mktDataCache == null){
			    props = new HashMap();
			    props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
				mktDataCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("MarketDataCache", mktDataCache);
			}

			TreeMap<Float,Long> bidMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
			if (reloadCache.equals("Y") || bidMap==null){
				log.warning("Reloading bid cache from order book");
				bidMap=Util.getBidsFromOrderBook();
				mktDataCache.put(Constants.MktDataStatistic.BID,bidMap);
			}
			
			TreeMap<Float,Long> askMap=(TreeMap<Float,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);;
			if (reloadCache.equals("Y") || askMap==null){
				log.warning("Reloading ask cache from order book");
				askMap=Util.getAsksFromOrderBook();
				mktDataCache.put(Constants.MktDataStatistic.ASK,askMap);
			}

			ArrayList<Trade> tradeList=(ArrayList<Trade>)mktDataCache.get(Constants.MktDataStatistic.ONX_TRADE);

			if (tradeList==null){
				if (startTradeId==0 || endTradeId==0 || startTradeId>endTradeId){
					tradeList=new ArrayList<Trade>();
				}
				else{
					log.warning("Reloading trades from trade book");
					tradeList=Util.getTradesFromTradeBook(startTradeId,endTradeId);
				}
			}
			
			Float closePrice=(Float)mktDataCache.get(Constants.MktDataStatistic.CLOSE_PRICE);
			Float openPrice=(Float)mktDataCache.get(Constants.MktDataStatistic.OPEN_PRICE);
			Float highPrice=(Float)mktDataCache.get(Constants.MktDataStatistic.HIGH_PRICE);
			Float lowPrice=(Float)mktDataCache.get(Constants.MktDataStatistic.LOW_PRICE);
			Float changeFromClose=(Float)mktDataCache.get(Constants.MktDataStatistic.CHANGE_FROM_CLOSE);
			Float changeFromClosePercent=(Float)mktDataCache.get(Constants.MktDataStatistic.CHANGE_FROM_CLOSE_PERCENT);
			Float lastPrice=(Float)mktDataCache.get(Constants.MktDataStatistic.LAST_PRICE);
			Long lastVol=(Long)mktDataCache.get(Constants.MktDataStatistic.LAST_VOLUME);
			Double dayValue=(Double)mktDataCache.get(Constants.MktDataStatistic.DAY_VALUE);
			Long dayVol=(Long)mktDataCache.get(Constants.MktDataStatistic.DAY_VOLUME);
			
			if (lastPrice==null || lastVol==null 
				|| closePrice==null || openPrice==null
				|| highPrice==null  || lowPrice==null
				|| dayValue==null || dayVol==null
				|| changeFromClose==null || changeFromClosePercent==null){
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

				highPrice=((Double)ostats.getProperty("highPrice")).floatValue();
				lowPrice=((Double)ostats.getProperty("lowPrice")).floatValue();
				openPrice=((Double)ostats.getProperty("openPrice")).floatValue();
				closePrice=((Double)ostats.getProperty("closePrice")).floatValue();
				changeFromClose=((Double)ostats.getProperty("changeFromClose")).floatValue();
				changeFromClosePercent=((Double)ostats.getProperty("changeFromClosePercent")).floatValue();

				lastVol=(Long)ostats.getProperty("lastVolume");
				lastPrice=((Double)ostats.getProperty("lastPrice")).floatValue();
				dayVol=(Long)ostats.getProperty("dayVolume");
				dayValue=(Double)ostats.getProperty("dayValue");

				mktDataCache.put(Constants.MktDataStatistic.CLOSE_PRICE,closePrice);
				mktDataCache.put(Constants.MktDataStatistic.OPEN_PRICE,openPrice);
				mktDataCache.put(Constants.MktDataStatistic.HIGH_PRICE,highPrice);
				mktDataCache.put(Constants.MktDataStatistic.LOW_PRICE,lowPrice);
				mktDataCache.put(Constants.MktDataStatistic.CHANGE_FROM_CLOSE,changeFromClose);
				mktDataCache.put(Constants.MktDataStatistic.CHANGE_FROM_CLOSE_PERCENT,changeFromClosePercent);
				mktDataCache.put(Constants.MktDataStatistic.LAST_PRICE,lastPrice);
				mktDataCache.put(Constants.MktDataStatistic.LAST_VOLUME,lastVol);
				mktDataCache.put(Constants.MktDataStatistic.DAY_VALUE,dayValue);
				mktDataCache.put(Constants.MktDataStatistic.DAY_VOLUME,dayVol);

			}
			
			Float dayVWAP=(float) ((dayVol>0)?(dayValue/dayVol):0);

			//Build JSON string representing trade stats
			StringBuffer quotes=new StringBuffer("{\"type\":\"MKTDATA\"");
			quotes.append(",\"closePrice\":"+new DecimalFormat("#0.00").format(closePrice));
			quotes.append(",\"openPrice\":"+new DecimalFormat("#0.00").format(openPrice));
			quotes.append(",\"highPrice\":"+new DecimalFormat("#0.00").format(highPrice));
			quotes.append(",\"lowPrice\":"+new DecimalFormat("#0.00").format(lowPrice));
			quotes.append(",\"changeFromClose\":"+new DecimalFormat("#0.00").format(changeFromClose));
			quotes.append(",\"changeFromClosePercent\":"+new DecimalFormat("#0.00").format(changeFromClosePercent));
			quotes.append(",\"lastPrice\":"+new DecimalFormat("#0.00").format(lastPrice));
			quotes.append(",\"lastVol\":"+lastVol.toString());
			quotes.append(",\"dayVWAP\":"+new DecimalFormat("#0.00").format(dayVWAP));
			quotes.append(",\"dayVol\":"+dayVol.toString());
			
			//Build JSON string representing best bids and asks
			quotes.append(",\"bid\":[");

			int i=1;
			for(Map.Entry<Float,Long> bid : bidMap.entrySet()){
				quotes.append("{\"price\":"+new DecimalFormat("#0.00").format(bid.getKey())+",\"vol\":"+bid.getValue()+"},");
				i++;
				if (i>Constants.QUOTE_COUNT){
					break;
				}
			}
			//insert 0s for remaining bids
			for (;i<=Constants.QUOTE_COUNT;i++){
				quotes.append("{\"price\":"+new DecimalFormat("#0.00").format(0)+",\"vol\":"+0+"},");
			}

			quotes.deleteCharAt(quotes.lastIndexOf(","));
			quotes.append("],\"ask\":[");

			i=1;
			for(Map.Entry<Float,Long> ask : askMap.entrySet()){
				quotes.append("{\"price\":"+new DecimalFormat("#0.00").format(ask.getKey())+",\"vol\":"+ask.getValue()+"},");
				i++;
				if (i>Constants.QUOTE_COUNT){
					break;
				}
			}
			//insert 0s for remaining asks
			for (;i<=Constants.QUOTE_COUNT;i++){
				quotes.append("{\"price\":"+new DecimalFormat("#0.00").format(0)+",\"vol\":"+0+"},");
			}

			quotes.deleteCharAt(quotes.lastIndexOf(","));
			quotes.append("],\"trade\":[");
			
			//Build trade update JSON
			
			if (!tradeList.isEmpty()){
				Iterator<Trade> it=tradeList.iterator();
				while (it.hasNext()){
					Trade t=it.next();
					Calendar cal=Calendar.getInstance();
					cal.setTimeInMillis(t.getTradeTime());
					quotes.append("{\"time\":\""
									+(cal.get(Calendar.HOUR_OF_DAY)<10?"0":"")+cal.get(Calendar.HOUR_OF_DAY)+":"
									+(cal.get(Calendar.MINUTE)<10?"0":"")+cal.get(Calendar.MINUTE)+":"
									+(cal.get(Calendar.SECOND)<10?"0":"")+cal.get(Calendar.SECOND)+"\","
								);
					quotes.append("\"qty\":"+t.getQty()
									+",\"price\":"+new DecimalFormat("#0.00").format(t.getPrice())
									+",\"buyerId\":\""+t.getBuyerId()+"\""
									+",\"sellerId\":\""+t.getSellerId()+"\""
									+"},"
									);
				}
				quotes.deleteCharAt(quotes.lastIndexOf(","));
				
			}
			quotes.append("]");
			
			ChannelService chService=ChannelServiceFactory.getChannelService();
			
			if (traderId!=null){
				//send message to one trader
				quotes.append(",\"highScore\":[]}");
				//log.info(quotes.toString());
				chService.sendMessage(new ChannelMessage(traderId,quotes.toString()));
				log.info("Individual message to traderId "+traderId);
			}
			else{
				//send message to all traders
				Cache userCache=mcacheMgr.getCache("UserCache");
				if (userCache == null){
					props = new HashMap();
					props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
					userCache = mcacheMgr.getCacheFactory().createCache(props);
					mcacheMgr.registerCache("UserCache", userCache);
				}
				HashMap<String,User> users=(HashMap<String,User>) userCache.get(Constants.UserType.TRADER);
				
				if (users==null){
					log.warning("Creating user cache");
					users=Util.fetchUsers();
					userCache.put(Constants.UserType.TRADER, users);
				}
				
				ArrayList<User> u=null;
				int hsCount=0;
				quotes.append(",\"highScore\":[");
				if (scores.equals("Y")){
					u=new ArrayList<User>(users.values());
					//sort users in descending order of score
					Collections.sort(u,Collections.reverseOrder());
					Iterator<User> it3 = u.iterator();
					while (it3.hasNext() && hsCount<=Constants.HIGH_SCORE_COUNT){
						User x=it3.next();
						if (x.getRealizedGain()>0){
							quotes.append("{\"userId\":\""+x.getUserId());
							quotes.append("\",\"traderId\":\""+x.getTraderId());
							quotes.append("\",\"score\":"+new DecimalFormat("#0.00").format(x.getRealizedGain()));
							quotes.append("},");
							hsCount++;
						}
						else{
							//reached users with low scores, break out
							break;
						}
					}
				}

				if (hsCount>0){
					quotes.deleteCharAt(quotes.lastIndexOf(","));
				}
				quotes.append("]}");
				
				//log.info(quotes.toString());
				
				Iterator<String> it2 = users.keySet().iterator();
				while(it2.hasNext()){
					traderId=it2.next();
					//log.info("MarketDataProcessor: Broadcast message to traderId "+traderId);
					//do not send update to bots
					if (!traderId.contains("BOT")){
						chService.sendMessage(new ChannelMessage(traderId,quotes.toString()));
					}
				}

			}
			
			//remove the trade cache as we don't want to keep it except for this order update
			mktDataCache.remove(Constants.MktDataStatistic.ONX_TRADE);

			//CacheStatistics cstats=mktDataCache.getCacheStatistics();
			//log.info(cstats.toString());
			
		}
		catch (CacheException e) {
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "EXCEPTION", e);
		}		
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doPost(req,resp);
	}

}