/*
 * HttpServlet which maps the URL address space into DB queries and template rendering requests.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletContent extends ServletBase {

	private static final long serialVersionUID = 1L;

	/**
	 * Requests to this Servlet are assumed to be of the following form:
	 * "/" navigation page
	 * "/1213" list all the courses in a given year
	 * "/1213/course" all content for a specific course in a given year
	 * "/1213/course/video/zebra a specific activity (zebra) of a specific type (video)
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		PrintWriter out = resp.getWriter();		

		try {
			final Database.Person person = authenticateAndIssueErrorMessage(req, resp);
			if (person == null) {
				return; //error message already written by authentication method.
			}

			database.putEvent(person.id, Database.EventType.PAGE_SUCCESS.getId(), req.getRequestURI());
			
			PageMetadata metadata = new PageMetadata(person,
					req.getRequestURI(), req.getContextPath(),req.getServletPath());
			//A typical path is {"1213", "prolog", "video, "zebra"}
			String[] path = metadata.breadcrumb;
			switch(path.length) {
			case 0:
				renderer.homePage(metadata, out);
				break;
			case 1:
				List<Database.Course> courses = database.getCourses(path[0]);
				renderer.allCourseInYear(metadata, out, courses);
				break;
			case 2:
				List<Database.Activity> activities 
					= database.getCourseActivities(person.id, path[1], path[0]);
				String title = database.getCourseTitle(path[1], path[0]);
				renderer.course(metadata, out, activities, title);
				break;
			case 3:
				StringBuffer fullUrl = req.getRequestURL();
				resp.sendRedirect(fullUrl.substring(0, fullUrl.length() - path[2].length() - 1));
				break;
			case 4:
				Database.Activity a = database.getActivity(person.id, path[1], path[0], path[2], path[3]);
				renderer.courseActivity(metadata, out, a);
				break;
			default:
				throw new FileNotFoundException();
			}
		} catch (FileNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (Exception e) {
			//TODO(arb33): log these errors in db and issue redirect or something else sensible instead
			out.write("" + e);
			for(StackTraceElement element: e.getStackTrace()) {
				out.write("<br/>" + element);
			}
		}
	}
	
	/**
	 * Support storage of explicit user-supplied data and implicit event data.
	 * 
	 * No content is returned to caller. All submissions are made by JavaScript asynchronously.
	 * Users need to be authenticated. Post data has one of the following key-value formats:
	 *  {"type": "activity", "id": /id of activity/, "value": /true or false/}
	 *  {"type": "event", "id": /id of eventType/, "value": /event value/}
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		
		Database.Person person = null;
		String reqType = null;
		String reqId = null;
		String reqValue = null;
		try {
			person = authenticateAndIssueErrorMessage(req, resp);
			if (person == null) {
				return; //error message already written by authentication method.
			}
			
			reqType = req.getParameter("type");
			reqId = req.getParameter("id");
			reqValue = req.getParameter("value");
			
			if (reqType != null && reqType.equals("activity")) {
				final int activityId = Integer.parseInt(reqId);
				database.putActivityComplete(person.id, activityId, reqValue.equals("true"));
				database.putEvent(person.id, Database.EventType.ACTIVITY_COMPLETE.getId(), activityId + "," + reqValue);
			} else if (reqType != null && reqType.equals("event")) {
				//TODO(arb33): Prevent external insertion of certain events (e.g. SYSTEM).
				database.putEvent(person.id, Integer.parseInt(reqId), reqValue);
			} else {
				throw new Exception("Error handling POST request for: " + reqType + ", " + reqId + ", " + reqValue);
			}
		} catch (Exception e) {
			try {
				database.putEvent(Database.SYSTEM_ID, Database.EventType.UNDEFINED_EVENT.getId(), 
						"(" + person.email + ", " + reqType + ", " + reqId + ", " + reqValue + ")");
			} catch (SQLException sqlException) {
				sqlException.printStackTrace();
			}
		}
	}
}