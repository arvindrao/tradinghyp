/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import java.io.IOException;
import javax.servlet.http.*;
import com.google.appengine.api.channel.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Arvind Rao
 * 
 * Process user disconnections
 */

@SuppressWarnings("serial")
public class DisconnectionDetector extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(DisconnectionDetector.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		ChannelService chService;
		ChannelPresence chPresence;
		long userId=0;
		Queue mdQueue= null;
		boolean activeOrders=false;

		log.info("BEGIN");
		try{
			chService=ChannelServiceFactory.getChannelService();
			chPresence=chService.parsePresence(req);
			userId=new Long(chPresence.clientId());
			log.info("UserId "+userId);
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);
			activeOrders=bMgr.removeUser(userId);

		}
		catch (Exception e){
			log.error("EXCEPTION", e);
		}
		finally{
			if (activeOrders){
				//publish mkt data update
				mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
				mdQueue.add(
						withUrl("/tasks/processMarketData.do")
						.param("reloadCache", "N")
						.method(Method.POST)
						);
			}
			log.info("END");

		}

	}
}
