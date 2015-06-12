/*
 * Redirect all URL queries to the specified static path.
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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletVideoRedirect extends ServletBase {

	private static final long serialVersionUID = 1L;	
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		try {
			final Database.Person person = authenticateAndIssueErrorMessage(req, resp);
			if (person == null) {
				return; //error message already written by authentication method.
			}
			
			PageMetadata metadata = new PageMetadata(person,
					req.getRequestURI(), req.getContextPath(),req.getServletPath());
			database.putEvent(person.id, Database.EventType.VIDEO_LOAD.getId(), metadata.uriInsideServlet);
			String videoUrlBase = database.getVideoUrlBase();
			resp.sendRedirect(videoUrlBase + metadata.uriInsideServlet);
		} catch (Exception e) {
			//TODO(arb33): log these errors in db and issue redirect or something else sensible instead
			resp.setContentType("text/html");
			resp.setCharacterEncoding("utf-8");
			PrintWriter out = resp.getWriter();		
			out.write("" + e);
			for(StackTraceElement element: e.getStackTrace()) {
				out.write("<br/>" + element);
			}
		}
	}
}
