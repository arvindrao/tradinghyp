/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.*;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Arvind Rao
 * 
 * Initialize bots as users of this application  
 */
@SuppressWarnings("serial")
public class BotInitializer extends HttpServlet {
	private static final Logger log = Logger.getLogger(BotInitializer.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("BEGIN");
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery obPQ=null;
		Query obQ=null;
		Entity ob=null;
		Transaction txn=null;
		Map props=null;
		HashMap<String,User> users=null;


		try {
			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			ob=obPQ.asIterator().next();

			CacheManager mcacheMgr = CacheManager.getInstance();
			Cache userCache=mcacheMgr.getCache("UserCache");
			if (userCache == null){
				log.info("Creating user cache");
				props = new HashMap();
				props.put(GCacheFactory.EXPIRATION_DELTA, 36000);
				userCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("UserCache", userCache);
			}
			users=(HashMap<String,User>) userCache.get(Constants.UserType.TRADER);
			if (users==null){
				users=Util.fetchUsers();
			}
			
			Entity u=new Entity("User",ob.getKey());
			User aUser=new User(Constants.BOT1_TRADER_ID,Constants.BOT1_TRADER_ID);
			users.put(Constants.BOT1_TRADER_ID, aUser);
			u.setProperty("userId",aUser.getUserId());
			u.setProperty("traderId",aUser.getTraderId());
			u.setProperty("loginTime",aUser.getLoginTime());
			u.setProperty("logoutTime",aUser.getLogoutTime());
			u.setProperty("longAvgPrice",aUser.getLongAvgPrice());
			u.setProperty("longCash",aUser.getLongCash());
			u.setProperty("longPosition",aUser.getLongPosition());
			u.setProperty("shortAvgPrice",aUser.getShortAvgPrice());
			u.setProperty("shortCash",aUser.getShortCash());
			u.setProperty("shortPosition",aUser.getShortPosition());
			u.setProperty("origRealizedGain",aUser.getOrigRealizedGain());
			u.setProperty("realizedGain",aUser.getRealizedGain());

			Entity u2=new Entity("User",ob.getKey());
			aUser=new User(Constants.BOT2_TRADER_ID,Constants.BOT2_TRADER_ID);
			users.put(Constants.BOT2_TRADER_ID, aUser);
			u2.setProperty("userId",aUser.getUserId());
			u2.setProperty("traderId",aUser.getTraderId());
			u2.setProperty("loginTime",aUser.getLoginTime());
			u2.setProperty("logoutTime",aUser.getLogoutTime());
			u2.setProperty("longAvgPrice",aUser.getLongAvgPrice());
			u2.setProperty("longCash",aUser.getLongCash());
			u2.setProperty("longPosition",aUser.getLongPosition());
			u2.setProperty("shortAvgPrice",aUser.getShortAvgPrice());
			u2.setProperty("shortCash",aUser.getShortCash());
			u2.setProperty("shortPosition",aUser.getShortPosition());
			u2.setProperty("origRealizedGain",aUser.getOrigRealizedGain());
			u2.setProperty("realizedGain",aUser.getRealizedGain());

			txn=ds.beginTransaction();
			ds.put(u);
			ds.put(u2);

			userCache.put(Constants.UserType.TRADER, users);

			txn.commit();
			resp.setContentType("text/plain");
			resp.getWriter().println("Initialized bots");

		} catch (Exception e) {
			log.log(Level.SEVERE, "EXCEPTION", e);
			resp.setContentType("text/plain");
			resp.getWriter().println("Error, could not initialize bots");
			resp.getWriter().println(e.toString());
		}
		finally{
			log.info("END");

		}
	}
	public void doGet (HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
			doPost(req,resp);
	}
}
