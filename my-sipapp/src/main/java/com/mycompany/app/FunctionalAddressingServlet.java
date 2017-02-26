/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.mycompany.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class FunctionalAddressingServlet extends SipServlet {
	private static final long serialVersionUID = 1L;
	
	
	private static Log logger = LogFactory.getLog(FunctionalAddressingServlet.class);
	HashMap<SipSession, SipSession> sessions = new HashMap<SipSession, SipSession>();
	Map<String, String[]> forwardingUris = null;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		logger.info("the FunctionalAddressingServlet servlet has been started");
		super.init(servletConfig);
		forwardingUris = new ConcurrentHashMap<String, String[]>();
		forwardingUris.put("sip:alice@kam.ims", new String[]{"sip:bob@kam.ims", "sip:bob@kam.ims"});
	}

	/**
	 * @Override protected void doInvite(SipServletRequest request) throws
	 *           ServletException, IOException {
	 * 
	 *           logger.info("Got request:${symbol_escape}n" +
	 *           request.toString()); String fromUri =
	 *           request.getFrom().getURI().toString(); logger.info(fromUri);
	 * 
	 *           SipServletResponse sipServletResponse =
	 *           request.createResponse(SipServletResponse.SC_OK);
	 *           sipServletResponse.send(); }
	 */

	@Override
	protected void doInvite(SipServletRequest request) throws ServletException, IOException {

		//Utils util = new Utils();
		/**
		 * Test code for the DB query--------------------------------------/
		 * try
		 * { ResultSet rs = client.execQuery("select * from public.\"CELL\"");
		 * while (rs.next()){ System.out.println(rs.getString(1) + "\t\t");
		 * System.out.println(rs.getString(2)); } } catch (SQLException ex) {
		 * System.err.println(ex); }
		 * -------------------------------------------------------------------
		 */
		
		}
	
		

/**		SipServletRequest outRequest = sipFactory.createRequest(request.getApplicationSession(), "INVITE",
				request.getFrom().getURI(), request.getTo().getURI());

		
		outRequest.setRequestURI(request.getTo().getURI());
		if (request.getContent() != null) {
			outRequest.setContent(request.getContent(), request.getContentType());
		}
		
		logger.info("------------------------------------ Forwarding request:\n");
		outRequest.send();
		sessions.put(request.getSession(), outRequest.getSession());
		sessions.put(outRequest.getSession(), request.getSession());
	}
	
*/
	protected void doResponse(SipServletResponse response) throws ServletException, IOException {
		if (logger.isInfoEnabled()) {
			logger.info("FunctionalAddressingServlet: Got response:\n" + response);
		}
		response.getSession().setAttribute("lastResponse", response);
		SipServletRequest request = (SipServletRequest) sessions.get(response.getSession()).getAttribute("lastRequest");
		SipServletResponse resp = request.createResponse(response.getStatus());
		if (response.getContent() != null) {
			resp.setContent(response.getContent(), response.getContentType());
		}
		resp.send();
	}
	
/**	 public static void sendSIPMessage(String toAddressString, SipServletRequest request)
	  {
	    try
	    {
	      logger.info( "Sending SIP message to " + request.getTo().getURI().toString() );

	      SipApplicationSession appSession = sipFactory.createApplicationSession();
	      Address from = sipFactory.createAddress("Missed Calls <sip:missed-calls@mss.mobicents.org>");
	      Address to = sipFactory.createAddress(toAddressString);
	      SipServletRequest forwardedrequest = sipFactory.createRequest(appSession, "INVITE", from, to);
	      forwardedrequest.setContent(request.getContent(), request.getContentType());

	      forwardedrequest.send(); 
	    }
	    catch (Exception e) {
	      logger.error( "Failure creating/sending SIP request", e );
	    }
	  }
*/
	@Override
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
		logger.info("the FunctionalAddressingServlet has received a BYE.....");
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
	}
}
