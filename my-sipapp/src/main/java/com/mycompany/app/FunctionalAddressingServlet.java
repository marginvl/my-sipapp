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
import java.rmi.ConnectException;
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
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class FunctionalAddressingServlet extends SipServlet {
	private static final long serialVersionUID = 3978425801979081269L;
	
	
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
		
		B2buaHelper b2buaHelper = request.getB2buaHelper();
		SipSession session = request.getSession();
		Map<String, List<String>> headerMap = new ConcurrentHashMap<String, List<String>>();
		SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		SipURI destination = (SipURI) sipFactory.createURI("sip:bob@kam.ims");

		String contactUri =  request.getHeader("Contact");
		
		logger.info("Functional addressing servlet - INVITE received\n");
		logger.info("Functional addressing servlet - To header:    " + destination.toString() + "\n");
		logger.info("Functional addressing servlet - Contact header:    " + contactUri.toString() + "\n");
				
		SipServletRequest forkedRequest = b2buaHelper.createRequest(request, true, headerMap);
		logger.info("Functional addressing servlet - B2BUAhelper request created    \n");
		
		forkedRequest.setRequestURI(destination);
		logger.info("Functional addressing servlet - B2BUAhelper destination set:    "+destination.toString()+"\n");
/*		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		forkedRequest.getSession().setAttribute("originalRequest", request);
		logger.info("Functional addressing servlet - some attribute set   \n");
		
		try {
			logger.info("Functional addressing servlet - trying to send the request    \n");
		forkedRequest.send();
		} catch (ConnectException e) {
			e.printStackTrace();
		}
		
//        if (request.isInitial()) {
//            Proxy proxy = request.getProxy();
//            proxy.setRecordRoute(true);
//            proxy.setSupervised(true);
//            proxy.proxyTo(request.getRequestURI());
//        }
				
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
	@Override
	protected void doSuccessResponse(SipServletResponse sipServletResponse)
			throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
			logger.info("Got : " + sipServletResponse.toString());
		}		
		if(sipServletResponse.getMethod().indexOf("BYE") != -1) {
			SipSession sipSession = sipServletResponse.getSession(false);
			if(sipSession != null && sipSession.isValid()) {
				sipSession.invalidate();
			}
			SipApplicationSession sipApplicationSession = sipServletResponse.getApplicationSession(false);
			if(sipApplicationSession != null && sipApplicationSession.isValid()) {
				sipApplicationSession.invalidate();
			}	
			return;
		} 
		
		if(sipServletResponse.getMethod().indexOf("INVITE") != -1) {
			//	if this is a response to an INVITE we ack it and forward the OK 
			SipServletRequest ackRequest = sipServletResponse.createAck();
			if(logger.isInfoEnabled()) {
				logger.info("Sending " +  ackRequest);
			}
			ackRequest.send();
			//create and sends OK for the first call leg							
			SipServletRequest originalRequest = (SipServletRequest) sipServletResponse.getSession().getAttribute("originalRequest");
			SipServletResponse responseToOriginalRequest = originalRequest.createResponse(sipServletResponse.getStatus());
			if(logger.isInfoEnabled()) {
				logger.info("Sending OK on 1st call leg" +  responseToOriginalRequest);
			}
			responseToOriginalRequest.setContentLength(sipServletResponse.getContentLength());
			if(sipServletResponse.getContent() != null && sipServletResponse.getContentType() != null)
				responseToOriginalRequest.setContent(sipServletResponse.getContent(), sipServletResponse.getContentType());
			responseToOriginalRequest.send();
		}		
		if(sipServletResponse.getMethod().indexOf("UPDATE") != -1) {
			B2buaHelper helper = sipServletResponse.getRequest().getB2buaHelper();
			SipServletRequest orgReq = helper.getLinkedSipServletRequest(sipServletResponse.getRequest());
			SipServletResponse res2 = orgReq.createResponse(sipServletResponse.getStatus());
			res2.send();
		}	
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
