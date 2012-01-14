/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

/**
 * @author Arvind Rao
 *
 * Global constants
 */
public final class Constants {
	
	public static final String ROOT_BEAN = "appManager";
	
	//Order status
	public static short ORD_PENDING=0;
	public static short ORD_ACK=1;
	public static short ORD_PART_EXEC=2;
	public static short ORD_EXEC=3;
	public static short ORD_REJ=4;
	public static short ORD_CXL=5;
	
	//Sides
	public static short BUY=0;
	public static short SELL=1;
	public static short SHORT_SELL=2;
	
	public static short QUOTE_COUNT=5;
	public static short HIGH_SCORE_COUNT=11;
	
	public enum OrderStatistic {ORDER_COUNT,LAST_ORDER_ID,LAST_TRADER_ID,ACTIVE_TRADER_COUNT,LAST_TRADE_ID,TRADE_COUNT};
	
	//mkt data
	public enum MktDataStatistic {BID,ASK,TRADE_STATS,ONX_TRADE};
	
	public enum UserType {TRADER};
	
	public static long MIN_MM_BOT_QTY=1;
	public static long MAX_MM_BOT_QTY=5;
	public static long MIN_MT_BOT_QTY=1;
	public static long MAX_MT_BOT_QTY=5;
	
	public static String BOT1_USER_ID="1";
	public static String BOT2_USER_ID="2";
	
	public static String QUERY_ACTIVE_USERS="ACTIVE_USERS";
	public static String QUERY_ACTIVE_BIDS="ACTIVE_BIDS";
	public static String QUERY_ACTIVE_OFFERS="ACTIVE_OFFERS";
	public static String QUERY_TRADES="TRADES";
	public static String QUERY_MATCH_BIDS="MATCH_BIDS";
	public static String QUERY_MATCH_OFFERS="MATCH_OFFERS";
	public static String QUERY_USER_ORDERS="USER_ORDERS";
	
	public static long LOT_SIZE=100L;
	public static long TICK_SIZE=1L;

	public static int CACHE_EXPIRATION=36000;
	
	private Constants(){
		throw new AssertionError();
		
	}
}
