/**
 * 
 */
package com.appspot.tradinghyp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.*;
import com.google.appengine.api.channel.*;
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
 * 
 * @author Arvind Rao
 *
 */

@SuppressWarnings("serial")
public class ConnectionInitializer extends HttpServlet {
	private static final Logger log = Logger.getLogger(ConnectionInitializer.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("BEGIN");
		
		String userId=req.getParameter("userId")!=null?(String)req.getParameter("userId"):"Anonymous";
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery obPQ=null,pq2=null;
		Query obQ=null,q=null;
		Entity ob=null,u=null,ustats=null;
		Transaction txn=null;
		Long traderId=1L;
		Long activeTraderCount=0L;
		HttpSession s=null;
		Map props=null;
		HashMap<String,User> users=null;
		StringBuffer connResponse=new StringBuffer("{\"connected\":");


		try {
			obQ=new Query("OrderBook");
			obQ.addFilter("active",Query.FilterOperator.EQUAL,true);
			obPQ = ds.prepare(obQ);
			ob=obPQ.asIterator().next();

			q=new Query("UserStats");
			q.setAncestor(ob.getKey());
			q.addFilter("active",Query.FilterOperator.EQUAL,true);
			pq2=ds.prepare(q);
			ustats=pq2.asIterator().next();
			traderId=(Long)ustats.getProperty("lastGuestTraderId")+1;
			activeTraderCount=(Long)ustats.getProperty("activeTraderCount")+1;
			ustats.setProperty("lastGuestTraderId", traderId);
			ustats.setProperty("activeTraderCount", activeTraderCount);

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
			
			User aUser=new User(userId,traderId.toString());
			users.put(traderId.toString(), aUser);

			u=new Entity("User",ob.getKey());
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

			txn=ds.beginTransaction();
			ds.put(ustats);
			ds.put(u);

			ChannelService chService;
			String chToken;
			chService=ChannelServiceFactory.getChannelService();
			chToken=chService.createChannel(traderId.toString());
			s=req.getSession();
			s.setAttribute("traderId", traderId.toString());
			s.setAttribute("userId", userId);
			connResponse.append("\"Y\",\"token\":\""+chToken+"\","
					+"\"traderId\":"+traderId.toString()+","
					+"\"activeTraderCount\":"+activeTraderCount+"}");

			userCache.put(Constants.UserType.TRADER, users);

			txn.commit();
			log.info("New traderId "+traderId.toString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			connResponse.append("\"N\"}");
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
		finally{
			resp.setContentType("text/plain");
			resp.getWriter().println(connResponse.toString());
			log.info("END");

		}
	}
}
