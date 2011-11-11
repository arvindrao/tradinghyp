/**
 * 
 */
package com.appspot.tradinghyp;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;
import java.io.IOException;
import javax.servlet.http.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Arvind Rao
 *
 */

@SuppressWarnings("serial")
public class MarketTakerBot extends HttpServlet {
	private static final Logger log = Logger.getLogger(MarketTakerBot.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		try{
			
			Queue oQueue= QueueFactory.getQueue("OrderTaskQueue");

			//decide whether to buy or sell
			Short side=(short) Math.round(Math.random());
			Long ordSize=(Constants.MIN_MT_BOT_QTY+Math.round(Math.random()*(Constants.MAX_MT_BOT_QTY-Constants.MIN_MT_BOT_QTY)))*Constants.LOT_SIZE;
				log.info(Constants.BOT2_TRADER_ID+(side==Constants.BUY?" BUYS ":" SELLS ")+ordSize+"@0");
					oQueue.add(withUrl("/tasks/processNewOrder.do")
							.param("symbol","HYP")
							.param("side",side.toString())
							.param("qty",ordSize.toString())
							.param("price","0")
							.param("traderId", Constants.BOT2_TRADER_ID)
							.countdownMillis(1000)
							.method(Method.POST));
		}
		catch (Exception e){
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
			doPost(req,resp);
	}

}
