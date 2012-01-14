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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.channel.*;

/**
 * @author Arvind Rao
 *
 * Process new order requests and match with opposite side if possible
 */

@SuppressWarnings("serial")
public class NewOrderTaskProcessor extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(NewOrderTaskProcessor.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		short side=new Short(req.getParameter("side"));
		long partyOrderQty=new Long(req.getParameter("qty"));
		long partyOrderPrice=new Long(req.getParameter("price"));
		long userId=new Long(req.getParameter("userId"));
		Queue mdQueue= null;

		try{
			//Process this order
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);
			Map<Long,StringBuffer> orderUpdate=bMgr.processNewOrder(symbol, side, partyOrderQty, partyOrderPrice, userId);

			//Send order update to party and counterparties
			ChannelService chService=ChannelServiceFactory.getChannelService();
			for (Map.Entry<Long,StringBuffer> entry : orderUpdate.entrySet()){
				//don't send update to bots, identified by userId<=100
				if (entry.getKey()>100){
					chService.sendMessage(new ChannelMessage(new Long(entry.getKey()).toString(),entry.getValue().toString()));
				}
			}
			
			//Create task to update market data
			mdQueue=QueueFactory.getQueue("MarketDataTaskQueue");
			mdQueue.add(
					withUrl("/tasks/processMarketData.do")
					.param("symbol",symbol)
					.param("reloadCache","N")
					.param("scores", "Y")
					.method(Method.POST)
					);


		}
		catch (Exception e){
			log.error("EXCEPTION", e);
		}
	}
}
