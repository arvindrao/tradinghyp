/**
 * TRADING HYP - the online day trading simulator
 * Written in 2011 by Arvind Rao arvindrao.dev@gmail.com
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. 
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. 
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.appspot.tradinghyp;

import java.io.IOException;
import javax.servlet.http.*;
import com.google.appengine.api.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Arvind Rao
 * 
 * Initalize new user connections
 */

@SuppressWarnings("serial")
public class ConnectionInitializer extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(ConnectionInitializer.class);
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("BEGIN");
		
		String userName=req.getParameter("userName")!=null?(String)req.getParameter("userName"):"Anonymous";
		HttpSession s=null;
		StringBuffer connResponse=new StringBuffer("{\"connected\":");
		String chToken;
		long userId;

		try {
			ApplicationContext ctx=WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			BusinessManager bMgr=(BusinessManager) ctx.getBean(Constants.ROOT_BEAN);
			userId=bMgr.addNewUser(userName);
			s=req.getSession();
			s.setAttribute("userId", userId);
			s.setAttribute("userName", userName);
			ChannelService chService;
			chService=ChannelServiceFactory.getChannelService();
			chToken=chService.createChannel(new Long(userId).toString());
			connResponse.append("\"Y\",\"token\":\""+chToken+"\","
					+"\"userId\":"+userId+"}");

			log.info("New userId "+userId);

		} catch (Exception e) {
			connResponse.append("\"N\"}");
			log.error("EXCEPTION", e);
		}
		finally{
			resp.setContentType("text/plain");
			resp.getWriter().println(connResponse.toString());
			log.info("END");

		}
	}
}
