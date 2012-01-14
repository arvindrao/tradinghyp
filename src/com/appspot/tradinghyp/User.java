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
 * Represents a user entity.
 */
public class User implements Serializable,Comparable<User> {
	private static final long serialVersionUID = 1L;

	private long userId;
	private String userName;
	private long longPosition;
	private long shortPosition;
	private long shortCash;
	private long longCash;
	private long realizedGain;
	private long origRealizedGain;
	private long shortAvgPrice;
	private long longAvgPrice;
	private long loginTime;
	private long logoutTime;
	

	public User() {
	}

	public User(String userName) {
		this.userName=userName;
	}

	public User(long userId,String userName) {
		this.userId=userId;
		this.userName=userName;
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
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the longPosition
	 */
	public long getLongPosition() {
		return longPosition;
	}

	/**
	 * @param longPosition the longPosition to set
	 */
	public void setLongPosition(long longPosition) {
		this.longPosition = longPosition;
	}

	/**
	 * @return the shortPosition
	 */
	public long getShortPosition() {
		return shortPosition;
	}

	/**
	 * @param shortPosition the shortPosition to set
	 */
	public void setShortPosition(long shortPosition) {
		this.shortPosition = shortPosition;
	}

	/**
	 * @return the shortCash
	 */
	public long getShortCash() {
		return shortCash;
	}

	/**
	 * @param shortCash the shortCash to set
	 */
	public void setShortCash(long shortCash) {
		this.shortCash = shortCash;
	}

	/**
	 * @return the longCash
	 */
	public long getLongCash() {
		return longCash;
	}

	/**
	 * @param longCash the longCash to set
	 */
	public void setLongCash(long longCash) {
		this.longCash = longCash;
	}

	/**
	 * @return the realizedGain
	 */
	public long getRealizedGain() {
		return realizedGain;
	}

	/**
	 * @param realizedGain the realizedGain to set
	 */
	public void setRealizedGain(long realizedGain) {
		this.realizedGain = realizedGain;
	}

	/**
	 * @return the origRealizedGain
	 */
	public long getOrigRealizedGain() {
		return origRealizedGain;
	}

	/**
	 * @param origRealizedGain the origRealizedGain to set
	 */
	public void setOrigRealizedGain(long origRealizedGain) {
		this.origRealizedGain = origRealizedGain;
	}

	/**
	 * @return the shortAvgPrice
	 */
	public long getShortAvgPrice() {
		return shortAvgPrice;
	}

	/**
	 * @param shortAvgPrice the shortAvgPrice to set
	 */
	public void setShortAvgPrice(long shortAvgPrice) {
		this.shortAvgPrice = shortAvgPrice;
	}

	/**
	 * @return the longAvgPrice
	 */
	public long getLongAvgPrice() {
		return longAvgPrice;
	}

	/**
	 * @param longAvgPrice the longAvgPrice to set
	 */
	public void setLongAvgPrice(long longAvgPrice) {
		this.longAvgPrice = longAvgPrice;
	}

	/**
	 * @return the loginTime
	 */
	public long getLoginTime() {
		return loginTime;
	}

	/**
	 * @param loginTime the loginTime to set
	 */
	public void setLoginTime(long loginTime) {
		this.loginTime = loginTime;
	}

	/**
	 * @return the logoutTime
	 */
	public long getLogoutTime() {
		return logoutTime;
	}

	/**
	 * @param logoutTime the logoutTime to set
	 */
	public void setLogoutTime(long logoutTime) {
		this.logoutTime = logoutTime;
	}

	@Override
	public int compareTo(User u) {
		//use score for comparison
		long otherRealizedGain=u.getRealizedGain();
		if (realizedGain==otherRealizedGain){
			return 0;
		}
		else if (realizedGain>otherRealizedGain){
			return 1;
		}
		else{
			return -1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (!(obj instanceof User)){
			return false;
		}
		User other = (User) obj;
		if (userId != other.userId){
			return false;
		}
		else{
			return true;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + (int) (userId ^ (userId >>> 32));
		return result;
	}
	
	public void updateScore(short lastExecSide, long lastExecVol, long lastExecPrice){
		if (lastExecSide==Constants.BUY){
			if (shortPosition>0){
				//player is short, try to close out as much of this position as possible
				if (shortPosition>lastExecVol){
					realizedGain=realizedGain+(shortAvgPrice*lastExecVol - lastExecPrice*lastExecVol);
					shortPosition=shortPosition-lastExecVol;
					shortCash=shortCash-(lastExecVol*lastExecPrice);
				}
				else{
					shortCash=shortCash-(shortPosition*lastExecPrice);
					longPosition=lastExecVol-shortPosition;
					shortAvgPrice=0;
					longAvgPrice=lastExecPrice;
					shortPosition=0;
					longCash=longCash-(longPosition*lastExecPrice);
					//calculate gain as position closed out
					realizedGain=origRealizedGain+shortCash;
					origRealizedGain=realizedGain;
					shortCash=0;
				}
		
			}
			else{
				//player is flat or long
				shortAvgPrice=0;
				longAvgPrice=((longAvgPrice*longPosition)+(lastExecVol*lastExecPrice))/(longPosition+lastExecVol);
				longPosition=longPosition+lastExecVol;
				longCash=longCash-(lastExecVol*lastExecPrice);
				origRealizedGain=realizedGain;
			}
		}
		else{
			//side=sell
			if (longPosition>0){
				//player is long, try to close out as much of this position as possible
				if (longPosition>lastExecVol){
					realizedGain=realizedGain+(lastExecPrice*lastExecVol - longAvgPrice*lastExecVol);
					longPosition=longPosition-lastExecVol;
					longCash=longCash+(lastExecVol*lastExecPrice);
				}
				else{
					longCash=longCash+(longPosition*lastExecPrice);
					shortPosition=lastExecVol-longPosition;
					longAvgPrice=0;
					shortAvgPrice=lastExecPrice;
					longPosition=0;
					shortCash=shortCash+(shortPosition*lastExecPrice);
					//calculate gain as position closed out
					realizedGain=origRealizedGain+longCash;
					origRealizedGain=realizedGain;
					longCash=0;
				}
			}
			else{
				//player is flat or short
				longAvgPrice=0;
				shortAvgPrice=((shortAvgPrice*shortPosition)+(lastExecVol*lastExecPrice))/(shortPosition+lastExecVol);
				shortPosition=shortPosition+lastExecVol;
				shortCash=shortCash+(lastExecVol*lastExecPrice);
				origRealizedGain=realizedGain;
			}
		}

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User [userId=" + userId + ", userName=" + userName
				+ ", loginTime=" + loginTime + ", logoutTime=" + logoutTime
				+ "]";
	}
}
