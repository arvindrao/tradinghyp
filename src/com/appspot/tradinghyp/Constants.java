/**
 * 
 */
package com.appspot.tradinghyp;

/**
 * @author Arvind Rao
 *
 */
public final class Constants {
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
	
	public static String OB_KEY="OB1";
	
	public enum OrderStatistic {ORDER_COUNT,LAST_ORDER_ID,LAST_TRADER_ID,ACTIVE_TRADER_COUNT,LAST_TRADE_ID,TRADE_COUNT};
	
	//mkt data
	public static float INIT_CLOSE_PRICE=200.0F;
	public static float INIT_OPEN_PRICE=201.0F;
	public static long LOT_SIZE=100L;
	public static float TICK_SIZE=0.01F;
	
	public enum MktDataStatistic {BID,ASK,ONX_TRADE,OFFX_TRADE,
								CLOSE_PRICE,OPEN_PRICE,HIGH_PRICE,LOW_PRICE,
								LAST_PRICE,LAST_VOLUME,DAY_VOLUME,DAY_VALUE,
								CHANGE_FROM_CLOSE,CHANGE_FROM_CLOSE_PERCENT};
	
	public enum UserType {TRADER};
	
	public static long MIN_MM_BOT_QTY=1;
	public static long MAX_MM_BOT_QTY=5;
	public static long MIN_MT_BOT_QTY=1;
	public static long MAX_MT_BOT_QTY=5;
	
	public static String BOT1_TRADER_ID="BOT1";
	public static String BOT2_TRADER_ID="BOT2";
	
	private Constants(){
		throw new AssertionError();
		
	}
	
	
}
