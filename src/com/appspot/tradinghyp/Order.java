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
 * Represents an order entity.
 */
public class Order implements Serializable,Comparable<Order> {
	private static final long serialVersionUID = 1;

	private long orderId;
	private long userId;
	private String symbol;
	private short side;
	private long orderQty;
	private long price;
	private long execQty;
	private long tradeValue;
	private short status;
	private long orderTime;
	

	public Order() {
	}


	/**
	 * @return the orderId
	 */
	public long getOrderId() {
		return orderId;
	}


	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}


	/**
	 * @return the userId
	 */
	public long getUserId() {
		return userId;
	}


	/**
	 * @param userId the userId to set
	 */
	public void setUserId(long userId) {
		this.userId = userId;
	}


	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}


	/**
	 * @param symbol the symbol to set
	 */
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}


	/**
	 * @return the side
	 */
	public short getSide() {
		return side;
	}


	/**
	 * @param side the side to set
	 */
	public void setSide(short side) {
		this.side = side;
	}


	/**
	 * @return the orderQty
	 */
	public long getOrderQty() {
		return orderQty;
	}


	/**
	 * @param orderQty the orderQty to set
	 */
	public void setOrderQty(long orderQty) {
		this.orderQty = orderQty;
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
	 * @return the execQty
	 */
	public long getExecQty() {
		return execQty;
	}


	/**
	 * @param execQty the execQty to set
	 */
	public void setExecQty(long execQty) {
		this.execQty = execQty;
	}


	/**
	 * @return the tradeValue
	 */
	public long getTradeValue() {
		return tradeValue;
	}


	/**
	 * @param tradeValue the tradeValue to set
	 */
	public void setTradeValue(long tradeValue) {
		this.tradeValue = tradeValue;
	}


	/**
	 * @return the status
	 */
	public short getStatus() {
		return status;
	}


	/**
	 * @param status the status to set
	 */
	public void setStatus(short status) {
		this.status = status;
	}


	/**
	 * @return the orderTime
	 */
	public long getOrderTime() {
		return orderTime;
	}


	/**
	 * @param orderTime the orderTime to set
	 */
	public void setOrderTime(long orderTime) {
		this.orderTime = orderTime;
	}


	@Override
	public int compareTo(Order o) {
		//use order size for comparizon
		long otherQty=o.getOrderQty();
		if (orderQty==otherQty){
			return 0;
		}
		else if (orderQty>otherQty){
			return 1;
		}
		else{
			return -1;
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + (int) (orderId ^ (orderId >>> 32));
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
		if (!(obj instanceof Order)){
			return false;
		}
		Order other = (Order) obj;
		if (orderId != other.orderId){
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
		return "Order [orderId=" + orderId + ", userId=" + userId + ", symbol="
				+ symbol + ", side=" + side + ", orderQty=" + orderQty
				+ ", price=" + price + ", execQty=" + execQty + ", tradeValue="
				+ tradeValue + ", status=" + status + ", orderTime="
				+ orderTime + "]";
	}

	

}
