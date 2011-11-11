/**
 * 
 */
package com.appspot.tradinghyp;

import java.io.IOException;

import javax.servlet.http.*;
import com.google.appengine.api.channel.*;


/**
 * 
 * @author Arvind Rao
 *
 */

@SuppressWarnings("serial")
public class ConnectionRefresher extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		System.out.println("ConnectionRefresher: BEGIN");
		HttpSession s;
		String traderId=null;
		
		StringBuffer connResponse=new StringBuffer("{\"connected\":");

		try{
			s=req.getSession();
			traderId=(String) s.getAttribute("traderId");
			
			ChannelService chService;
			String chToken;
			chService=ChannelServiceFactory.getChannelService();
			chToken=chService.createChannel(traderId.toString());

			connResponse.append("\"Y\",\"token\":\""+chToken+"\"}");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			connResponse.append("\"N\"}");
			e.printStackTrace();
		}
		finally{
			resp.setContentType("text/plain");
			resp.getWriter().println(connResponse.toString());
			System.out.println("ConnectionRefresher: response="+connResponse.toString());
			System.out.println("ConnectionRefresher: END");

		}
	}
}
