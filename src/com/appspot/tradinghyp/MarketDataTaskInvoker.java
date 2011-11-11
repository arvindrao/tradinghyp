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


/**
 * 
 * @author Arvind Rao
 *
 */

@SuppressWarnings("serial")
public class MarketDataTaskInvoker extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		String traderId=(String)req.getSession().getAttribute("traderId");
		//System.out.println("Market Data Task Invoker:"+symbol+";"+traderId);
		
		Queue mdQueue= QueueFactory.getQueue("MarketDataTaskQueue");
		//TaskOptions tOptions = TaskOptions.Builder.withUrl("/orderTask.do").param("side",side).param("qty",qty).method(Method.POST);
		
		mdQueue.add(
				withUrl("/tasks/processMarketData.do")
				.param("symbol",symbol)
				.param("traderId", traderId)
				.method(Method.POST)
		);
		
	}
}
