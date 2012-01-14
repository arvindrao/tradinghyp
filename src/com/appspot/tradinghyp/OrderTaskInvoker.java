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
 * Create an order processing task.
 */

@SuppressWarnings("serial")
public class OrderTaskInvoker extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String symbol=req.getParameter("symbol");
		String orderType=req.getParameter("orderType");
		String side=req.getParameter("side");
		String qty=req.getParameter("qty");
		String price=req.getParameter("price");
		Long userId=(Long)(req.getSession().getAttribute("userId"));

		Queue oQueue= QueueFactory.getQueue("OrderTaskQueue");
		
		if (orderType.equals("NEW")){
			oQueue.add(withUrl("/tasks/processNewOrder.do")
					.param("symbol",symbol)
					.param("side",side.equals("B")?"0":"1")
					.param("qty",qty)
					.param("price",price)
					.param("userId", new Long(userId).toString())
					.method(Method.POST));

					resp.setContentType("text/plain");
					resp.getWriter().println("Order sent! "+side+" "+qty+" shares @ "+price);
		}
		else if (orderType.equals("CXL")){
			long orderId=new Long(req.getParameter("orderId"));
			oQueue.add(withUrl("/tasks/processCancelOrder.do")
					.param("symbol",symbol)
					.param("userId", new Long(userId).toString())
					.param("orderId", ((Long)orderId).toString())
					.method(Method.POST));

					resp.setContentType("text/plain");
					resp.getWriter().println("Cancel sent for order# "+orderId);
			
		}
		else{
			resp.setContentType("text/plain");
			resp.getWriter().println("Invalid order type");
			
		}
	}

}
