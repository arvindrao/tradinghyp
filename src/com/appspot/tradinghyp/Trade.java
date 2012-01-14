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
	
	public Trade(long tradeTime, long qty, long price,
			long buyerId, long sellerId) {
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
	private long price;
	private long buyerId;
	private long sellerId;

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
	public long getPrice() {
		return price;
	}
	/**
	 * @param price the price to set
	 */
	public void setPrice(long price) {
		this.price = price;
	}
	/**
	 * @return the buyerId
	 */
	public long getBuyerId() {
		return buyerId;
	}
	/**
	 * @param buyerId the buyerId to set
	 */
	public void setBuyerId(long buyerId) {
		this.buyerId = buyerId;
	}
	/**
	 * @return the sellerId
	 */
	public long getSellerId() {
		return sellerId;
	}
	/**
	 * @param sellerId the sellerId to set
	 */
	public void setSellerId(long sellerId) {
		this.sellerId = sellerId;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override

	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + (int) (tradeId ^ (tradeId >>> 32));
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (!(obj instanceof Trade)){
			return false;
		}
		Trade other = (Trade) obj;
		if (tradeId != other.tradeId){
			return false;
		}
		else{
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Trade [tradeId=" + tradeId + ", tradeTime=" + tradeTime
				+ ", qty=" + qty + ", price=" + price + ", buyerId=" + buyerId
				+ ", sellerId=" + sellerId + "]";
	}
	
};
