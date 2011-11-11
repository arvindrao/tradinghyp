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
public class OrderTaskInvoker extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//System.out.println("OrderTaskInvoker: BEGIN");
		String symbol=req.getParameter("symbol");
		String orderType=req.getParameter("orderType");
		String side=req.getParameter("side");
		String qty=req.getParameter("qty");
		String price=req.getParameter("price");
		String traderId=(String)req.getSession().getAttribute("traderId");

		Queue oQueue= QueueFactory.getQueue("OrderTaskQueue");
		
		if (orderType.equals("NEW")){
			oQueue.add(withUrl("/tasks/processNewOrder.do")
					.param("symbol",symbol)
					.param("side",side.equals("B")?"0":"1")
					.param("qty",qty)
					.param("price",price)
					.param("traderId", traderId)
					.method(Method.POST));

					resp.setContentType("text/plain");
					resp.getWriter().println("Order sent! "+side+" "+qty+" shares @ "+price);
		}
		else if (orderType.equals("CXL")){
			long orderId=new Long(req.getParameter("orderId"));
			oQueue.add(withUrl("/tasks/processCancelOrder.do")
					.param("symbol",symbol)
					.param("traderId", traderId)
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
