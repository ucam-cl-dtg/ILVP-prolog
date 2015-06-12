package uk.ac.cam.arb33.lectureserver;

import static uk.ac.cam.ucs.webauth.RavenFilter.ATTR_REMOTE_USER;

import java.io.IOException;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class RavenLogin
 */
public class RavenLogin extends ServletBase {
	private static final long serialVersionUID = 1L;
	protected Database database;
	ServletContext context;   
    
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		context = config.getServletContext();
		
		try {
			database = Database.getDatabase();
		} catch (NamingException e) {
			throw new ServletException(e);
		} catch (SQLException e) {
			throw new ServletException(e);
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String crsid = (String) request.getSession().getAttribute(ATTR_REMOTE_USER);
		String cam = "@cam.ac.uk";
		String email = crsid + cam;

		HttpSession session = request.getSession(true);
		session.setAttribute(EMAIL, email);
		session.setAttribute(FIRSTNAME, null);
		session.setAttribute(FAMILYNAME, null);
		
		//TODO(arb33): Long-term, we need to go back to where we came from (referer field)
		//For now, we'll go to a default page specfied in the db
		String defaultHomePage;
		try {
			defaultHomePage = database.getDefaultHomePage();
			response.sendRedirect(defaultHomePage);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}
	
}
