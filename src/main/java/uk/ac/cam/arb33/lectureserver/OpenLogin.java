package uk.ac.cam.arb33.lectureserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

public class OpenLogin extends ServletBase {

	private static final long serialVersionUID = 1L;
	
	final static String YAHOO_ENDPOINT = "https://me.yahoo.com";
	final static String GOOGLE_ENDPOINT = "https://www.google.com/accounts/o8/id";
	final static Map<String, String> endpoints = new HashMap<String, String>();
	
	ServletContext context;
	ConsumerManager manager;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		context = config.getServletContext();
		this.manager = new ConsumerManager(); //This line throws an exception
		endpoints.put("google", GOOGLE_ENDPOINT);
		endpoints.put("yahoo", YAHOO_ENDPOINT);
	}
		
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {	
		String identifier = endpoints.get(req.getParameter("identifier"));
		String returnToUrl = req.getRequestURL().toString();
		
		try {
			DiscoveryInformation discovered = manager.associate(manager.discover(identifier));
			req.getSession().setAttribute("openid", discovered);
			AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

			FetchRequest fetch = FetchRequest.createFetchRequest();
			if (identifier.startsWith(GOOGLE_ENDPOINT)) {
				fetch.addAttribute("email",
						"http://axschema.org/contact/email", true);
				fetch.addAttribute("firstName",
						"http://axschema.org/namePerson/first", true);
				fetch.addAttribute("lastName",
						"http://axschema.org/namePerson/last", true);
			} else if (identifier.startsWith(YAHOO_ENDPOINT)) {
				fetch.addAttribute("email",
						"http://axschema.org/contact/email", true);
				fetch.addAttribute("fullname",
						"http://axschema.org/namePerson", true);
			} else { // works for myOpenID
				fetch.addAttribute("fullname",
						"http://schema.openid.net/namePerson", true);
				fetch.addAttribute("email",
						"http://schema.openid.net/contact/email", true);
			}
			authReq.addExtension(fetch);
			resp.sendRedirect(authReq.getDestinationUrl(true));
		} catch(Exception e) {
			PrintWriter output = resp.getWriter();
			output.write("<html><body>");
			for(StackTraceElement trace: e.getStackTrace())
				output.write(trace + "<br />");
			output.write("</body></html>");
		}
		
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		try {
			ParameterList response = new ParameterList(req.getParameterMap());
			DiscoveryInformation discovered = (DiscoveryInformation) req.getSession().getAttribute("openid");
			StringBuffer receivingURL = req.getRequestURL();
			String queryString = req.getQueryString();
			
			if (queryString != null && queryString.length() > 0)
				receivingURL.append("?").append(req.getQueryString());
			VerificationResult verification = manager.verify(receivingURL.toString(), response, discovered);
			Identifier verified = verification.getVerifiedId();
			if (verified != null) {
				AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();
				if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
					FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);

					//TODO(arb33): Can email be null here?
					String email = (String) fetchResp.getAttributeValues("email").get(0);
					String firstname = (String) fetchResp.getAttributeValues("firstName").get(0);
					String familyname = (String) fetchResp.getAttributeValues("lastName").get(0);
					
					if (firstname == null && familyname == null) {
						String fullname = (String) fetchResp.getAttributeValues("fullname").get(0);
						String[] names = fullname.split("\\s+");
						//TODO(arb33): investigate how these names are created (two or more?); alternatively, can we query Yahoo differently for this data?
						firstname = names[0];
						familyname = names[names.length-1];
					}
					
					HttpSession session = req.getSession();					
					session.setAttribute(FIRSTNAME, firstname);
					session.setAttribute(FAMILYNAME, familyname);					
					session.setAttribute(EMAIL, email);
					
					
					//TODO(arb33): Long-term, we need to go back to where we came from (referer field)
					//For now, we'll go to a default page specfied in the db
					String defaultHomePage = database.getDefaultHomePage();
					resp.sendRedirect(defaultHomePage);
				} else {
					//Should I log this?
				}
			}
		} catch (MessageException e) {
			resp.sendRedirect(req.getContextPath() + "/");
		} catch (DiscoveryException e) {
			resp.sendRedirect(req.getContextPath() + "/");
		} catch (AssociationException e) {
			resp.sendRedirect(req.getContextPath() + "/");
		} catch (SQLException e) {
			resp.sendRedirect(req.getContextPath() + "/");
		}		
	}
}