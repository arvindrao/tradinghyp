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
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Arvind Rao
 * 
 * Process cancel order requests
 */
@SuppressWarnings("serial")
public class CancelOrderTaskProcessor extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(CancelOrderTaskProcessor.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("BEGIN");

		String symbol=req.getParameter("symbol");
		long userId=new Long(req.getParameter("userId"));
		long orderId=new Long(req.getParameter("orderId"));
		Queue mdQueue= null;

		try{
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);

			String orderData= bMgr.processCancelOrder(orderId, userId);

			//Send order update to party
			ChannelService chService=ChannelServiceFactory.getChannelService();
			chService.sendMessage(new ChannelMessage(new Long(userId).toString(),orderData));

			//Update market data
			mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
			mdQueue.add(
					withUrl("/tasks/processMarketData.do")
					.param("symbol",symbol)
					.param("reloadCache", "N")
					.method(Method.POST)
					);
		}
		catch (Exception e){
			log.error("EXCEPTION", e);
		}

		log.info("END");
	}
}
