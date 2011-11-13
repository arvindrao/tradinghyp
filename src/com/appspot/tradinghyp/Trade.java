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
 * Represents a trade entity
 */
public class Trade implements Serializable{
	private static final long serialVersionUID = 1;
	
	public Trade(long tradeId, long tradeTime, long qty, float price,
			String buyerId, String sellerId) {
		this.tradeId = tradeId;
		this.tradeTime = tradeTime;
		this.qty = qty;
		this.price = price;
		this.buyerId = buyerId;
		this.sellerId = sellerId;
	}

	public Trade() {
	}
	
	private long tradeId;
	private long tradeTime;
	private long qty;
	private float price;
	private String buyerId;
	private String sellerId;
	/**
	 * @return the tradeId
	 */
	public long getTradeId() {
		return tradeId;
	}
	/**
	 * @param tradeId the tradeId to set
	 */
	public void setTradeId(long tradeId) {
		this.tradeId = tradeId;
	}
	/**
	 * @return the tradeTime
	 */
	public long getTradeTime() {
		return tradeTime;
	}
	/**
	 * @param tradeTime the tradeTime to set
	 */
	public void setTradeTime(long tradeTime) {
		this.tradeTime = tradeTime;
	}
	/**
	 * @return the qty
	 */
	public long getQty() {
		return qty;
	}
	/**
	 * @param qty the qty to set
	 */
	public void setQty(long qty) {
		this.qty = qty;
	}
	/**
	 * @return the price
	 */
	public float getPrice() {
		return price;
	}
	/**
	 * @param price the price to set
	 */
	public void setPrice(float price) {
		this.price = price;
	}
	/**
	 * @return the buyerId
	 */
	public String getBuyerId() {
		return buyerId;
	}
	/**
	 * @param buyerId the buyerId to set
	 */
	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}
	/**
	 * @return the sellerId
	 */
	public String getSellerId() {
		return sellerId;
	}
	/**
	 * @param sellerId the sellerId to set
	 */
	public void setSellerId(String sellerId) {
		this.sellerId = sellerId;
	}
};

