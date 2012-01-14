/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arvind Rao
 *
 * Initialization for a new instance
 */
public class InitServlet implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(InitServlet.class);

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("BEGIN");
		

		try{
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(arg0.getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);
			
			CacheManager mcacheMgr = CacheManager.getInstance();
			Map props =null;
			

			Cache userCache=mcacheMgr.getCache("UserCache");
			if (userCache == null){
				log.info("Creating user cache");
				props = new HashMap();
				props.put(GCacheFactory.EXPIRATION_DELTA, Constants.CACHE_EXPIRATION);
				userCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("UserCache", userCache);
			}

			Map<Long,User> users=bMgr.getActiveUsers();
			if (users!=null){
				userCache.put(Constants.UserType.TRADER, users);	
			}

			Cache mktDataCache=mcacheMgr.getCache("MarketDataCache");
			if (mktDataCache == null){
				log.info("Creating MarketDataCache");
				props = new HashMap();
				props.put(GCacheFactory.EXPIRATION_DELTA, Constants.CACHE_EXPIRATION);
				mktDataCache = mcacheMgr.getCacheFactory().createCache(props);
				mcacheMgr.registerCache("MarketDataCache", mktDataCache);
			}
			
			if (mktDataCache.get(Constants.MktDataStatistic.TRADE_STATS)==null){
				TradeStats tStats=bMgr.getTradeStats();
				if (tStats!=null){
					mktDataCache.put(Constants.MktDataStatistic.TRADE_STATS, tStats);
				}
			}

		}
		catch(Exception e){
			log.error("EXCEPTION", e);
		}

		log.info("END");
	}

}
