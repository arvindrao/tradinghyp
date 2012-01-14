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
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import javax.servlet.http.*;
import com.google.appengine.api.channel.*;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Arvind Rao
 *
 * Send market data update to user(s) using the cache (if available), or the database
 */

@SuppressWarnings("serial")
public class MarketDataTaskProcessor extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(MarketDataTaskProcessor.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		String reloadCache=req.getParameter("reloadCache")!=null?req.getParameter("reloadCache"):"N";
		String userId=req.getParameter("userId");
		String scores=req.getParameter("scores")!=null?req.getParameter("scores"):"N";
		Long startTradeId=req.getParameter("startTradeId")!=null?new Long(req.getParameter("startTradeId")):0;
		Long endTradeId=req.getParameter("endTradeId")!=null?new Long(req.getParameter("endTradeId")):0;
		
		Map props =null;
		try{
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);
			CacheManager mcacheMgr = CacheManager.getInstance();

			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");
			if (mktDataCache == null){
			    props = new HashMap();
			    props.put(GCacheFactory.EXPIRATION_DELTA, Constants.CACHE_EXPIRATION);
				mktDataCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("MarketDataCache", mktDataCache);
			}

			Map<Long,Long> bidMap=(Map<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
			if (reloadCache.equals("Y") || bidMap==null){
				log.warn("Reloading bid cache from order book");
				bidMap=bMgr.getActiveBids();
				mktDataCache.put(Constants.MktDataStatistic.BID,bidMap);
			}
			
			Map<Long,Long> askMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);;
			if (reloadCache.equals("Y") || askMap==null){
				log.warn("Reloading ask cache from order book");
				askMap=bMgr.getActiveOffers();
				mktDataCache.put(Constants.MktDataStatistic.ASK,askMap);
			}

			List<Trade> tradeList=(ArrayList<Trade>)mktDataCache.get(Constants.MktDataStatistic.ONX_TRADE);
			if (tradeList==null){
				if (startTradeId==0 || endTradeId==0 || startTradeId>endTradeId){
					tradeList=new ArrayList<Trade>();
				}
				else{
					log.warn("Reloading trades from trade book");
					tradeList=bMgr.getTrades(startTradeId,endTradeId);
				}
			}

			TradeStats tStats=(TradeStats)mktDataCache.get(Constants.MktDataStatistic.TRADE_STATS);
			
			if (tStats==null){
				tStats=bMgr.getTradeStats();
				mktDataCache.put(Constants.MktDataStatistic.TRADE_STATS,tStats);
			}
			
			long dayVWAP=tStats.getDayVolume()>0?(tStats.getDayValue()/tStats.getDayVolume()):0;

			//Build JSON string representing trade stats
			StringBuffer quotes=new StringBuffer("{\"type\":\"MKTDATA\"");
			quotes.append(",\"closePrice\":"+tStats.getClosePrice());
			quotes.append(",\"openPrice\":"+tStats.getOpenPrice());
			quotes.append(",\"highPrice\":"+tStats.getHighPrice());
			quotes.append(",\"lowPrice\":"+tStats.getLowPrice());
			quotes.append(",\"changeFromClose\":"+tStats.getChangeFromClose());
			quotes.append(",\"changeFromClosePercent\":"+tStats.getChangeFromClosePercent());
			quotes.append(",\"lastPrice\":"+tStats.getLastPrice());
			quotes.append(",\"lastVol\":"+tStats.getLastVolume());
			quotes.append(",\"dayVWAP\":"+dayVWAP);
			quotes.append(",\"dayVol\":"+tStats.getDayVolume());
			
			//Build JSON string representing best bids and asks
			quotes.append(",\"bid\":[");

			int i=1;
			for(Map.Entry<Long,Long> bid : bidMap.entrySet()){
				quotes.append("{\"price\":"+bid.getKey()+",\"vol\":"+bid.getValue()+"},");
				i++;
				if (i>Constants.QUOTE_COUNT){
					break;
				}
			}
			//insert 0s for remaining bids
			for (;i<=Constants.QUOTE_COUNT;i++){
				quotes.append("{\"price\":"+0+",\"vol\":"+0+"},");
			}

			quotes.deleteCharAt(quotes.lastIndexOf(","));
			quotes.append("],\"ask\":[");

			i=1;
			for(Map.Entry<Long,Long> ask : askMap.entrySet()){
				quotes.append("{\"price\":"+ask.getKey()+",\"vol\":"+ask.getValue()+"},");
				i++;
				if (i>Constants.QUOTE_COUNT){
					break;
				}
			}
			//insert 0s for remaining asks
			for (;i<=Constants.QUOTE_COUNT;i++){
				quotes.append("{\"price\":"+0+",\"vol\":"+0+"},");
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
									+",\"price\":"+t.getPrice()
									+",\"buyerId\":\""+t.getBuyerId()+"\""
									+",\"sellerId\":\""+t.getSellerId()+"\""
									+"},"
									);
				}
				quotes.deleteCharAt(quotes.lastIndexOf(","));
				
			}
			quotes.append("]");
			
			ChannelService chService=ChannelServiceFactory.getChannelService();
			
			if (userId!=null){
				//send message to one trader
				quotes.append(",\"highScore\":[]}");
				//log.info(quotes.toString());
				chService.sendMessage(new ChannelMessage(userId,quotes.toString()));
				log.info("Individual message to userId "+userId);
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
				Map<Long,User> users=(HashMap<Long,User>) userCache.get(Constants.UserType.TRADER);
				
				if (users==null){
					log.warn("Creating user cache");
					users=bMgr.getActiveUsers();
					if (users!=null){
						userCache.put(Constants.UserType.TRADER, users);	
					}
				}
				
				List<User> u=null;
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
							quotes.append("{\"userName\":\""+x.getUserName());
							quotes.append("\",\"userId\":\""+x.getUserId());
							quotes.append("\",\"score\":"+x.getRealizedGain());
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
				
				Iterator<Long> it2 = users.keySet().iterator();
				Long uId;
				while(it2.hasNext()){
					uId=it2.next();
					//log.info("MarketDataProcessor: Broadcast message to traderId "+traderId);
					//do not send update to bots (identified by user Ids less than 100)
					if (uId>100){
						chService.sendMessage(new ChannelMessage(uId.toString(),quotes.toString()));
					}
				}

			}
			
			//remove the trade cache as we don't want to keep it except for this order update
			mktDataCache.remove(Constants.MktDataStatistic.ONX_TRADE);

		}
		catch (CacheException e) {
			log.error( "EXCEPTION", e);
		}
		catch (Exception e) {
			log.error( "EXCEPTION", e);
		}		
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doPost(req,resp);
	}

}