/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.io.Serializable;

/**
 * @author Arvind Rao
 *
 * Represents a trade statistics entity.
 */
public class TradeStats implements Serializable {
	private static final long serialVersionUID = 1;
	
	private long tradeStatsId;
	private long closePrice;
	private long openPrice;
	private long highPrice;
	private long lowPrice;
	private long lastPrice;
	private long lastVolume;
	private long changeFromClose;
	private long changeFromClosePercent;
	private long dayValue;
	private long dayVolume;
	
	public TradeStats() {
	}

	/**
	 * @return the tradeStatsId
	 */
	public long getTradeStatsId() {
		return tradeStatsId;
	}

	/**
	 * @param tradeStatsId the tradeStatsId to set
	 */
	public void setTradeStatsId(long tradeStatsId) {
		this.tradeStatsId = tradeStatsId;
	}

	/**
	 * @return the changeFromClose
	 */
	public long getChangeFromClose() {
		return changeFromClose;
	}

	/**
	 * @param changeFromClose the changeFromClose to set
	 */
	public void setChangeFromClose(long changeFromClose) {
		this.changeFromClose = changeFromClose;
	}

	/**
	 * @return the changeFromClosePercent
	 */
	public long getChangeFromClosePercent() {
		return changeFromClosePercent;
	}

	/**
	 * @param changeFromClosePercent the changeFromClosePercent to set
	 */
	public void setChangeFromClosePercent(long changeFromClosePercent) {
		this.changeFromClosePercent = changeFromClosePercent;
	}

	/**
	 * @return the dayValue
	 */
	public long getDayValue() {
		return dayValue;
	}

	/**
	 * @param dayValue the dayValue to set
	 */
	public void setDayValue(long dayValue) {
		this.dayValue = dayValue;
	}

	/**
	 * @return the dayVolume
	 */
	public long getDayVolume() {
		return dayVolume;
	}

	/**
	 * @param dayVolume the dayVolume to set
	 */
	public void setDayVolume(long dayVolume) {
		this.dayVolume = dayVolume;
	}

	/**
	 * @return the closePrice
	 */
	public long getClosePrice() {
		return closePrice;
	}

	/**
	 * @param closePrice the closePrice to set
	 */
	public void setClosePrice(long closePrice) {
		this.closePrice = closePrice;
	}

	/**
	 * @return the openPrice
	 */
	public long getOpenPrice() {
		return openPrice;
	}

	/**
	 * @param openPrice the openPrice to set
	 */
	public void setOpenPrice(long openPrice) {
		this.openPrice = openPrice;
	}

	/**
	 * @return the highPrice
	 */
	public long getHighPrice() {
		return highPrice;
	}

	/**
	 * @param highPrice the highPrice to set
	 */
	public void setHighPrice(long highPrice) {
		this.highPrice = highPrice;
	}

	/**
	 * @return the lowPrice
	 */
	public long getLowPrice() {
		return lowPrice;
	}

	/**
	 * @param lowPrice the lowPrice to set
	 */
	public void setLowPrice(long lowPrice) {
		this.lowPrice = lowPrice;
	}

	/**
	 * @return the lastPrice
	 */
	public long getLastPrice() {
		return lastPrice;
	}

	/**
	 * @param lastPrice the lastPrice to set
	 */
	public void setLastPrice(long lastPrice) {
		this.lastPrice = lastPrice;
	}

	/**
	 * @return the lastVolume
	 */
	public long getLastVolume() {
		return lastVolume;
	}

	/**
	 * @param lastVolume the lastVolume to set
	 */
	public void setLastVolume(long lastVolume) {
		this.lastVolume = lastVolume;
	}


}
