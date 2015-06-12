package uk.ac.cam.arb33.lectureserver;

import java.io.IOException;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static uk.ac.cam.arb33.lectureserver.ServletBase.EMAIL;

public class FilterAuthenticate implements Filter {
	
    static Log log = LogFactory.getLog(FilterAuthenticate.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // Only process http requests.
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            String msg = "Configuration Error.  This filter can only handle Http requests. The rest of the filter chain will NOT be processed.";
            log.error(msg);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
		
        Object email = req.getSession().getAttribute(EMAIL);
        if (email == null || !(email instanceof String)) {

        	//TODO(arb33): Need a method of handling DB failure gracefully
        	Database db;
			try {
				db = Database.getDatabase();
				if (db.getAuthType() != Database.AuthType.OPEN) {
	        		//user not authenticated redirect and abandon filter chain
	        		resp.sendRedirect(req.getContextPath() + "/login.html");
	        		return;					
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			} catch (NamingException e) {
				e.printStackTrace();
				return;
			}
        }
        
        chain.doFilter(request, response);		
	}

	@Override
	public void destroy() {
	}

}
