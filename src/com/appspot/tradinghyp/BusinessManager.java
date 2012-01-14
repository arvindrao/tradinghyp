/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package com.appspot.tradinghyp;

import java.util.List;
import java.util.Map;

/**
 * @author Arvind Rao
 * 
 * Business Layer
 */
public interface BusinessManager {

	public void setDao(GenericDao dao);

	//Business methods
	public Map<Long, User> getActiveUsers();
	public long addNewUser(String userName) throws Exception;
	public boolean removeUser(long userId) throws Exception;
	public Map<Long,Long> getActiveBids();
	public Map<Long,Long> getActiveOffers();
	public TradeStats getTradeStats(); 
	public List<Trade> getTrades(long startTradeId,long endTradeId);
	public Map<Long,StringBuffer> processNewOrder(String symbol,short side,long partyOrderQty,long partyOrderPrice,long userId) throws Exception;
	public String processCancelOrder(long orderId, long userId) throws Exception;
}
