/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package com.appspot.tradinghyp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.stereotype.Service;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arvind Rao
 * 
 * Implementation of the Business Layer
 */
@Service
public class BusinessManagerImpl implements BusinessManager {
	private static final Logger log = LoggerFactory.getLogger(BusinessManagerImpl.class);

	private GenericDao dao;

	public BusinessManagerImpl(){
	}

	public static BusinessManager getInstance(){
		return null;
	}

	@Override
	public void setDao(GenericDao dao){
		this.dao=dao;
	}

	@Override
	@Transactional (readOnly=true)
	public Map<Long,User> getActiveUsers(){
		log.info("BEGIN");
		Map<Long,User> users=new HashMap<Long,User>();

		List<User> userList=this.dao.findTypedEntities(User.class, Constants.QUERY_ACTIVE_USERS);
		for (User aUser: userList){	
			users.put(aUser.getUserId(), aUser);
		}

		log.info("END");
		return users;
	}

	@Override
	@Transactional (rollbackFor={Exception.class})
	public long addNewUser(String userName) throws Exception{
		log.info("BEGIN");
		Map props=null;
		Map<Long,User> users=null;
		CacheManager mcacheMgr = CacheManager.getInstance();
		Cache userCache=mcacheMgr.getCache("UserCache");
		if (userCache == null){
			log.info("Creating user cache");
			props = new HashMap();
			props.put(GCacheFactory.EXPIRATION_DELTA, Constants.CACHE_EXPIRATION);
			userCache = mcacheMgr.getCacheFactory().createCache(props);
			mcacheMgr.registerCache("UserCache", userCache);
		}
		users=(HashMap<Long,User>) userCache.get(Constants.UserType.TRADER);

		if (users==null){
			users=getActiveUsers();
		}

		User u=new User(userName);
		u.setLoginTime(System.currentTimeMillis());
		long userId=this.dao.save(u);

		users.put(userId, u);
		userCache.put(Constants.UserType.TRADER, users);
		log.info("END");
		return userId;
	}

	@Override
	@Transactional (readOnly=true)
	public Map<Long, Long> getActiveBids() {
		log.info("BEGIN");
		//reverse sort order because best (higher) bids should be on top
		Map<Long,Long> bidMap=new TreeMap<Long,Long>(Collections.reverseOrder());

		//find active bids from backend
		List<Object[]> bids=dao.findNonEntities(Constants.QUERY_ACTIVE_BIDS);

		for (Object[] bid : bids){
			bidMap.put((Long)bid[0], (Long)bid[1]);
		}

		log.info("END");
		return bidMap;

	}

	@Override
	@Transactional (readOnly=true)
	public Map<Long, Long> getActiveOffers() {
		log.info("BEGIN");
		//sort in ascending order, best (lowest) ask price on top
		Map<Long,Long> askMap=new TreeMap<Long,Long>();

		//find active bids from backend
		List<Object[]> asks=dao.findNonEntities(Constants.QUERY_ACTIVE_OFFERS);

		for (Object[] ask : asks){
			askMap.put((Long)ask[0], (Long)ask[1]);
		}

		log.info("END");
		return askMap;
	}

	@Override
	@Transactional (readOnly=true)
	public TradeStats getTradeStats() {
		return dao.findEntity(TradeStats.class, 1);
	}

	@Override
	@Transactional (readOnly=true)
	public List<Trade> getTrades(long startTradeId, long endTradeId) {
		return dao.findTrades(startTradeId, endTradeId);
	}

	@Override
	@Transactional (rollbackFor={Exception.class})
	public Map<Long,StringBuffer> processNewOrder(String symbol, short side, long partyOrderQty,
			long partyOrderPrice, long userId) throws Exception{
		log.info("BEGIN");
		Map<Long,StringBuffer> orderUpdate = new HashMap<Long,StringBuffer>();
		Map<Long,String> scoreUpdate = new HashMap<Long,String>();
		boolean matchPossible=false;
		boolean mktDataCacheAvailable=false,tradeCacheAvailable=false;
		SortedMap<Long,Long> askMap=null,bidMap=null;
		List<Trade> tradeList=new ArrayList<Trade>();
		long orderQty,execQty,temp=partyOrderQty;
		long lastVol=0,availableVolAtThisPrx,currQty=0;
		long lastPrice = 0,lowPrice,highPrice,closePrice;
		long newOrderId=0L,orderId=0L,
				oldDayVol=0L;
		long oldDayValue=0;
		String score;
		long tradeVol=0;
		Map props =null;
		List<Order> oList=null;

		TradeStats tStats=getTradeStats();

		oldDayVol=tStats.getDayVolume();
		oldDayValue=tStats.getDayValue();
		highPrice=tStats.getHighPrice();
		lowPrice=tStats.getLowPrice();
		closePrice=tStats.getClosePrice();

		CacheManager mcacheMgr = CacheManager.getInstance();

		Cache userCache=mcacheMgr.getCache("UserCache");
		Map<Long,User> users=null;
		if (userCache != null){
			users=(HashMap<Long,User>) userCache.get(Constants.UserType.TRADER);
		}
		else{
			log.warn("Creating user cache");
			props = new HashMap();
			props.put(GCacheFactory.EXPIRATION_DELTA, Constants.CACHE_EXPIRATION);
			userCache = mcacheMgr.getCacheFactory().createCache(props);
			mcacheMgr.registerCache("UserCache", userCache);
		}
		if (users==null){
			users=getActiveUsers();
		}

		User partyUser=users.get(userId);

		Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");

		if (mktDataCache!=null){
			bidMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
			askMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);

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

		//Order matching begins
		if (side==Constants.BUY){
			if (mktDataCacheAvailable){
				//use the cache if available
				if (askMap.isEmpty()){
					//no offers
					matchPossible=false;
				}
				else if (partyOrderPrice>0 && askMap.firstKey()<=partyOrderPrice){
					//there is at least one offer which can be matched
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

				//if it's a mkt buy order, set order price to max so that all active offers potentially match
				oList=dao.findOffersToMatch((partyOrderPrice>0)?partyOrderPrice:99999999900L);
				if (oList.isEmpty()){
					matchPossible=false;
				}
				else{
					matchPossible=true;
				}

			}

		}
		else{
			//Sell order
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
				oList=dao.findBidsToMatch(partyOrderPrice);
				if (oList.isEmpty()){
					matchPossible=false;
				}
				else{
					matchPossible=true;
				}

			}

		}

		long cpartyTradeValue=0;
		long tradeValue=0;
		long cpartyUserId;
		StringBuffer orderData=null;
		StringBuffer oldOrderData=null;

		if (matchPossible){
			for (Order o : oList){

				new Trade();
				orderQty=o.getOrderQty();
				execQty=o.getExecQty();
				lastPrice=o.getPrice();
				cpartyTradeValue=o.getTradeValue();
				orderId=o.getOrderId();
				short cpartySide=o.getSide();
				//Build order data update JSON
				cpartyUserId=o.getUserId();
				orderData=new StringBuffer("{\"orderId\":"+orderId
						+",\"side\":"+(cpartySide==Constants.BUY?"\"B\"":"\"S\"")
						+",\"limitPrice\":"+lastPrice
						+",\"orderQty\":"+orderQty);

				if (temp>orderQty-execQty){
					lastVol=orderQty-execQty;
					temp-=(orderQty-execQty);
					cpartyTradeValue+=lastVol*lastPrice;
					o.setExecQty(orderQty);
					o.setStatus(Constants.ORD_EXEC);
					o.setTradeValue(cpartyTradeValue);
					orderData.append(",\"execQty\":"+orderQty
							+",\"avgPrice\":"+(cpartyTradeValue/orderQty)
							+",\"status\":\"Done\"}");

				}
				else if (temp==orderQty-execQty){
					lastVol=orderQty-execQty;
					temp=0;
					cpartyTradeValue+=lastVol*lastPrice;
					o.setExecQty(orderQty);
					o.setStatus(Constants.ORD_EXEC);
					o.setTradeValue(cpartyTradeValue);
					orderData.append(",\"execQty\":"+orderQty
							+",\"avgPrice\":"+(cpartyTradeValue/orderQty)
							+",\"status\":\"Done\"}");

				}
				else if (temp<orderQty-execQty){
					lastVol=temp;
					cpartyTradeValue+=lastVol*lastPrice;
					o.setExecQty(execQty+temp);
					//dont change status, still working
					o.setTradeValue(cpartyTradeValue);
					orderData.append(",\"execQty\":"+(execQty+temp)
							+",\"avgPrice\":"+(cpartyTradeValue/(execQty+temp))
							+",\"status\":\"Working\"}");
					temp=0;

				}

				if (userId != cpartyUserId){
					User cpartyUser=users.get(cpartyUserId);
					//if user logged off and this is their order
					//create a new dummy user to handle gracefully
					if (cpartyUser==null){
						cpartyUser=new User(cpartyUserId,"Logged Off");
					}
					cpartyUser.updateScore(cpartySide, lastVol, lastPrice);
					users.put(cpartyUserId, cpartyUser);
					dao.update(cpartyUser);

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
					scoreUpdate.put(cpartyUserId, score);
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
				scoreUpdate.put(userId, score);

				tradeVol+=lastVol;
				tradeValue+=(lastVol*lastPrice);

				Trade tradeCacheEntry=new Trade(System.currentTimeMillis(),
						lastVol,lastPrice,
						side==Constants.BUY?userId:cpartyUserId,
								side==Constants.SELL?userId:cpartyUserId);

				//persist the updated counterparty order
				dao.update(o);
				//persist the trade
				dao.save(tradeCacheEntry);
				
				//if there are multiple orders for a trader
				//append these order details to the existing JSON
				if (orderUpdate.containsKey(cpartyUserId)){
					oldOrderData=orderUpdate.get(cpartyUserId);
					oldOrderData.delete(oldOrderData.lastIndexOf("]"),oldOrderData.length());

					oldOrderData.append(",");
					oldOrderData.append(orderData);
					oldOrderData.append("]");
					orderUpdate.put(cpartyUserId, oldOrderData);

				}
				else{
					orderData.insert(0, "{\"type\":\"ORDER\",\"order\":[");
					orderData.append("]");
					orderUpdate.put(cpartyUserId,orderData);
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
			users.put(userId, partyUser);
			dao.update(partyUser);
		}

		Order newOrder = new Order();
		newOrder.setSymbol(symbol);
		newOrder.setSide(side);
		newOrder.setOrderQty(partyOrderQty);
		newOrder.setPrice(partyOrderPrice);
		newOrder.setExecQty(partyOrderQty-temp);
		newOrder.setTradeValue(tradeValue);
		//if its an incomplete market order, cancel it
		newOrder.setStatus(temp>0?(partyOrderPrice>0?Constants.ORD_ACK:Constants.ORD_CXL):Constants.ORD_EXEC);
		newOrder.setOrderTime(System.currentTimeMillis());
		newOrder.setUserId(userId);
		newOrderId=dao.save(newOrder);

		orderData=new StringBuffer("{\"orderId\":"+newOrderId
				+",\"side\":"
				+(side==Constants.BUY?"\"B\"":"\"S\"")
				+",\"limitPrice\":"+partyOrderPrice
				+",\"orderQty\":"+partyOrderQty
				+",\"execQty\":"+(partyOrderQty-temp)
				+",\"avgPrice\":"+(tradeVol>0?(tradeValue/tradeVol):0)
				+",\"status\":"
				+(temp>0?(partyOrderPrice>0?"\"Working\"}":"\"Eliminated\"}"):"\"Done\"}")
				);

		if (orderUpdate.containsKey(userId)){
			oldOrderData=orderUpdate.get(userId);
			oldOrderData.delete(oldOrderData.lastIndexOf("]"),oldOrderData.length());

			oldOrderData.append(",");
			oldOrderData.append(orderData);
			oldOrderData.append("]");
			orderUpdate.put(userId, oldOrderData);

		}
		else{
			orderData.insert(0, "{\"type\":\"ORDER\",\"order\":[");
			orderData.append("]");
			orderUpdate.put(userId,orderData);
		}

		//Order matching ends

		long changeFromClose=lastPrice-closePrice;
		//convert to bps to remove decimals
		long changeFromClosePercent=(changeFromClose*10000)/closePrice;

		if (tradeVol>0){

			if (lastPrice>highPrice){
				highPrice=lastPrice;
				tStats.setHighPrice(lastPrice);
			}
			if (lastPrice<lowPrice){
				lowPrice=lastPrice;
				tStats.setLowPrice(lastPrice);
			}
			tStats.setLastVolume(lastVol);
			tStats.setLastPrice(lastPrice);
			tStats.setChangeFromClose(changeFromClose);
			tStats.setChangeFromClosePercent(changeFromClosePercent);
			tStats.setDayVolume(oldDayVol+tradeVol);
			tStats.setDayValue(oldDayValue+tradeValue);
		}

		dao.update(tStats);

		if (tradeVol>0){
			userCache.put(Constants.UserType.TRADER, users);
		}

		if (mktDataCacheAvailable){
			if (temp>0 && partyOrderPrice>0){
				(side==Constants.BUY?bidMap:askMap).put(partyOrderPrice, temp+currQty);
			}
			mktDataCache.put(Constants.MktDataStatistic.BID, bidMap);
			mktDataCache.put(Constants.MktDataStatistic.ASK, askMap);

			if (tradeVol>0){
				mktDataCache.put(Constants.MktDataStatistic.TRADE_STATS, tStats);
			}
		}

		if (tradeCacheAvailable){
			mktDataCache.put(Constants.MktDataStatistic.ONX_TRADE, tradeList);
		}

		for (Map.Entry<Long,StringBuffer> entry : orderUpdate.entrySet()){
			StringBuffer stemp=new StringBuffer(entry.getValue());
			if (scoreUpdate.containsKey(entry.getKey())){
				stemp.append(scoreUpdate.get(entry.getKey()));
			}
			stemp.append("}");
			entry.setValue(stemp);
		}

		return orderUpdate;
	}

	@Override
	@Transactional (rollbackFor={Exception.class})
	public String processCancelOrder(long orderId, long userId
			) throws Exception{
		log.info("BEGIN");

		boolean cancelPossible=false;
		boolean mktDataCacheAvailable=false;
		long currQty=0;
		long price=0;
		long tradeValue=0;
		short side=0;
		long orderQty=0,execQty=0;
		String orderData=null;
		CacheManager mcacheMgr = null;
		Cache mktDataCache=null;
		SortedMap<Long, Long> bidMap=null, askMap=null;
		mcacheMgr = CacheManager.getInstance();
		mktDataCache=mcacheMgr.getCache("MarketDataCache");

		if (mktDataCache!=null){
				bidMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
				askMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);
				if (bidMap!=null && askMap!=null){
					mktDataCacheAvailable=true;
				}
		}

		Order o=dao.findEntity(Order.class, orderId);

		if (o==null){
			//cannot cancel, order must have been completed
			cancelPossible=false;
		}
		else{
			if (o.getUserId()!=userId) {
				throw new Exception("Security issue: UserId "+userId+" cannot cancel OrderId "+orderId);
			}
			else{
				//cancel
				side=o.getSide();
				price=o.getPrice();
				orderQty=o.getOrderQty();
				execQty=o.getExecQty();
				tradeValue=o.getTradeValue();

				o.setStatus(Constants.ORD_CXL);

				cancelPossible=true;

				if (mktDataCacheAvailable){
					if (side==Constants.BUY){
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
					else{
						//sell order
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
		}

		if (cancelPossible){
			//Create JSON message to send to party
			orderData=new String("{\"type\":\"ORDER\",\"order\":[{\"orderId\":"
					+orderId
					+",\"side\":"
					+(side==Constants.BUY?"\"B\"":"\"S\"")
					+",\"limitPrice\":"+price
					+",\"orderQty\":"+orderQty
					+",\"execQty\":"+execQty
					+",\"avgPrice\":"+(execQty>0?(tradeValue/execQty):0)
					+",\"status\":\"Cancelled\"}]}"
					);

			//save this order
			dao.save(o);

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
		log.info("END");
		return orderData;
	}

	@Override
	@Transactional (rollbackFor={Exception.class})
	public boolean removeUser(long userId) throws Exception {
		log.info("BEGIN");

		short side;
		boolean activeOrders=false;
		boolean mktDataCacheAvailable=false;
		SortedMap<Long,Long> askMap=null,bidMap=null;
		long orderQty,execQty;
		long lastVol=0,availableVolAtThisPrx;
		long lastPrice;

		log.info("UserId "+userId);

		CacheManager mcacheMgr = CacheManager.getInstance();
		Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");

		if (mktDataCache!=null){
			bidMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.BID);
			askMap=(TreeMap<Long,Long>)mktDataCache.get(Constants.MktDataStatistic.ASK);
			if (bidMap!=null && askMap!=null){
				mktDataCacheAvailable=true;
			}
		}

		User u=dao.findEntity(User.class, userId);
		if (u==null){
			log.error("User not found");
		}
		else{
			if (u.getLogoutTime()!=0){
				log.error("User already logged out");
			}
			else{
				u.setLogoutTime(System.currentTimeMillis());
				dao.update(u);
				log.info("Updated logoff time for User");
			}
		}
		
		Map<Long,User> users=null;
		Cache userCache=mcacheMgr.getCache("UserCache");
		if (userCache != null){
			users=(HashMap<Long,User>) userCache.get(Constants.UserType.TRADER);
			if (users!=null){
				users.remove(userId);
				userCache.put(Constants.UserType.TRADER, users);
				log.info("Removed from UserCache");
			}
		}

		List<Order> oList=dao.findUserOrders(userId);
		if (oList!=null){
			activeOrders=true;
		}
		else{
			activeOrders=false;
			log.info("No active orders");
		}

		if (activeOrders){
			//cancel orders
			for (Order o: oList){
				log.info("Cancelling orderId "+o.getOrderId());
				orderQty=o.getOrderQty();
				execQty=o.getExecQty();
				side=o.getSide();

				lastPrice=o.getPrice();
				lastVol=orderQty-execQty;
				o.setStatus(Constants.ORD_CXL);
				dao.update(o);

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
			
			//update market data
			if (mktDataCacheAvailable){
				mktDataCache.put(Constants.MktDataStatistic.BID, bidMap);
				mktDataCache.put(Constants.MktDataStatistic.ASK, askMap);
			}
		}
		log.info("END");
		return activeOrders;

	}
}
