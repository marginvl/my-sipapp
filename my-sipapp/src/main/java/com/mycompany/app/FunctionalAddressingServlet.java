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
import java.sql.ResultSet;
import java.sql.SQLException;
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


	@Override
	protected void doInvite(SipServletRequest request) throws ServletException, IOException {
		
		B2buaHelper b2buaHelper = request.getB2buaHelper();
		SipSession session = request.getSession();
		SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		Utils util = new Utils();
		logger.info("Functional addressing servlet - INVITE received\n");
		logger.info("Functional addressing servlet - Request-URI:    " + request.getRequestURI().toString() + "\n");
		String cellID = request.getHeader("P-Access-Network-Info");
		logger.info("Functional addressing servlet - CELL ID:    " + cellID + "\n");
		String destination = util.getDestination(cellID);
		logger.info("Functional addressing servlet - destination:    " + destination + "\n");
		SipURI destinationURI = (SipURI) sipFactory.createURI(destination);
		//String cellID = request.getRequestURI().toString().substring(5, 10);	
	
		if (request.isInitial()) {
			Map<String, List<String>> headerMap = new ConcurrentHashMap<String, List<String>>();
			SipServletRequest forwardedRequest = b2buaHelper.createRequest(request, true, headerMap);
			forwardedRequest.setRequestURI(destinationURI);
			logger.info("Functional addressing servlet - B2BUAhelper destination set:    " + destinationURI.toString() + "\n");
			forwardedRequest.getSession().setAttribute("originalRequest", request);
			try {
				logger.info("Functional addressing servlet - trying to send the request    \n");
				forwardedRequest.send();
			} catch (ConnectException e) {
				e.printStackTrace();
			}
		} else {
			Map<String, List<String>> headerMap = new ConcurrentHashMap<String, List<String>>();
			SipSession initialSession = request.getSession();
			SipSession otherSession = b2buaHelper.getLinkedSession(initialSession);
			SipServletRequest reInvite = b2buaHelper.createRequest(otherSession, request, headerMap);
			reInvite.send();
		}
		
	}	
	
	protected void doCancel(SipServletRequest request)
            throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
		logger.info("Functional addressing servlet - CANCEL request received");
		}
		SipSession session = request.getSession();	
		B2buaHelper helper = request.getB2buaHelper();
		SipSession linkedSession = helper.getLinkedSession(session);
		SipServletRequest originalRequest = (SipServletRequest)linkedSession.getAttribute("originalRequest");
		SipServletRequest  cancelRequest = helper.getLinkedSipServletRequest(originalRequest).createCancel();
		if(logger.isInfoEnabled()) {
			logger.info("Functional addressing servlet - CANCEL request sent " + cancelRequest + "\n" );
		}
		cancelRequest.send();
    }

	
	@Override
	protected void doSuccessResponse(SipServletResponse sipServletResponse)
			throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
			logger.info("Functional addressing servlet -Got : " + sipServletResponse.toString());
		}	
		
		if(sipServletResponse.getMethod().indexOf("INVITE") != -1) {
			//	if it is the response to an INVITE, ack and forward the OK
			SipServletRequest ackRequest = sipServletResponse.createAck();
			if(logger.isInfoEnabled()) {
				logger.info("Functional addressing servlet -Sending " +  ackRequest);
			}
			ackRequest.send();
			
			//create and sends OK for the first call leg							
			SipServletRequest originalRequest = (SipServletRequest) sipServletResponse.getSession().getAttribute("originalRequest");
			logger.info("Original request URI " +  originalRequest.getRequestURI().toString() + "\n");
			SipServletResponse responseToOriginalRequest = originalRequest.createResponse(sipServletResponse.getStatus());
			if(logger.isInfoEnabled()) {
				logger.info("Functional addressing servlet -Sending OK on 1st call leg" +  responseToOriginalRequest);
			}
			responseToOriginalRequest.setContentLength(sipServletResponse.getContentLength());
			if(sipServletResponse.getContent() != null && sipServletResponse.getContentType() != null)
				responseToOriginalRequest.setContent(sipServletResponse.getContent(), sipServletResponse.getContentType());
			responseToOriginalRequest.send();
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
	}
	


	protected void doErrorResponse(SipServletResponse sipServletResponse)
			throws ServletException, IOException {
		if(logger.isInfoEnabled()) {
			logger.info("Functional addressing servlet -Got : " + sipServletResponse.getStatus() + " "
				+ sipServletResponse.getReasonPhrase());
		}
		// don't forward the timeout nor the Request Terminated due to CANCEL
		if(sipServletResponse.getStatus() != 408 && sipServletResponse.getStatus() != 487) {
			//create and send the error response for the first call leg
			SipServletRequest originalRequest = (SipServletRequest) sipServletResponse.getSession().getAttribute("originalRequest");
			SipServletResponse responseToOriginalRequest = originalRequest.createResponse(sipServletResponse.getStatus());
			if(logger.isInfoEnabled()) {
				logger.info("Functional addressing servlet -Sending on the first call leg " + responseToOriginalRequest.toString());
			}
			responseToOriginalRequest.send();		
		}
	}
	
	protected void doProvisionalResponse(SipServletResponse sipServletResponse)
			throws ServletException, IOException {
		if (logger.isInfoEnabled()){
			logger.info("Functional addressing servlet - Got provisional response: " + sipServletResponse.getStatus() + " " + sipServletResponse.getReasonPhrase() + "\n");
		}
		SipServletRequest originalRequest = (SipServletRequest) sipServletResponse.getSession().getAttribute("originalRequest");
		SipServletResponse responseToOriginalRequest = originalRequest.createResponse(sipServletResponse.getStatus());
		if(logger.isInfoEnabled()) {
			logger.info("Functional addressing servlet - Sending Provisional response on the first call leg " + responseToOriginalRequest.toString());
		}
		responseToOriginalRequest.send();
	}
	
	
	@Override
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
		if (logger.isInfoEnabled()){
			logger.info("Functional addressing servlet - Got BYE: " + request.getRequestURI().toString() + "\n");
		}
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
		SipSession session = request.getSession();
		B2buaHelper helper = request.getB2buaHelper();
		SipSession linkedSession = helper.getLinkedSession(session);		
		SipServletRequest forkedRequest = linkedSession.createRequest("BYE");
		if(logger.isInfoEnabled()) {
			logger.info("Functional addressing servlet - Sending BYE " + forkedRequest + "\n");
		}
		forkedRequest.send();	
		if(session != null && session.isValid()) {
			session.invalidate();
		}	

		return;
	}
	
	
	
}
