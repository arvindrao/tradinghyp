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
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Arvind Rao
 * 
 * Refresh connection on channel socket timeout
 * TODO: invoke this in client javascript
 */

@SuppressWarnings("serial")
public class ConnectionRefresher extends HttpServlet {
	private static final Logger log = Logger.getLogger(ConnectionRefresher.class.getName());
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("BEGIN");
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
			connResponse.append("\"N\"}");
			log.log(Level.SEVERE, "EXCEPTION", e);
		}
		finally{
			resp.setContentType("text/plain");
			resp.getWriter().println(connResponse.toString());
			//log.info(connResponse.toString());
			log.info("END");

		}
	}
}
