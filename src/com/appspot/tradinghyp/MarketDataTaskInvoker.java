/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;
import java.io.IOException;
import javax.servlet.http.*;

/**
 * @author Arvind Rao
 *
 * Create a task to update market data for one user  
 */

@SuppressWarnings("serial")
public class MarketDataTaskInvoker extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		String traderId=(String)req.getSession().getAttribute("traderId");
		
		Queue mdQueue= QueueFactory.getQueue("MarketDataTaskQueue");
		
		mdQueue.add(
				withUrl("/tasks/processMarketData.do")
				.param("symbol",symbol)
				.param("traderId", traderId)
				.method(Method.POST)
		);
		
	}
}
