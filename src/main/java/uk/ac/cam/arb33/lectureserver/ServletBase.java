/*
 * HttpServlet common to all servlets in this application; includes support for authentication.
 *
 * Copyright 2012 Alastair R. Beresford
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero Public License for more details.
 */
package uk.ac.cam.arb33.lectureserver;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletBase extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected Database database;
	protected static volatile HtmlRenderer renderer;

	public final static String EMAIL = "uk.ac.cam.arb33.lectureserver.RemoteUser";
	public final static String FIRSTNAME = "uk.ac.cam.arb33.lectureserver.Firstname";
	public final static String FAMILYNAME = "uk.ac.cam.arb33.lectureserver.Familyname";

	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config); //otherwise getServletContext() generates internal NullPointerException!

		try {
			database = Database.getDatabase();
			reloadSoyTemplateRenderer();
		} catch (NamingException e) {
			throw new ServletException(e);
		} catch (SQLException e) {
			throw new ServletException(e);
		}
	}

	protected String reloadSoyTemplateRenderer() throws SQLException {
		String resource = "/WEB-INF/classes/prolog.soy";
		String soyTemplate = database.getSoyTemplate();
		if (soyTemplate != null) {
			resource = soyTemplate;
		}
		final InputStream is = getServletContext().getResourceAsStream(resource);
		renderer = new HtmlRenderer(is);
		return resource;
	}
	
	private String getCookie(Cookie[] cookies, String name) {
		if (cookies == null)
			return null;
		for(Cookie c: cookies) {
			if (c.getName().equals(name)) {
				return c.getValue();
			}
		}
		return null;
	}

	private Cookie createCrsidCookie(String crsid, int maxAge) {
		Cookie c = new Cookie("crsid", crsid);
		c.setMaxAge(maxAge);
		c.setPath("/");
		return c;
	}

	protected Database.Person cookieImpersonate(HttpServletRequest req, 
			HttpServletResponse resp) throws SQLException {

		String crsid = req.getParameter("crsid");
		Cookie[] cookies = req.getCookies();
		if (crsid == null) {
			crsid = getCookie(cookies, "crsid");
		}
		Database.Person person;
		if (crsid == null || (person = database.getPerson(crsid)) == null) {
			resp.addCookie(createCrsidCookie("", 0)); //user not known, delete cookie
			return null;
		}
		resp.addCookie(createCrsidCookie(crsid, -1)); //impersonate user for length of browser session
		return person;
	}

	/**
	 * Extract CRSID and check person is in the Database, issuing error message if not.
	 *
	 * @param req
	 * @param resp
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	protected Database.Person authenticateAndIssueErrorMessage(HttpServletRequest req,
			HttpServletResponse resp) throws SQLException, IOException {
		
		Database.Person person;
		String email = (String) req.getSession().getAttribute(EMAIL);
		String firstname = (String) req.getSession().getAttribute(FIRSTNAME);
		String familyname = (String) req.getSession().getAttribute(FAMILYNAME);
		
		person = database.findOrAddPerson(firstname, familyname, email, req.getSession().getId());
				
		if (person == null) {
			String msg = "The email " + email + " is not authorized to access this page.";
			database.putEvent(Database.SYSTEM_ID, Database.EventType.SYSTEM.getId(), msg);
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg);
		} else {
			if (person.email != null && (person.email.equals("arb33@cam.ac.uk") || person.email.equals("acr31@cam.ac.uk"))) {
				//TODO(arb33): in the long term, store course administrators in the DB, not hard code as here
				Database.Person impersonate = cookieImpersonate(req, resp);
				if (impersonate != null) {
					person = impersonate;
				}
			}
		}

		return person;
	}
}
