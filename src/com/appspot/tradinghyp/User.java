/**
 * 
 */
package com.appspot.tradinghyp;

import java.io.Serializable;

/**
 * @author Arvind Rao
 *
 */
public class User implements Serializable,Comparable<User> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1;

	public User() {
		longPosition=0;
		shortPosition=0;
		shortCash=0;
		longCash=0;
		realizedGain=0;
		origRealizedGain=0;
		shortAvgPrice=0;
		longAvgPrice=0;
		loginTime=System.currentTimeMillis();
		logoutTime=0;
		userId=null;
		traderId=null;
	}

	public User(String userId,String traderId) {
		longPosition=0;
		shortPosition=0;
		shortCash=0;
		longCash=0;
		realizedGain=0;
		origRealizedGain=0;
		shortAvgPrice=0;
		longAvgPrice=0;
		loginTime=System.currentTimeMillis();
		logoutTime=0;
		this.userId=userId;
		this.traderId=traderId;
	}
	
	private String userId;
	private String traderId;
	private long longPosition;
	private long shortPosition;
	private double shortCash;
	private double longCash;
	private double realizedGain;
	private double origRealizedGain;
	private float shortAvgPrice;
	private float longAvgPrice;
	private long loginTime;
	private long logoutTime;
	

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
	public double getShortCash() {
		return shortCash;
	}
	/**
	 * @param shortCash the shortCash to set
	 */
	public void setShortCash(double shortCash) {
		this.shortCash = shortCash;
	}
	/**
	 * @return the longCash
	 */
	public double getLongCash() {
		return longCash;
	}
	/**
	 * @param longCash the longCash to set
	 */
	public void setLongCash(double longCash) {
		this.longCash = longCash;
	}
	/**
	 * @return the realizedGain
	 */
	public double getRealizedGain() {
		return realizedGain;
	}
	/**
	 * @param realizedGain the realizedGain to set
	 */
	public void setRealizedGain(double realizedGain) {
		this.realizedGain = realizedGain;
	}
	/**
	 * @return the origRealizedGain
	 */
	public double getOrigRealizedGain() {
		return origRealizedGain;
	}
	/**
	 * @param origRealizedGain the origRealizedGain to set
	 */
	public void setOrigRealizedGain(double origRealizedGain) {
		this.origRealizedGain = origRealizedGain;
	}
	/**
	 * @return the shortAvgPrice
	 */
	public float getShortAvgPrice() {
		return shortAvgPrice;
	}
	/**
	 * @param shortAvgPrice the shortAvgPrice to set
	 */
	public void setShortAvgPrice(float shortAvgPrice) {
		this.shortAvgPrice = shortAvgPrice;
	}
	/**
	 * @return the longAvgPrice
	 */
	public float getLongAvgPrice() {
		return longAvgPrice;
	}
	/**
	 * @param longAvgPrice the longAvgPrice to set
	 */
	public void setLongAvgPrice(float longAvgPrice) {
		this.longAvgPrice = longAvgPrice;
	}
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return the traderId
	 */
	public String getTraderId() {
		return traderId;
	}
	/**
	 * @param traderId the traderId to set
	 */
	public void setTraderId(String traderId) {
		this.traderId = traderId;
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
	public int compareTo(User o) {
		//use score for comparison
		if (this.realizedGain==o.getRealizedGain()){
			return 0;
		}
		else if (this.realizedGain>o.getRealizedGain()){
			return 1;
		}
		else{
			return -1;
		}
	}

	public void updateScore(Short lastExecSide, long lastExecVol, float lastExecPrice){
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
	
}
